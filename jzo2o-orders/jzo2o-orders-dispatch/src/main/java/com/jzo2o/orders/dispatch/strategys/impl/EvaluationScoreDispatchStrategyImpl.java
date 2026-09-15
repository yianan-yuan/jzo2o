package com.jzo2o.orders.dispatch.strategys.impl;

import com.jzo2o.common.utils.ComparatorUtils;
import com.jzo2o.orders.dispatch.model.dto.ServeProviderDTO;
import com.jzo2o.orders.dispatch.rules.IDispatchRule;
import com.jzo2o.orders.dispatch.rules.impl.AcceptNumDispatchRule;
import com.jzo2o.orders.dispatch.rules.impl.DefaultIDispatchRule;
import com.jzo2o.orders.dispatch.rules.impl.EvaluationScoreDispatchRule;

/**
 * 历史评分策略，仅保留源码兼容；当前策略类型 2 已切换为好评率优先。
 * 先根据评分规则获取高优先级的服务人员或机构，评分越高优先级越高，如果存在多个最高优先级的服务人员或机构再去进行最少
 * 按评分计算->按最少接单数计算
 */
@Deprecated
public class EvaluationScoreDispatchStrategyImpl extends AbstractDispatchStrategyImpl{
    @Override
    protected IDispatchRule getRules() {
        // 最少接单规则，数量越少优先级越高
        IDispatchRule acceptNumDispatchRule = new AcceptNumDispatchRule(null);
        // 按评分计算规则,评分越高优先级越高
        IDispatchRule evaluationScoreDispatchRule = new EvaluationScoreDispatchRule(acceptNumDispatchRule);
        return evaluationScoreDispatchRule;
//        // 最少接单规则，数量越少优先级越高
//        DefaultIDispatchRule leastAcceptOrderNumDispatchRule = new DefaultIDispatchRule(null, ComparatorUtils.nullToFirstComparing(ServeProviderDTO::getAcceptanceNum));
//        // 按评分计算规则,评分越高优先级越高
//        IDispatchRule evaluationScoreDispatchRule = new DefaultIDispatchRule(leastAcceptOrderNumDispatchRule, ComparatorUtils.nullToFirstComparing(ServeProviderDTO::getEvaluationScore).reversed());
//        return evaluationScoreDispatchRule;
    }
}
