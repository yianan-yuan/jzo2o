package com.jzo2o.aigc.controller.consumer.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class AssistantMessageReqDTO {

    @NotBlank
    @Size(max = 1000)
    private String message;

    @NotBlank
    @Pattern(regexp = "[0-9]{3,6}")
    private String cityCode;
}
