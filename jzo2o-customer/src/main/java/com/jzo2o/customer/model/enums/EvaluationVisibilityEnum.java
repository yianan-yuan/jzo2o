package com.jzo2o.customer.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EvaluationVisibilityEnum {
    HIDDEN(0, "已隐藏"),
    VISIBLE(1, "可见");

    private final Integer code;
    private final String name;

    public static boolean isValid(Integer code) {
        return HIDDEN.code.equals(code) || VISIBLE.code.equals(code);
    }
}
