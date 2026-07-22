package com.jzo2o.aigc.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jzo2o.aigc.model.ModelProvider;
import com.jzo2o.aigc.model.ollama.OllamaModelProvider;
import com.jzo2o.aigc.model.openai.OpenAiCompatibleModelProvider;
import com.jzo2o.aigc.properties.AigcProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;

@Configuration
public class ModelProviderConfiguration {

    @Bean
    public HttpClient modelHttpClient() {
        return HttpClient.newHttpClient();
    }

    @Bean
    public ModelProvider modelProvider(
            HttpClient httpClient, ObjectMapper objectMapper, AigcProperties properties) {
        switch (properties.getModel().getProvider()) {
            case "ollama":
                return new OllamaModelProvider(httpClient, objectMapper, properties);
            case "openai-compatible":
                return new OpenAiCompatibleModelProvider(httpClient, objectMapper, properties);
            default:
                throw new IllegalArgumentException("Unsupported model provider: "
                        + properties.getModel().getProvider());
        }
    }
}
