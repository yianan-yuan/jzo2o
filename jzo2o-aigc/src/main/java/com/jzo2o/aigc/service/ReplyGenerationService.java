package com.jzo2o.aigc.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jzo2o.aigc.controller.consumer.dto.RecommendationCardDTO;
import com.jzo2o.aigc.domain.AigcSession;
import com.jzo2o.aigc.exception.AigcErrorCode;
import com.jzo2o.aigc.exception.AigcException;
import com.jzo2o.aigc.model.CancellationToken;
import com.jzo2o.aigc.model.ModelMessage;
import com.jzo2o.aigc.model.ModelProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class ReplyGenerationService {

    private static final String SYSTEM_PROMPT = "你是家政服务说明助手。只能解释用户画像和权威服务卡片中的事实，"
            + "不得修改名称、价格、单位或图片，不得生成新卡片、创建订单或承诺服务。只输出简短说明文本。";

    private final ModelProvider provider;
    private final ObjectMapper objectMapper;

    public void streamReply(AigcSession session,
                            List<RecommendationCardDTO> cards,
                            CancellationToken cancellationToken,
                            Consumer<String> onDelta) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("profile", session.getDemandProfile());
        payload.put("cards", cards);
        try {
            provider.stream(Arrays.asList(
                    new ModelMessage("system", SYSTEM_PROMPT),
                    new ModelMessage("user", objectMapper.writeValueAsString(payload))),
                    cancellationToken, onDelta);
        } catch (JsonProcessingException error) {
            throw new AigcException(AigcErrorCode.MODEL_OUTPUT_INVALID);
        }
    }
}
