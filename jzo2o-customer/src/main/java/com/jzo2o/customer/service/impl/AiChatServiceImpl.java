package com.jzo2o.customer.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.jzo2o.common.expcetions.CommonException;
import com.jzo2o.customer.model.dto.request.AiChatReqDTO;
import com.jzo2o.customer.model.dto.response.AiChatResDTO;
import com.jzo2o.customer.properties.AiChatProperties;
import com.jzo2o.customer.service.IAiChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * AI聊天服务实现 - 对接Ollama API
 *
 * @author itcast
 */
@Slf4j
@Service
public class AiChatServiceImpl implements IAiChatService {

    @Resource
    private AiChatProperties aiChatProperties;

    private static final String SYSTEM_PROMPT = "你是一个友好的AI助手，请用简洁清晰的中文回答用户的问题。";

    @Override
    public AiChatResDTO chat(AiChatReqDTO reqDTO) {
        try {
            // 构建Ollama API请求体
            JSONObject body = new JSONObject();
            body.set("model", aiChatProperties.getModel());
            body.set("stream", false);

            // 构建消息列表
            JSONArray messages = new JSONArray();

            JSONObject systemMsg = new JSONObject();
            systemMsg.set("role", "system");
            systemMsg.set("content", SYSTEM_PROMPT);
            messages.add(systemMsg);

            JSONObject userMsg = new JSONObject();
            userMsg.set("role", "user");
            userMsg.set("content", reqDTO.getMessage());
            messages.add(userMsg);

            body.set("messages", messages);

            // 设置可选参数
            JSONObject options = new JSONObject();
            if (aiChatProperties.getTemperature() != null) {
                options.set("temperature", aiChatProperties.getTemperature());
            }
            if (aiChatProperties.getMaxTokens() != null) {
                options.set("num_predict", aiChatProperties.getMaxTokens());
            }
            body.set("options", options);

            // 调用Ollama API
            String url = aiChatProperties.getBaseUrl() + "/api/chat";
            log.debug("Calling Ollama API: {}", url);

            String result = HttpRequest.post(url)
                    .body(body.toString())
                    .setConnectionTimeout(aiChatProperties.getTimeout())
                    .setReadTimeout(aiChatProperties.getTimeout())
                    .execute()
                    .body();

            log.debug("Ollama response: {}", result);

            // 解析响应
            JSONObject response = JSONUtil.parseObj(result);
            JSONObject message = response.getJSONObject("message");
            if (message == null) {
                throw new CommonException("AI服务返回格式异常");
            }

            String content = message.getStr("content");
            AiChatResDTO resDTO = new AiChatResDTO();
            resDTO.setReply(content);
            return resDTO;

        } catch (CommonException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI聊天服务调用失败", e);
            throw new CommonException("AI服务暂时不可用，请稍后再试");
        }
    }
}
