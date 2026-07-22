package com.jzo2o.customer.controller.consumer;

import com.jzo2o.customer.model.dto.request.AiChatReqDTO;
import com.jzo2o.customer.model.dto.response.AiChatResDTO;
import com.jzo2o.customer.service.IAiChatService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;


@RestController("consumerAiChatController")
@RequestMapping("/consumer/ai")
@Api(tags = "用户端 - AI聊天接口")
public class AiChatController {

    @Resource
    private IAiChatService aiChatService;

    @PostMapping("/chat")
    @ApiOperation("发送聊天消息")
    public AiChatResDTO chat(@RequestBody @Valid AiChatReqDTO reqDTO) {
        return aiChatService.chat(reqDTO);
    }
}
