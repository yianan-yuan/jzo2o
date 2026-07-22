package com.jzo2o.aigc.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatTurn implements Serializable {

    private static final long serialVersionUID = 1L;

    private String role;
    private String content;
}
