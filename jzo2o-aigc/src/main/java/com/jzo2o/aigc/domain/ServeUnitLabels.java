package com.jzo2o.aigc.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ServeUnitLabels {

    private static final Map<Integer, String> LABELS;

    static {
        Map<Integer, String> labels = new LinkedHashMap<>();
        labels.put(1, "小时");
        labels.put(2, "天");
        labels.put(3, "次");
        labels.put(4, "台");
        labels.put(5, "个");
        labels.put(6, "㎡");
        labels.put(7, "米");
        LABELS = Collections.unmodifiableMap(labels);
    }

    private ServeUnitLabels() {
    }

    public static String labelOf(Integer unit) {
        return LABELS.get(unit);
    }
}
