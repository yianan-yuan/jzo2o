package com.jzo2o.customer.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EvaluationTypeEnum {
    GOOD(1, "好评"),
    BAD(2, "差评");

    private final Integer code;
    private final String name;

    public static boolean isValid(Integer code) {
        return GOOD.code.equals(code) || BAD.code.equals(code);
    }
}
