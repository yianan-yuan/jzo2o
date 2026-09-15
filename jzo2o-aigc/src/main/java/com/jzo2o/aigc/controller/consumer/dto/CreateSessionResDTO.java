package com.jzo2o.aigc.controller.consumer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreateSessionResDTO {

    private String sessionId;
    private int expiresInSeconds;
}
