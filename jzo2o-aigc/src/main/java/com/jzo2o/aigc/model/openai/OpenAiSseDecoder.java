package com.jzo2o.aigc.model.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Objects;

public final class OpenAiSseDecoder {

    private final ObjectMapper objectMapper;

    public OpenAiSseDecoder(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    public String decode(String payload) throws IOException {
        JsonNode root = objectMapper.readTree(payload);
        if (root == null || !root.isObject()) {
            throw new IOException("OpenAI response root is not an object");
        }
        JsonNode choices = root.get("choices");
        if (choices == null || !choices.isArray() || choices.isEmpty()) {
            throw new IOException("OpenAI response is missing choices");
        }
        JsonNode firstChoice = choices.get(0);
        if (firstChoice == null || !firstChoice.isObject()) {
            throw new IOException("OpenAI response first choice is not an object");
        }
        JsonNode delta = firstChoice.get("delta");
        if (delta == null || !delta.isObject()) {
            throw new IOException("OpenAI response is missing delta");
        }
        JsonNode content = delta.get("content");
        if (content == null || content.isNull()) {
            return null;
        }
        if (!content.isTextual()) {
            throw new IOException("OpenAI response delta content is not text");
        }
        return content.asText().isEmpty() ? null : content.asText();
    }
}
