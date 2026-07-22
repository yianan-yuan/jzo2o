package com.jzo2o.customer.model.dto.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * AI聊天响应DTO
 *
 * @author itcast
 */
@Data
@ApiModel("AI聊天响应")
public class AiChatResDTO {

    @ApiModelProperty("AI回复内容")
    private String reply;
}
