package com.jzo2o.orders.dispatch.strategys.impl;

import com.jzo2o.orders.dispatch.annotations.DispatchStrategy;
import com.jzo2o.orders.dispatch.enums.DispatchStrategyEnum;
import com.jzo2o.orders.dispatch.rules.IDispatchRule;
import com.jzo2o.orders.dispatch.rules.impl.GoodRateDispatchRule;
import org.springframework.stereotype.Component;

@Component("goodRateDispatchStrategy")
@DispatchStrategy(DispatchStrategyEnum.GOOD_RATE)
public class GoodRateDispatchStrategyImpl extends AbstractDispatchStrategyImpl {

    @Override
    protected IDispatchRule getRules() {
        return new GoodRateDispatchRule(null);
    }
}
