package com.jzo2o.aigc.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jzo2o.aigc.model.ModelProvider;
import com.jzo2o.aigc.model.ollama.OllamaModelProvider;
import com.jzo2o.aigc.model.openai.OpenAiCompatibleModelProvider;
import com.jzo2o.aigc.properties.AigcProperties;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.net.http.HttpClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModelProviderConfigurationTest {

    private static final Logger SPRING_LOGGER =
            (Logger) LoggerFactory.getLogger("org.springframework");
    private static Level originalSpringLogLevel;

    @BeforeAll
    static void suppressSpringDebugLogging() {
        originalSpringLogLevel = SPRING_LOGGER.getLevel();
        SPRING_LOGGER.setLevel(Level.WARN);
    }

    @AfterAll
    static void restoreSpringLogging() {
        SPRING_LOGGER.setLevel(originalSpringLogLevel);
    }

    @Test
    void ollamaSelectionCreatesExactlyOneOllamaProviderBean() {
        assertSelectedProvider("ollama", OllamaModelProvider.class);
    }

    @Test
    void openAiSelectionCreatesExactlyOneOpenAiProviderBean() {
        assertSelectedProvider("openai-compatible", OpenAiCompatibleModelProvider.class);
    }

    @Test
    void unknownProviderThrowsExactIllegalArgumentException() {
        AigcProperties properties = propertiesWithProvider("unsupported");
        ModelProviderConfiguration configuration = new ModelProviderConfiguration();

        assertThatThrownBy(() -> configuration.modelProvider(
                HttpClient.newHttpClient(), new ObjectMapper(), properties))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported model provider: unsupported");
    }

    private void assertSelectedProvider(String providerName, Class<? extends ModelProvider> expectedType) {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(ModelProviderConfiguration.class);
            context.registerBean(ObjectMapper.class, () -> new ObjectMapper());
            context.registerBean(AigcProperties.class, () -> propertiesWithProvider(providerName));
            context.refresh();

            assertThat(context.getBeansOfType(ModelProvider.class)).hasSize(1);
            assertThat(context.getBean(ModelProvider.class)).isExactlyInstanceOf(expectedType);
        }
    }

    private AigcProperties propertiesWithProvider(String providerName) {
        AigcProperties properties = new AigcProperties();
        properties.getModel().setProvider(providerName);
        return properties;
    }
}
