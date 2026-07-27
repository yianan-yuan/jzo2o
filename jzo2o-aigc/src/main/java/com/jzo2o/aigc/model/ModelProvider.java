package com.jzo2o.aigc.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.function.Consumer;

public interface ModelProvider {

    String complete(List<ModelMessage> messages, double temperature, CancellationToken cancellationToken);

    default String completeJson(List<ModelMessage> messages,
                                double temperature,
                                CancellationToken cancellationToken,
                                JsonNode schema) {
        return complete(messages, temperature, cancellationToken);
    }

    void stream(List<ModelMessage> messages,
                CancellationToken cancellationToken,
                Consumer<String> onDelta);
}
