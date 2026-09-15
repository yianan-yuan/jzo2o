package com.jzo2o.orders.dispatch.rules;

import com.jzo2o.orders.dispatch.model.dto.ServeProviderDTO;
import com.jzo2o.orders.dispatch.rules.impl.GoodRateDispatchRule;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GoodRateDispatchRuleTest {

    @Test
    void selectsRatedWorkerByRateThenAcceptanceDistanceAndId() {
        List<ServeProviderDTO> candidates = Arrays.asList(
                worker(30L, true, 90D, 1, 5),
                worker(20L, true, 90D, 1, 3),
                worker(10L, false, null, 0, 1));

        List<ServeProviderDTO> result = new GoodRateDispatchRule(null).doFilter(candidates);

        assertEquals(1, result.size());
        assertEquals(20L, result.get(0).getId());
    }

    @Test
    void selectsRatedWorkerBeforeUnratedWorkerEvenWhenRateIsZero() {
        List<ServeProviderDTO> result = new GoodRateDispatchRule(null).doFilter(Arrays.asList(
                worker(10L, false, null, 0, 1),
                worker(20L, true, 0D, 5, 10)));

        assertEquals(20L, result.get(0).getId());
    }

    private ServeProviderDTO worker(Long id, boolean hasEvaluation, Double goodRate,
                                    Integer acceptanceNum, Integer acceptanceDistance) {
        ServeProviderDTO worker = new ServeProviderDTO();
        worker.setId(id);
        worker.setHasEvaluation(hasEvaluation);
        worker.setGoodRate(goodRate);
        worker.setAcceptanceNum(acceptanceNum);
        worker.setAcceptanceDistance(acceptanceDistance);
        return worker;
    }
}
