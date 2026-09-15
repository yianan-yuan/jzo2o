package com.jzo2o.orders.dispatch.service;

import com.jzo2o.api.customer.dto.response.ServeProviderGoodRateResDTO;
import com.jzo2o.orders.dispatch.model.dto.ServeProviderDTO;

import java.util.List;
import java.util.Map;

public final class GoodRateCandidateEnricher {

    private GoodRateCandidateEnricher() {
    }

    public static void apply(List<ServeProviderDTO> candidates,
                             Map<Long, ServeProviderGoodRateResDTO> goodRates) {
        for (ServeProviderDTO candidate : candidates) {
            ServeProviderGoodRateResDTO rate = goodRates == null ? null : goodRates.get(candidate.getId());
            candidate.setHasEvaluation(rate != null && Boolean.TRUE.equals(rate.getHasEvaluation()));
            candidate.setGoodRate(rate == null ? null : rate.getGoodRate());
        }
    }
}
