package com.jzo2o.aigc.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

@Data
@Validated
@ConfigurationProperties(prefix = "jzo2o.aigc")
public class AigcProperties {

    @Min(1)
    private int sessionTtlMinutes = 30;

    @Min(1)
    private int maxRounds = 10;

    @Min(1)
    private int maxMessageLength = 1000;

    @Min(1)
    private int requestsPerMinute = 10;

    @Min(1)
    private int maxCandidates = 20;

    @Min(1)
    private int maxRecommendations = 3;

    @Min(0)
    private int maxSuggestedQuestions = 3;

    @Min(1)
    private int firstTokenTimeoutSeconds = 30;

    @Min(1)
    private int totalTimeoutSeconds = 90;

    @Valid
    private Model model = new Model();

    @Data
    public static class Model {

        @NotBlank
        private String provider = "ollama";

        @NotBlank
        private String baseUrl = "http://localhost:11434";

        private String apiKey;

        @NotBlank
        private String model = "qwen3:0.6b";

        private double temperature = 0.2D;

        private int maxTokens = 1024;
    }
}
