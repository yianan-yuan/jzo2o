package com.jzo2o.aigc.service;

import com.jzo2o.aigc.controller.consumer.dto.RecommendationCardDTO;
import com.jzo2o.aigc.domain.AigcSession;
import com.jzo2o.aigc.domain.ChatTurn;
import com.jzo2o.aigc.domain.ConversationStage;
import com.jzo2o.aigc.domain.DemandDecision;
import com.jzo2o.aigc.domain.SelectedService;
import com.jzo2o.aigc.domain.ServeUnitLabels;
import com.jzo2o.aigc.exception.AigcErrorCode;
import com.jzo2o.aigc.exception.AigcException;
import com.jzo2o.aigc.model.CancellationToken;
import com.jzo2o.aigc.observability.AigcObservation;
import com.jzo2o.aigc.observability.AigcObservationLogger;
import com.jzo2o.aigc.properties.AigcProperties;
import com.jzo2o.aigc.stream.SseEventSink;
import com.jzo2o.api.foundations.dto.response.ServeAggregationResDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssistantOrchestrator {

    private static final int HARD_RECOMMENDATION_LIMIT = 3;
    private static final Pattern SENSITIVE_DIGITS = Pattern.compile("(?<!\\d)\\d{6,}(?!\\d)");
    private static final String NO_MATCH_TEXT = "暂时没有找到符合条件的服务，请换个需求描述试试。";
    private static final String STALE_REFERENCE_TEXT = "你刚才提到的服务目前不可用，请换一个需求描述试试。";
    private static final String FALLBACK_TEXT = "智能服务暂时不可用，已为你匹配以下真实服务。";
    private static final String FALLBACK_REASON = "根据你的需求匹配到该服务";

    private final AigcSessionService sessionService;
    private final DemandUnderstandingService understanding;
    private final ServiceCatalogService catalog;
    private final CandidateSelectionService selection;
    private final ReplyGenerationService replyGenerator;
    private final AigcProperties properties;
    private final AigcObservationLogger observationLogger;

    public void run(Long userId,
                    String sessionId,
                    String cityCode,
                    String message,
                    SseEventSink sink,
                    CancellationToken cancellationToken) {
        if (cancelled(cancellationToken)) {
            return;
        }
        ObservationContext observation = startObservation(userId);
        long loadStarted = System.nanoTime();
        AigcSession session = sessionService.loadOwned(userId, sessionId);
        observation.observation.recordSessionLoadMillis(elapsedMillis(loadStarted));
        runControlled(session, cityCode, message, sink, cancellationToken, observation);
    }

    public void run(AigcSession session,
                    String cityCode,
                    String message,
                    SseEventSink sink,
                    CancellationToken cancellationToken) {
        if (cancelled(cancellationToken)) {
            return;
        }
        runControlled(session, cityCode, message, sink, cancellationToken,
                startObservation(Objects.requireNonNull(session, "session").getUserId()));
    }

    private void runControlled(AigcSession session,
                               String cityCode,
                               String message,
                               SseEventSink sink,
                               CancellationToken cancellationToken,
                               ObservationContext observation) {
        if (cancelled(cancellationToken)) {
            return;
        }
        session.updateCity(cityCode);
        sink.status("UNDERSTANDING");
        if (cancelled(cancellationToken)) {
            return;
        }
        try {
            executeControlled(session, cityCode, message, sink, cancellationToken, observation);
        } catch (AigcException error) {
            if (cancelled(cancellationToken)) {
                return;
            }
            if (error.getErrorCode() == AigcErrorCode.MODEL_UNAVAILABLE) {
                fallback(session, cityCode, message, sink, cancellationToken, observation);
                return;
            }
            emitError(sink, error.getErrorCode(), cancellationToken, observation);
        } catch (RuntimeException error) {
            if (cancelled(cancellationToken)) {
                return;
            }
            log.error("Unexpected assistant orchestration failure", error);
            emitError(sink, AigcErrorCode.MODEL_UNAVAILABLE, cancellationToken, observation);
        }
    }

    private void executeControlled(AigcSession session,
                                   String cityCode,
                                   String message,
                                   SseEventSink sink,
                                   CancellationToken cancellationToken,
                                   ObservationContext observation) {
        if (cancelled(cancellationToken)) {
            return;
        }
        observation.modelCalls++;
        long demandStarted = System.nanoTime();
        DemandDecision decision;
        try {
            decision = understanding.understand(session, message, cancellationToken);
        } finally {
            observation.observation.recordDemandExtractionMillis(elapsedMillis(demandStarted));
        }
        if (cancelled(cancellationToken)) {
            return;
        }
        session.setDemandProfile(decision.getProfile());
        if (decision.isNeedsClarification()) {
            finishClarification(session, message, decision.getClarifyingQuestion(), sink, cancellationToken,
                    observation);
            return;
        }
        if (decision.getReferencedRecommendationIndex() != null) {
            explainReferenced(session, cityCode, message, decision.getReferencedRecommendationIndex(),
                    sink, cancellationToken, observation);
            return;
        }
        recommendByKeyword(session, cityCode, message, decision, sink, cancellationToken, observation);
    }

    private void recommendByKeyword(AigcSession session,
                                    String cityCode,
                                    String message,
                                    DemandDecision decision,
                                    SseEventSink sink,
                                    CancellationToken cancellationToken,
                                    ObservationContext observation) {
        if (cancelled(cancellationToken)) {
            return;
        }
        sink.status("SEARCHING_SERVICES");
        if (cancelled(cancellationToken)) {
            return;
        }
        List<ServeAggregationResDTO> candidates = activeCityCandidates(
                searchCatalog(cityCode, decision.getProfile().getSearchKeyword(), 20, observation), cityCode);
        observation.candidateCount = candidates.size();
        if (cancelled(cancellationToken)) {
            return;
        }
        if (candidates.isEmpty()) {
            finishNoMatch(session, message, NO_MATCH_TEXT, sink, cancellationToken, observation);
            return;
        }
        observation.modelCalls++;
        List<SelectedService> selected = selection.select(decision.getProfile(), candidates, cancellationToken);
        if (cancelled(cancellationToken)) {
            return;
        }
        List<RecommendationCardDTO> cards = cards(selected, candidates);
        if (cards.isEmpty()) {
            finishNoMatch(session, message, NO_MATCH_TEXT, sink, cancellationToken, observation);
            return;
        }
        streamRecommendation(session, message, cards, sink, cancellationToken, observation, false);
    }

    private void explainReferenced(AigcSession session,
                                   String cityCode,
                                   String message,
                                   int oneBasedIndex,
                                   SseEventSink sink,
                                   CancellationToken cancellationToken,
                                   ObservationContext observation) {
        if (cancelled(cancellationToken)) {
            return;
        }
        List<Long> previousIds = session.getLastRecommendedServeIds();
        if (oneBasedIndex < 1 || oneBasedIndex > previousIds.size()) {
            finishNoMatch(session, message, STALE_REFERENCE_TEXT, sink, cancellationToken, observation);
            return;
        }
        Long serveId = previousIds.get(oneBasedIndex - 1);
        ServeAggregationResDTO candidate = serveId == null ? null : findCatalog(serveId, observation);
        observation.candidateCount = candidate == null ? 0 : 1;
        if (cancelled(cancellationToken)) {
            return;
        }
        if (!isActiveInCity(candidate, cityCode) || ServeUnitLabels.labelOf(candidate.getUnit()) == null) {
            if (candidate != null && ServeUnitLabels.labelOf(candidate.getUnit()) == null) {
                logInvalidUnit(candidate);
            }
            finishNoMatch(session, message, STALE_REFERENCE_TEXT, sink, cancellationToken, observation);
            return;
        }
        List<RecommendationCardDTO> cards = cards(
                Collections.singletonList(new SelectedService(serveId, "根据你刚才关注的服务继续说明")),
                Collections.singletonList(candidate));
        if (cards.isEmpty()) {
            finishNoMatch(session, message, STALE_REFERENCE_TEXT, sink, cancellationToken, observation);
            return;
        }
        streamRecommendation(session, message, cards, sink, cancellationToken, observation, false);
    }

    private void streamRecommendation(AigcSession session,
                                      String message,
                                      List<RecommendationCardDTO> cards,
                                      SseEventSink sink,
                                      CancellationToken cancellationToken,
                                      ObservationContext observation,
                                      boolean degraded) {
        if (cancelled(cancellationToken)) {
            return;
        }
        sink.status("GENERATING");
        if (cancelled(cancellationToken)) {
            return;
        }
        StringBuilder reply = new StringBuilder();
        Consumer<String> delta = text -> {
            if (!cancelled(cancellationToken) && text != null && !text.trim().isEmpty()) {
                emitDelta(sink, text, observation);
                if (!cancelled(cancellationToken)) {
                    reply.append(text);
                }
            }
        };
        observation.modelCalls++;
        replyGenerator.streamReply(session, cards, cancellationToken, delta);
        if (cancelled(cancellationToken)) {
            return;
        }
        sink.recommendations(cards);
        if (cancelled(cancellationToken)
                || !finishRecommendationState(session, message, reply.toString(), cards, cancellationToken)) {
            return;
        }
        if (cancelled(cancellationToken)) {
            return;
        }
        sink.done(ConversationStage.RECOMMENDING, suggestedQuestions(cards.size()));
        observeTerminal(observation, ConversationStage.RECOMMENDING.name(), null, degraded,
                cards.size());
    }

    private void fallback(AigcSession session,
                          String cityCode,
                          String message,
                          SseEventSink sink,
                          CancellationToken cancellationToken,
                          ObservationContext observation) {
        if (cancelled(cancellationToken)) {
            return;
        }
        try {
            sink.status("SEARCHING_SERVICES");
            if (cancelled(cancellationToken)) {
                return;
            }
            List<ServeAggregationResDTO> candidates = activeCityCandidates(
                    searchCatalog(cityCode, normalizeForFallback(message), HARD_RECOMMENDATION_LIMIT, observation),
                    cityCode);
            observation.candidateCount = candidates.size();
            if (cancelled(cancellationToken)) {
                return;
            }
            List<RecommendationCardDTO> cards = new ArrayList<>();
            for (ServeAggregationResDTO candidate : candidates) {
                if (cancelled(cancellationToken)) {
                    return;
                }
                RecommendationCardDTO card = card(new SelectedService(candidate.getId(), FALLBACK_REASON), candidate);
                if (card != null) {
                    cards.add(card);
                    if (cards.size() == HARD_RECOMMENDATION_LIMIT) {
                        break;
                    }
                }
            }
            if (cards.isEmpty()) {
                emitError(sink, AigcErrorCode.MODEL_UNAVAILABLE, cancellationToken, observation);
                return;
            }
            if (cancelled(cancellationToken)) {
                return;
            }
            sink.status("GENERATING");
            if (cancelled(cancellationToken)) {
                return;
            }
            emitDelta(sink, FALLBACK_TEXT, observation);
            if (cancelled(cancellationToken)) {
                return;
            }
            sink.recommendations(cards);
            if (cancelled(cancellationToken)
                    || !finishRecommendationState(session, message, FALLBACK_TEXT, cards, cancellationToken)) {
                return;
            }
            if (cancelled(cancellationToken)) {
                return;
            }
            sink.done(ConversationStage.RECOMMENDING, suggestedQuestions(cards.size()));
            observeTerminal(observation, ConversationStage.RECOMMENDING.name(), null, true, cards.size());
        } catch (AigcException error) {
            emitError(sink, error.getErrorCode(), cancellationToken, observation);
        } catch (RuntimeException error) {
            if (cancelled(cancellationToken)) {
                return;
            }
            log.warn("Fallback service catalog failed", error);
            emitError(sink, AigcErrorCode.SERVICE_CATALOG_UNAVAILABLE, cancellationToken, observation);
        }
    }

    private void finishClarification(AigcSession session,
                                     String message,
                                     String question,
                                     SseEventSink sink,
                                     CancellationToken cancellationToken,
                                     ObservationContext observation) {
        if (cancelled(cancellationToken)) {
            return;
        }
        sink.status("GENERATING");
        if (cancelled(cancellationToken)) {
            return;
        }
        emitDelta(sink, question, observation);
        if (cancelled(cancellationToken)) {
            return;
        }
        session.setLastRecommendedServeIds(Collections.emptyList());
        session.setStage(ConversationStage.CLARIFYING);
        appendTurns(session, message, question);
        if (cancelled(cancellationToken)) {
            return;
        }
        sessionService.save(session);
        if (cancelled(cancellationToken)) {
            return;
        }
        sink.done(ConversationStage.CLARIFYING, Collections.emptyList());
        observeTerminal(observation, ConversationStage.CLARIFYING.name(), null, false, 0);
    }

    private void finishNoMatch(AigcSession session,
                               String message,
                               String text,
                               SseEventSink sink,
                               CancellationToken cancellationToken,
                               ObservationContext observation) {
        if (cancelled(cancellationToken)) {
            return;
        }
        sink.status("GENERATING");
        if (cancelled(cancellationToken)) {
            return;
        }
        emitDelta(sink, text, observation);
        if (cancelled(cancellationToken)) {
            return;
        }
        session.setLastRecommendedServeIds(Collections.emptyList());
        session.setStage(ConversationStage.NO_MATCH);
        appendTurns(session, message, text);
        if (cancelled(cancellationToken)) {
            return;
        }
        sessionService.save(session);
        if (cancelled(cancellationToken)) {
            return;
        }
        sink.done(ConversationStage.NO_MATCH, Collections.singletonList("可以换个需求描述吗？"));
        observeTerminal(observation, ConversationStage.NO_MATCH.name(), null, false, 0);
    }

    private boolean finishRecommendationState(AigcSession session,
                                              String message,
                                              String assistantText,
                                              List<RecommendationCardDTO> cards,
                                              CancellationToken cancellationToken) {
        if (cancelled(cancellationToken)) {
            return false;
        }
        session.setLastRecommendedServeIds(cardIds(cards));
        session.setStage(ConversationStage.RECOMMENDING);
        appendTurns(session, message, assistantText);
        if (cancelled(cancellationToken)) {
            return false;
        }
        sessionService.save(session);
        return !cancelled(cancellationToken);
    }

    private List<ServeAggregationResDTO> searchCatalog(String cityCode,
                                                        String keyword,
                                                        int limit,
                                                        ObservationContext observation) {
        long queryStarted = System.nanoTime();
        try {
            return catalog.search(cityCode, keyword, limit);
        } catch (RuntimeException error) {
            throw new AigcException(AigcErrorCode.SERVICE_CATALOG_UNAVAILABLE);
        } finally {
            observation.observation.addServiceQueryMillis(elapsedMillis(queryStarted));
        }
    }

    private ServeAggregationResDTO findCatalog(Long serveId, ObservationContext observation) {
        long queryStarted = System.nanoTime();
        try {
            return catalog.findById(serveId);
        } catch (RuntimeException error) {
            throw new AigcException(AigcErrorCode.SERVICE_CATALOG_UNAVAILABLE);
        } finally {
            observation.observation.addServiceQueryMillis(elapsedMillis(queryStarted));
        }
    }

    private List<ServeAggregationResDTO> activeCityCandidates(List<ServeAggregationResDTO> candidates,
                                                               String cityCode) {
        List<ServeAggregationResDTO> result = new ArrayList<>();
        if (candidates == null) {
            return result;
        }
        for (ServeAggregationResDTO candidate : candidates) {
            if (isActiveInCity(candidate, cityCode)) {
                result.add(candidate);
            }
        }
        return result;
    }

    private boolean isActiveInCity(ServeAggregationResDTO candidate, String cityCode) {
        return candidate != null
                && Integer.valueOf(2).equals(candidate.getSaleStatus())
                && Objects.equals(cityCode, candidate.getCityCode());
    }

    private List<RecommendationCardDTO> cards(List<SelectedService> selected,
                                               List<ServeAggregationResDTO> candidates) {
        Map<Long, ServeAggregationResDTO> byId = new LinkedHashMap<>();
        for (ServeAggregationResDTO candidate : candidates) {
            if (candidate != null && candidate.getId() != null) {
                byId.put(candidate.getId(), candidate);
            }
        }
        List<RecommendationCardDTO> cards = new ArrayList<>();
        if (selected == null) {
            return cards;
        }
        int configuredLimit = Math.max(1, properties.getMaxRecommendations());
        int resultLimit = Math.min(HARD_RECOMMENDATION_LIMIT, configuredLimit);
        for (SelectedService chosen : selected) {
            if (chosen == null) {
                continue;
            }
            RecommendationCardDTO card = card(chosen, byId.get(chosen.getServeId()));
            if (card != null) {
                cards.add(card);
                if (cards.size() == resultLimit) {
                    break;
                }
            }
        }
        return cards;
    }

    private RecommendationCardDTO card(SelectedService selected, ServeAggregationResDTO candidate) {
        if (candidate == null) {
            return null;
        }
        String priceUnit = ServeUnitLabels.labelOf(candidate.getUnit());
        if (priceUnit == null) {
            logInvalidUnit(candidate);
            return null;
        }
        return RecommendationCardDTO.builder()
                .serveId(candidate.getId())
                .serveItemName(candidate.getServeItemName())
                .serveItemImg(candidate.getServeItemImg())
                .price(candidate.getPrice())
                .priceUnit(priceUnit)
                .recommendationReason(selected.getReason())
                .actionType("SERVICE_DETAIL")
                .build();
    }

    private void logInvalidUnit(ServeAggregationResDTO candidate) {
        log.error("Invalid service catalog unit: serveId={}, unit={}", candidate.getId(), candidate.getUnit());
    }

    private String normalizeForFallback(String message) {
        String normalized = message == null ? "" : message.trim().replaceAll("\\s+", " ");
        return SENSITIVE_DIGITS.matcher(normalized).replaceAll("***");
    }

    private List<Long> cardIds(List<RecommendationCardDTO> cards) {
        List<Long> ids = new ArrayList<>();
        for (RecommendationCardDTO card : cards) {
            ids.add(card.getServeId());
        }
        return ids;
    }

    private List<String> suggestedQuestions(int recommendationCount) {
        List<String> questions = new ArrayList<>();
        questions.add("还有其他选择吗？");
        questions.add("这个服务怎么预约？");
        if (recommendationCount >= 2) {
            questions.add("第二个服务怎么样？");
        }
        return questions;
    }

    private void appendTurns(AigcSession session, String userText, String assistantText) {
        session.addChatTurn(new ChatTurn("user", userText), properties.getMaxRounds());
        session.addChatTurn(new ChatTurn("assistant", assistantText), properties.getMaxRounds());
    }

    private boolean cancelled(CancellationToken cancellationToken) {
        return cancellationToken != null && cancellationToken.isCancelled();
    }

    private ObservationContext startObservation(Long userId) {
        return new ObservationContext(AigcObservation.start(
                UUID.randomUUID().toString(),
                userId,
                properties.getModel().getProvider(),
                properties.getModel().getModel()));
    }

    private void emitDelta(SseEventSink sink, String text, ObservationContext observation) {
        sink.delta(text);
        observation.observation.markFirstDelta();
    }

    private void observeTerminal(ObservationContext observation,
                                 String terminalStage,
                                 String errorCode,
                                 boolean degraded,
                                 int recommendationCount) {
        boolean firstTerminal = observation.observation.finish(
                terminalStage,
                errorCode,
                degraded,
                observation.candidateCount,
                recommendationCount,
                observation.modelCalls,
                observation.retries,
                0L);
        if (!firstTerminal) {
            return;
        }
        try {
            observationLogger.log(observation.observation);
        } catch (RuntimeException ignored) {
            log.warn("AIGC observation logger failed");
        }
    }

    private void emitError(SseEventSink sink,
                           AigcErrorCode errorCode,
                           CancellationToken cancellationToken,
                           ObservationContext observation) {
        if (!cancelled(cancellationToken)) {
            sink.error(errorCode, errorCode.getCode());
            observeTerminal(observation, "ERROR", errorCode.getCode(), false, 0);
        }
    }

    private long elapsedMillis(long startedNanos) {
        return Math.max(0L, (System.nanoTime() - startedNanos) / 1_000_000L);
    }

    private static final class ObservationContext {
        private final AigcObservation observation;
        private int modelCalls;
        private int retries;
        private int candidateCount;

        private ObservationContext(AigcObservation observation) {
            this.observation = observation;
        }
    }
}
