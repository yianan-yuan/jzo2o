package com.jzo2o.aigc.stream;

import com.jzo2o.aigc.controller.consumer.dto.RecommendationCardDTO;
import com.jzo2o.aigc.domain.ConversationStage;
import com.jzo2o.aigc.exception.AigcErrorCode;

import java.util.List;

public interface SseEventSink {

    void status(String stage);

    void delta(String text);

    void recommendations(List<RecommendationCardDTO> cards);

    void done(ConversationStage stage, List<String> suggestedQuestions);

    void error(AigcErrorCode errorCode, String message);
}
