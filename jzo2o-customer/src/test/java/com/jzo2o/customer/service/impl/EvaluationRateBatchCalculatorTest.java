package com.jzo2o.customer.service.impl;

import com.jzo2o.customer.model.domain.EvaluationRecord;
import com.jzo2o.customer.model.dto.response.EvaluationRateResDTO;
import com.jzo2o.customer.model.enums.EvaluationTypeEnum;
import com.jzo2o.customer.model.enums.EvaluationVisibilityEnum;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvaluationRateBatchCalculatorTest {

    @Test
    void calculatesOnlyVisibleGoodAndBadRecordsForEveryRequestedWorker() {
        List<EvaluationRecord> records = Arrays.asList(
                record(101L, EvaluationTypeEnum.GOOD, EvaluationVisibilityEnum.VISIBLE),
                record(101L, EvaluationTypeEnum.GOOD, EvaluationVisibilityEnum.VISIBLE),
                record(101L, EvaluationTypeEnum.BAD, EvaluationVisibilityEnum.VISIBLE),
                record(101L, EvaluationTypeEnum.BAD, EvaluationVisibilityEnum.HIDDEN),
                record(303L, EvaluationTypeEnum.BAD, EvaluationVisibilityEnum.VISIBLE));

        Map<Long, EvaluationRateResDTO> rates = EvaluationRateBatchCalculator.calculate(
                Arrays.asList(101L, 202L, 303L), records);

        assertTrue(rates.get(101L).getHasEvaluation());
        assertEquals(66.7D, rates.get(101L).getGoodRate());
        assertFalse(rates.get(202L).getHasEvaluation());
        assertEquals(0.0D, rates.get(303L).getGoodRate());
    }

    private EvaluationRecord record(Long workerId, EvaluationTypeEnum type,
                                    EvaluationVisibilityEnum visibility) {
        EvaluationRecord record = new EvaluationRecord();
        record.setServeProviderId(workerId);
        record.setEvaluationType(type.getCode());
        record.setVisibleStatus(visibility.getCode());
        return record;
    }
}
