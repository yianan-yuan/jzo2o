package com.jzo2o.aigc.service;

import com.jzo2o.aigc.controller.consumer.dto.RecommendationCardDTO;
import com.jzo2o.aigc.domain.AigcSession;
import com.jzo2o.aigc.exception.AigcErrorCode;
import com.jzo2o.aigc.exception.AigcException;
import com.jzo2o.aigc.model.CancellationToken;
import com.jzo2o.aigc.model.ModelProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class ReplyGenerationService {

    private final ModelProvider provider;
    private final PromptFactory promptFactory;

    public void streamReply(AigcSession session,
                            List<RecommendationCardDTO> cards,
                            CancellationToken cancellationToken,
                            Consumer<String> onDelta) {
        AtomicBoolean emittedEffectiveDelta = new AtomicBoolean();
        Consumer<String> effectiveDelta = text -> {
            if (text != null && !text.trim().isEmpty()) {
                onDelta.accept(text);
                emittedEffectiveDelta.set(true);
            }
        };
        provider.stream(promptFactory.replyMessages(session.getDemandProfile(), cards),
                cancellationToken, effectiveDelta);
        if (!emittedEffectiveDelta.get() && !cancellationToken.isCancelled()) {
            throw new AigcException(AigcErrorCode.MODEL_UNAVAILABLE);
        }
    }
}
