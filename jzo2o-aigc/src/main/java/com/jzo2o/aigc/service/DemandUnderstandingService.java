package com.jzo2o.aigc.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jzo2o.aigc.domain.AigcSession;
import com.jzo2o.aigc.domain.DemandDecision;
import com.jzo2o.aigc.domain.DemandProfile;
import com.jzo2o.aigc.exception.AigcErrorCode;
import com.jzo2o.aigc.exception.AigcException;
import com.jzo2o.aigc.model.CancellationToken;
import com.jzo2o.aigc.model.ModelMessage;
import com.jzo2o.aigc.model.ModelProvider;
import com.jzo2o.aigc.properties.AigcProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DemandUnderstandingService {

    private static final String FALLBACK_CLARIFYING_QUESTION =
            "您好！请告诉我需要哪类家政服务，例如日常保洁、空调维修或厨卫维修。";

    private static final Set<String> REQUIRED_FIELDS = new LinkedHashSet<>(Arrays.asList(
            "summary", "searchKeyword", "constraints", "needsClarification",
            "clarifyingQuestion"));
    private static final Set<String> ALLOWED_FIELDS = new LinkedHashSet<>(REQUIRED_FIELDS);

    static {
        ALLOWED_FIELDS.add("referencedRecommendationIndex");
    }

    private final ModelProvider provider;
    private final ObjectMapper objectMapper;
    private final AigcProperties properties;
    private final PromptFactory promptFactory;

    public DemandDecision understand(AigcSession session,
                                     String userText,
                                     CancellationToken cancellationToken) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(userText, "userText");
        Objects.requireNonNull(cancellationToken, "cancellationToken");
        ensureActive(cancellationToken);

        if (isGreeting(userText.trim()) || isGenericServiceQuery(userText.trim())) {
            return fallbackDecision(session, userText);
        }

        List<ModelMessage> messages = promptFactory.demandMessages(userText);
        String firstOutput = provider.completeJson(
                messages, properties.getModel().getTemperature(), cancellationToken,
                promptFactory.demandResponseSchema());
        try {
            return parse(firstOutput, session);
        } catch (InvalidModelOutput ignored) {
            ensureActive(cancellationToken);
            String retryOutput = provider.completeJson(
                    messages, 0D, cancellationToken, promptFactory.demandResponseSchema());
            try {
                return parse(retryOutput, session);
            } catch (InvalidModelOutput invalidAgain) {
                if (invalidAgain.isFallbackEligible()) {
                    return fallbackDecision(session, userText);
                }
                throw new AigcException(AigcErrorCode.MODEL_OUTPUT_INVALID);
            }
        }
    }

    private DemandDecision fallbackDecision(AigcSession session, String userText) {
        String normalizedText = userText.trim();
        DemandProfile previous = session.getDemandProfile();
        DemandProfile profile = new DemandProfile();
        if (previous != null) {
            profile.setServiceTypeHint(previous.getServiceTypeHint());
            profile.setConfirmedConstraints(previous.getConfirmedConstraints() == null
                    ? new ArrayList<>()
                    : new ArrayList<>(previous.getConfirmedConstraints()));
            profile.setClarifiedFacts(previous.getClarifiedFacts() == null
                    ? new LinkedHashMap<>()
                    : new LinkedHashMap<>(previous.getClarifiedFacts()));
        } else {
            profile.setClarifiedFacts(new LinkedHashMap<>());
        }
        if (isGreeting(normalizedText)) {
            profile.setSummary("用户尚未说明具体家政需求");
            profile.setSearchKeyword("");
            return new DemandDecision(profile, true, FALLBACK_CLARIFYING_QUESTION, null, null);
        }
        if (isGenericServiceQuery(normalizedText)) {
            profile.setSummary("用户希望浏览可预约的家政服务");
            profile.setSearchKeyword("");
            return new DemandDecision(profile, false, null, null, null);
        }
        profile.setSummary(normalizedText);
        profile.setSearchKeyword(inferSearchKeyword(normalizedText));
        return new DemandDecision(profile, false, null, null, null);
    }

    private boolean isGreeting(String text) {
        String compact = text.replaceAll("[\\s，。！？!?、,.]", "").toLowerCase(Locale.ROOT);
        return "你好".equals(compact) || "您好".equals(compact) || "嗨".equals(compact)
                || "在吗".equals(compact) || "hi".equals(compact) || "hello".equals(compact);
    }

    private boolean isGenericServiceQuery(String text) {
        String compact = text.replaceAll("[\\s，。！？!?、,.]", "");
        return "有什么服务".equals(compact) || "有哪些服务".equals(compact)
                || "服务推荐".equals(compact) || "推荐服务".equals(compact)
                || "服务列表".equals(compact);
    }

    private String inferSearchKeyword(String text) {
        String[] keywords = {"保洁", "清洗", "空调", "厨卫", "维修", "月嫂", "保姆", "家电", "家政"};
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return keyword;
            }
        }
        return text;
    }

    private DemandDecision parse(String output, AigcSession session) {
        JsonNode root;
        try {
            root = objectMapper.readTree(output);
        } catch (JsonProcessingException | RuntimeException error) {
            throw new InvalidModelOutput(true);
        }
        if (root == null || !root.isObject()) {
            throw new InvalidModelOutput(true);
        }
        requireObjectWithExactFields(root);
        JsonNode summary = root.get("summary");
        JsonNode searchKeyword = root.get("searchKeyword");
        JsonNode constraints = root.get("constraints");
        JsonNode needsClarification = root.get("needsClarification");
        JsonNode clarifyingQuestion = root.get("clarifyingQuestion");
        JsonNode recommendationIndex = root.get("referencedRecommendationIndex");

        require(summary.isTextual());
        require(searchKeyword.isTextual());
        require(constraints.isObject());
        require(needsClarification.isBoolean());
        require(clarifyingQuestion.isNull() || clarifyingQuestion.isTextual());
        require(recommendationIndex == null || recommendationIndex.isNull()
                || recommendationIndex.isIntegralNumber() && recommendationIndex.canConvertToInt());

        DemandProfile previous = session.getDemandProfile();
        Map<String, String> facts = previous == null || previous.getClarifiedFacts() == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(previous.getClarifiedFacts());
        Iterator<Map.Entry<String, JsonNode>> constraintFields = constraints.fields();
        while (constraintFields.hasNext()) {
            Map.Entry<String, JsonNode> constraint = constraintFields.next();
            require(constraint.getValue().isTextual());
            facts.put(constraint.getKey(), constraint.getValue().textValue());
        }

        boolean clarification = needsClarification.booleanValue();
        String question = clarifyingQuestion.isNull() ? null : clarifyingQuestion.textValue();
        if (clarification) {
            require(hasText(question));
        }

        Integer index = recommendationIndex == null || recommendationIndex.isNull()
                ? null
                : recommendationIndex.intValue();
        Long serveId = null;
        if (index != null) {
            List<Long> recommendations = session.getLastRecommendedServeIds();
            if (recommendations == null || recommendations.isEmpty()) {
                // 小模型在首次问候时偶尔会凭空返回推荐序号；没有历史推荐时该字段没有语义。
                index = null;
            } else {
                require(index >= 1 && index <= recommendations.size());
                serveId = recommendations.get(index - 1);
                require(serveId != null);
            }
        }
        if (!clarification) {
            require(hasText(searchKeyword.textValue()) || index != null);
        }

        DemandProfile profile = new DemandProfile();
        profile.setSummary(summary.textValue());
        profile.setSearchKeyword(searchKeyword.textValue());
        if (previous != null) {
            profile.setServiceTypeHint(previous.getServiceTypeHint());
            profile.setConfirmedConstraints(previous.getConfirmedConstraints() == null
                    ? new ArrayList<>()
                    : new ArrayList<>(previous.getConfirmedConstraints()));
        }
        profile.setClarifiedFacts(new LinkedHashMap<>(facts));
        return new DemandDecision(profile, clarification, question, index, serveId);
    }

    private void requireObjectWithExactFields(JsonNode root) {
        require(root != null && root.isObject());
        Set<String> actualFields = new LinkedHashSet<>();
        root.fieldNames().forEachRemaining(actualFields::add);
        require(actualFields.containsAll(REQUIRED_FIELDS) && ALLOWED_FIELDS.containsAll(actualFields));
    }

    private void ensureActive(CancellationToken cancellationToken) {
        if (cancellationToken.isCancelled()) {
            throw new AigcException(AigcErrorCode.REQUEST_TIMEOUT);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private void require(boolean valid) {
        if (!valid) {
            throw new InvalidModelOutput();
        }
    }

    private static final class InvalidModelOutput extends RuntimeException {
        private static final long serialVersionUID = 1L;

        private final boolean fallbackEligible;

        private InvalidModelOutput() {
            this(false);
        }

        private InvalidModelOutput(boolean fallbackEligible) {
            this.fallbackEligible = fallbackEligible;
        }

        private boolean isFallbackEligible() {
            return fallbackEligible;
        }
    }
}
