package com.jzo2o.customer.model.dto.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * AI聊天请求DTO
 *
 * @author itcast
 */
@Data
@ApiModel("AI聊天请求")
public class AiChatReqDTO {

    @NotBlank(message = "消息内容不能为空")
    @ApiModelProperty(value = "用户消息内容", required = true)
    private String message;
}
