package com.jzo2o.aigc.model.ollama;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jzo2o.aigc.exception.AigcErrorCode;
import com.jzo2o.aigc.exception.AigcException;
import com.jzo2o.aigc.model.CancellationToken;
import com.jzo2o.aigc.model.ModelMessage;
import com.jzo2o.aigc.model.ModelProvider;
import com.jzo2o.aigc.properties.AigcProperties;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class OllamaModelProvider implements ModelProvider {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AigcProperties properties;
    private final OllamaStreamDecoder streamDecoder;

    public OllamaModelProvider(HttpClient httpClient, ObjectMapper objectMapper, AigcProperties properties) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.properties = Objects.requireNonNull(properties, "properties");
        this.streamDecoder = new OllamaStreamDecoder(objectMapper);
    }

    @Override
    public String complete(List<ModelMessage> messages,
                           double temperature,
                           CancellationToken cancellationToken) {
        return complete(messages, temperature, cancellationToken, null);
    }

    @Override
    public String completeJson(List<ModelMessage> messages,
                               double temperature,
                               CancellationToken cancellationToken,
                               JsonNode schema) {
        Objects.requireNonNull(schema, "schema");
        return complete(messages, temperature, cancellationToken, schema);
    }

    private String complete(List<ModelMessage> messages,
                            double temperature,
                            CancellationToken cancellationToken,
                            JsonNode schema) {
        requireArguments(messages, cancellationToken);
        if (cancellationToken.isCancelled()) {
            return "";
        }
        try {
            HttpResponse<InputStream> response = send(messages, false, temperature, schema);
            try (InputStream body = response.body()) {
                ensureSuccessful(response.statusCode());
                cancellationToken.onCancel(() -> closeQuietly(body));
                if (cancellationToken.isCancelled()) {
                    return "";
                }
                JsonNode root = objectMapper.readTree(body);
                if (root == null || !root.isObject()) {
                    throw unavailable("Ollama response root is not an object");
                }
                JsonNode message = root.get("message");
                if (message == null || !message.isObject()) {
                    throw unavailable("Ollama response is missing message");
                }
                JsonNode content = message.get("content");
                if (content == null || !content.isTextual()) {
                    throw unavailable("Ollama response is missing message content");
                }
                return content.asText();
            }
        } catch (AigcException error) {
            throw error;
        } catch (IOException error) {
            if (cancellationToken.isCancelled()) {
                return "";
            }
            throw unavailable(error.getMessage());
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            if (cancellationToken.isCancelled()) {
                return "";
            }
            throw unavailable(error.getMessage());
        }
    }

    @Override
    public void stream(List<ModelMessage> messages,
                       CancellationToken cancellationToken,
                       Consumer<String> onDelta) {
        requireArguments(messages, cancellationToken);
        Objects.requireNonNull(onDelta, "onDelta");
        if (cancellationToken.isCancelled()) {
            return;
        }
        try {
            HttpResponse<InputStream> response = send(
                    messages, true, properties.getModel().getTemperature(), null);
            try (InputStream body = response.body();
                 BufferedReader reader = new BufferedReader(
                         new InputStreamReader(body, StandardCharsets.UTF_8))) {
                ensureSuccessful(response.statusCode());
                cancellationToken.onCancel(() -> closeQuietly(body));
                while (!cancellationToken.isCancelled()) {
                    String line = reader.readLine();
                    if (line == null || cancellationToken.isCancelled()) {
                        break;
                    }
                    if (line.trim().isEmpty()) {
                        continue;
                    }
                    String delta = streamDecoder.decode(line);
                    if (delta != null) {
                        onDelta.accept(delta);
                        if (cancellationToken.isCancelled()) {
                            break;
                        }
                    }
                }
            }
        } catch (AigcException error) {
            throw error;
        } catch (IOException error) {
            if (!cancellationToken.isCancelled()) {
                throw unavailable(error.getMessage());
            }
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            if (!cancellationToken.isCancelled()) {
                throw unavailable(error.getMessage());
            }
        }
    }

    private HttpResponse<InputStream> send(List<ModelMessage> messages,
                                           boolean stream,
                                           double temperature,
                                           JsonNode schema) throws IOException, InterruptedException {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", properties.getModel().getModel());
        ArrayNode requestMessages = root.putArray("messages");
        for (ModelMessage message : messages) {
            ObjectNode requestMessage = requestMessages.addObject();
            requestMessage.put("role", message.getRole());
            requestMessage.put("content", message.getContent());
        }
        root.put("stream", stream);
        if (schema != null) {
            root.set("format", schema);
        }
        ObjectNode options = root.putObject("options");
        options.put("temperature", temperature);
        options.put("num_predict", properties.getModel().getMaxTokens());

        HttpRequest request = HttpRequest.newBuilder(chatUri())
                .timeout(Duration.ofSeconds(properties.getTotalTimeoutSeconds()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(root), StandardCharsets.UTF_8))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
    }

    private URI chatUri() {
        String baseUrl = properties.getModel().getBaseUrl();
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return URI.create(baseUrl + "/api/chat");
    }

    private void ensureSuccessful(int statusCode) {
        if (statusCode < 200 || statusCode >= 300) {
            throw unavailable("Ollama returned HTTP " + statusCode);
        }
    }

    private void requireArguments(List<ModelMessage> messages, CancellationToken cancellationToken) {
        Objects.requireNonNull(messages, "messages");
        Objects.requireNonNull(cancellationToken, "cancellationToken");
    }

    private AigcException unavailable(String detail) {
        String message = detail == null || detail.isEmpty()
                ? AigcErrorCode.MODEL_UNAVAILABLE.getCode()
                : AigcErrorCode.MODEL_UNAVAILABLE.getCode() + ": " + detail;
        return new AigcException(AigcErrorCode.MODEL_UNAVAILABLE, message);
    }

    private void closeQuietly(InputStream stream) {
        try {
            stream.close();
        } catch (IOException ignored) {
            // Cancellation closes the response body best-effort.
        }
    }
}
