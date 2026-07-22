package com.jzo2o.customer.service;

import com.jzo2o.customer.model.dto.request.AiChatReqDTO;
import com.jzo2o.customer.model.dto.response.AiChatResDTO;

/**
 * AI聊天服务接口
 *
 * @author itcast
 */
public interface IAiChatService {

    /**
     * 发送聊天消息并获取AI回复
     *
     * @param reqDTO 请求参数
     * @return AI回复
     */
    AiChatResDTO chat(AiChatReqDTO reqDTO);
}
