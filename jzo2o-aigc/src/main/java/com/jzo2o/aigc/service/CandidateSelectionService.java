package com.jzo2o.aigc.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jzo2o.aigc.domain.DemandProfile;
import com.jzo2o.aigc.domain.SelectedService;
import com.jzo2o.aigc.exception.AigcErrorCode;
import com.jzo2o.aigc.exception.AigcException;
import com.jzo2o.aigc.model.CancellationToken;
import com.jzo2o.aigc.model.ModelMessage;
import com.jzo2o.aigc.model.ModelProvider;
import com.jzo2o.aigc.properties.AigcProperties;
import com.jzo2o.api.foundations.dto.response.ServeAggregationResDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CandidateSelectionService {

    private static final int HARD_CANDIDATE_LIMIT = 20;
    private static final int HARD_SELECTION_LIMIT = 3;
    private static final String FALLBACK_REASON = "根据你的需求匹配到该服务";

    private final ModelProvider provider;
    private final ObjectMapper objectMapper;
    private final AigcProperties properties;
    private final PromptFactory promptFactory;

    public List<SelectedService> select(DemandProfile profile,
                                        List<ServeAggregationResDTO> candidates,
                                        CancellationToken cancellationToken) {
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(candidates, "candidates");
        Objects.requireNonNull(cancellationToken, "cancellationToken");
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }
        ensureActive(cancellationToken);

        int configuredLimit = Math.max(1, properties.getMaxCandidates());
        int candidateCount = Math.min(candidates.size(), Math.min(HARD_CANDIDATE_LIMIT, configuredLimit));
        List<ServeAggregationResDTO> promptCandidates = new ArrayList<>(candidates.subList(0, candidateCount));
        Set<Long> allowedIds = new LinkedHashSet<>();
        for (ServeAggregationResDTO candidate : promptCandidates) {
            if (candidate != null && candidate.getId() != null) {
                allowedIds.add(candidate.getId());
            }
        }
        List<ModelMessage> messages = promptFactory.selectionMessages(profile, promptCandidates);
        String firstOutput = provider.completeJson(
                messages, properties.getModel().getTemperature(), cancellationToken,
                promptFactory.selectionResponseSchema());
        try {
            return withCandidateFallback(parse(firstOutput, allowedIds), promptCandidates);
        } catch (InvalidModelOutput ignored) {
            ensureActive(cancellationToken);
            String retryOutput = provider.completeJson(
                    messages, 0D, cancellationToken, promptFactory.selectionResponseSchema());
            try {
                return withCandidateFallback(parse(retryOutput, allowedIds), promptCandidates);
            } catch (InvalidModelOutput invalidAgain) {
                throw new AigcException(AigcErrorCode.MODEL_OUTPUT_INVALID);
            }
        }
    }

    private List<SelectedService> withCandidateFallback(List<SelectedService> selected,
                                                        List<ServeAggregationResDTO> candidates) {
        if (!selected.isEmpty()) {
            return selected;
        }
        int configuredLimit = Math.max(1, properties.getMaxRecommendations());
        int resultLimit = Math.min(HARD_SELECTION_LIMIT, configuredLimit);
        Set<Long> seen = new LinkedHashSet<>();
        List<SelectedService> result = new ArrayList<>();
        for (ServeAggregationResDTO candidate : candidates) {
            if (candidate == null || candidate.getId() == null || !seen.add(candidate.getId())) {
                continue;
            }
            result.add(new SelectedService(candidate.getId(), FALLBACK_REASON));
            if (result.size() == resultLimit) {
                break;
            }
        }
        return result;
    }

    private List<SelectedService> parse(String output, Set<Long> allowedIds) {
        JsonNode root;
        try {
            root = objectMapper.readTree(output);
        } catch (JsonProcessingException | RuntimeException error) {
            throw new InvalidModelOutput();
        }
        require(root != null && root.isObject() && root.size() == 1 && root.has("selected"));
        JsonNode selected = root.get("selected");
        require(selected.isArray());

        int configuredLimit = Math.max(1, properties.getMaxRecommendations());
        int resultLimit = Math.min(HARD_SELECTION_LIMIT, configuredLimit);
        Set<Long> seen = new LinkedHashSet<>();
        List<SelectedService> result = new ArrayList<>();
        for (JsonNode item : selected) {
            require(item != null && item.isObject());
            JsonNode serveId = item.get("serveId");
            JsonNode reason = item.get("reason");
            require(serveId != null && serveId.isIntegralNumber() && serveId.canConvertToLong());
            require(reason != null && reason.isTextual() && hasText(reason.textValue()));
            long id = serveId.longValue();
            if (allowedIds.contains(id) && seen.add(id)) {
                result.add(new SelectedService(id, reason.textValue()));
                if (result.size() == resultLimit) {
                    break;
                }
            }
        }
        return result;
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
    }
}
