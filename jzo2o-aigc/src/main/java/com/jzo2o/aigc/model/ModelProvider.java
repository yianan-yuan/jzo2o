package com.jzo2o.aigc.model;

import java.util.List;
import java.util.function.Consumer;

public interface ModelProvider {

    String complete(List<ModelMessage> messages, double temperature, CancellationToken cancellationToken);

    void stream(List<ModelMessage> messages,
                CancellationToken cancellationToken,
                Consumer<String> onDelta);
}
