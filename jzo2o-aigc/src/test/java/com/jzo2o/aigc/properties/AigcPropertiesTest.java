package com.jzo2o.aigc.properties;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AigcPropertiesTest {

    @Test
    void shouldExposeApprovedDefaults() {
        AigcProperties p = new AigcProperties();

        assertThat(p.getSessionTtlMinutes()).isEqualTo(30);
        assertThat(p.getMaxRounds()).isEqualTo(10);
        assertThat(p.getMaxMessageLength()).isEqualTo(1000);
        assertThat(p.getRequestsPerMinute()).isEqualTo(10);
        assertThat(p.getMaxCandidates()).isEqualTo(20);
        assertThat(p.getMaxRecommendations()).isEqualTo(3);
        assertThat(p.getMaxSuggestedQuestions()).isEqualTo(3);
        assertThat(p.getFirstTokenTimeoutSeconds()).isEqualTo(30);
        assertThat(p.getTotalTimeoutSeconds()).isEqualTo(90);
        assertThat(p.getModel().getProvider()).isEqualTo("ollama");
    }
}
