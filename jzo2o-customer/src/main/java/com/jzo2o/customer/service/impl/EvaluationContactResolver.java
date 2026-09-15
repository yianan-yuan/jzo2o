package com.jzo2o.customer.service.impl;

import cn.hutool.core.util.StrUtil;

/** 评价快照中的联系方式解析规则。 */
final class EvaluationContactResolver {
    private EvaluationContactResolver() {
    }

    static String resolvePhone(String consumerPhone, String contactsPhone) {
        return StrUtil.isNotBlank(consumerPhone) ? consumerPhone : contactsPhone;
    }
}
