package com.jzo2o.customer.controller.operation;

import com.jzo2o.customer.model.dto.response.EvaluationRateResDTO;
import com.jzo2o.customer.service.UnifiedEvaluationService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EvaluationControllerTest {

    @Test
    void summary_returnsTheSelectedWorkersGoodRate() {
        UnifiedEvaluationService unifiedEvaluationService = mock(UnifiedEvaluationService.class);
        EvaluationRateResDTO expected = new EvaluationRateResDTO(true, 87.5D, "87.5%");
        when(unifiedEvaluationService.rateForWorker(1001L)).thenReturn(expected);

        EvaluationController controller = new EvaluationController();
        ReflectionTestUtils.setField(controller, "unifiedEvaluationService", unifiedEvaluationService);

        assertSame(expected, controller.summary(1001L));
    }
}
