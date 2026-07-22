package com.jzo2o.aigc.model.ollama;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Objects;

public final class OllamaStreamDecoder {

    private final ObjectMapper objectMapper;

    public OllamaStreamDecoder(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    public String decode(String line) throws IOException {
        JsonNode root = objectMapper.readTree(line);
        JsonNode message = root.get("message");
        if (message == null || !message.isObject()) {
            throw new IOException("Ollama response is missing message");
        }
        JsonNode content = message.get("content");
        if (content == null || !content.isTextual() || content.asText().isEmpty()) {
            return null;
        }
        return content.asText();
    }
}
