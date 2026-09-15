package com.jzo2o.orders.dispatch.service;

import com.jzo2o.api.customer.dto.response.ServeProviderGoodRateResDTO;
import com.jzo2o.orders.dispatch.model.dto.ServeProviderDTO;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoodRateCandidateEnricherTest {

    @Test
    void appliesReturnedRatesAndTreatsMissingWorkersAsUnrated() {
        ServeProviderDTO rated = candidate(101L);
        ServeProviderDTO missing = candidate(202L);
        Map<Long, ServeProviderGoodRateResDTO> rates = new HashMap<>();
        rates.put(101L, new ServeProviderGoodRateResDTO(true, 80D));

        GoodRateCandidateEnricher.apply(Arrays.asList(rated, missing), rates);

        assertTrue(rated.getHasEvaluation());
        assertEquals(80D, rated.getGoodRate());
        assertFalse(missing.getHasEvaluation());
        assertEquals(null, missing.getGoodRate());
    }

    @Test
    void degradesEveryCandidateToUnratedWhenRateLookupFails() {
        ServeProviderDTO candidate = candidate(101L);
        candidate.setHasEvaluation(true);
        candidate.setGoodRate(100D);

        GoodRateCandidateEnricher.apply(candidate == null ? Collections.emptyList() : Collections.singletonList(candidate), null);

        assertFalse(candidate.getHasEvaluation());
        assertEquals(null, candidate.getGoodRate());
    }

    private ServeProviderDTO candidate(Long id) {
        ServeProviderDTO candidate = new ServeProviderDTO();
        candidate.setId(id);
        return candidate;
    }
}
