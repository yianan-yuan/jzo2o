package com.jzo2o.aigc.observability;

import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AigcObservationTest {

    @Test
    void shouldExposeOnlyAllowListedStructuredFieldsWithAnonymousUser() {
        AigcObservation observation = AigcObservation.start("req-1", 7L, "ollama", "qwen3:0.6b");
        observation.recordSessionLoadMillis(4);
        observation.recordDemandExtractionMillis(12);
        observation.recordServiceQueryMillis(8);
        observation.recordFirstDeltaMillis(20);

        assertThat(observation.finish("RECOMMENDING", null, false, 5, 3, 2, 0, 0)).isTrue();

        Map<String, Object> fields = observation.toLogFields();
        assertThat(fields).containsOnlyKeys(
                "requestId", "anonymousUser", "provider", "model",
                "sessionLoadMillis", "demandExtractionMillis", "serviceQueryMillis",
                "firstDeltaMillis", "totalMillis", "modelCalls", "retries", "tokenUsage",
                "candidateCount", "recommendationCount", "terminalStage", "errorCode", "degraded");
        assertThat(fields)
                .containsEntry("requestId", "req-1")
                .containsEntry("anonymousUser", "53ed65896279")
                .containsEntry("provider", "ollama")
                .containsEntry("model", "qwen3:0.6b")
                .containsEntry("terminalStage", "RECOMMENDING")
                .containsEntry("errorCode", null)
                .containsEntry("degraded", false);
        assertThat(fields).doesNotContainKeys("userId", "message", "prompt", "messages", "candidates");
        assertThat(fields.values()).noneMatch(value -> String.valueOf(value).contains("13800138000"));
        assertThatThrownBy(() -> fields.put("message", "raw"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldClampAllDurationsAndCountersToNonNegativeValues() {
        AigcObservation observation = AigcObservation.start("req-2", 7L, "openai", "model-x");
        observation.recordSessionLoadMillis(-1);
        observation.recordDemandExtractionMillis(-2);
        observation.recordServiceQueryMillis(-3);
        observation.recordFirstDeltaMillis(-4);
        observation.finish("ERROR", "AIGC_MODEL_UNAVAILABLE", false, -5, -6, -7, -8, -9);

        assertThat(observation.toLogFields())
                .containsEntry("sessionLoadMillis", 0L)
                .containsEntry("demandExtractionMillis", 0L)
                .containsEntry("serviceQueryMillis", 0L)
                .containsEntry("firstDeltaMillis", 0L)
                .containsEntry("candidateCount", 0)
                .containsEntry("recommendationCount", 0)
                .containsEntry("modelCalls", 0)
                .containsEntry("retries", 0)
                .containsEntry("tokenUsage", 0L);
        assertThat((Long) observation.toLogFields().get("totalMillis")).isNotNegative();
    }

    @Test
    void shouldAcceptOnlyTheFirstTerminalOutcome() {
        AigcObservation observation = AigcObservation.start("req-3", 7L, "ollama", "qwen");

        assertThat(observation.finish("NO_MATCH", null, false, 0, 0, 1, 0, 0)).isTrue();
        assertThat(observation.finish("ERROR", "AIGC_MODEL_UNAVAILABLE", true, 9, 9, 9, 9, 9)).isFalse();

        assertThat(observation.toLogFields())
                .containsEntry("terminalStage", "NO_MATCH")
                .containsEntry("errorCode", null)
                .containsEntry("degraded", false)
                .containsEntry("modelCalls", 1);
    }

    @Test
    void loggerShouldRemainAnInjectableComponent() {
        assertThat(AigcObservationLogger.class).hasAnnotation(Component.class);
    }
}
