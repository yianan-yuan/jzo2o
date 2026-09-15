package com.jzo2o.customer.service;

import com.jzo2o.customer.model.dto.response.EvaluationRateResDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvaluationRateResDTOTest {

    @Test
    void visibleGoodAndBadEvaluationsProduceOneDecimalGoodRate() {
        EvaluationRateResDTO rate = EvaluationRateResDTO.of(1L, 2L);

        assertTrue(rate.getHasEvaluation());
        assertEquals("33.3%", rate.getDisplayRate());
        assertEquals(33.3D, rate.getGoodRate());
    }

    @Test
    void noVisibleEvaluationsDoesNotPretendTheRateIsZero() {
        EvaluationRateResDTO rate = EvaluationRateResDTO.of(0L, 0L);

        assertFalse(rate.getHasEvaluation());
        assertEquals("暂无评价", rate.getDisplayRate());
        assertEquals(null, rate.getGoodRate());
    }
}
