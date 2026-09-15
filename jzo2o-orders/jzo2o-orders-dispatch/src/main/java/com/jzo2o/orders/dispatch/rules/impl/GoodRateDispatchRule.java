package com.jzo2o.orders.dispatch.rules.impl;

import com.jzo2o.common.utils.CollUtils;
import com.jzo2o.orders.dispatch.model.dto.ServeProviderDTO;
import com.jzo2o.orders.dispatch.rules.IDispatchRule;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 好评率优先规则：已评价优先，再按好评率、接单量、距离和服务人员 ID 排序。
 */
public class GoodRateDispatchRule extends AbstractIDispatchRule {

    public GoodRateDispatchRule(IDispatchRule next) {
        super(next);
    }

    @Override
    public List<ServeProviderDTO> doFilter(List<ServeProviderDTO> candidates) {
        if (CollUtils.size(candidates) < 2) {
            return candidates;
        }
        Comparator<ServeProviderDTO> order = Comparator
                .comparing(ServeProviderDTO::getHasEvaluation, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ServeProviderDTO::getGoodRate, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ServeProviderDTO::getAcceptanceNum, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ServeProviderDTO::getAcceptanceDistance, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ServeProviderDTO::getId, Comparator.nullsLast(Comparator.naturalOrder()));
        ServeProviderDTO precedence = candidates.stream().sorted(order).findFirst().orElse(null);
        return precedence == null ? candidates : candidates.stream()
                .filter(candidate -> candidate == precedence)
                .collect(Collectors.toList());
    }
}
