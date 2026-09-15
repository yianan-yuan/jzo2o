package com.jzo2o.customer.service.impl;

import com.jzo2o.customer.model.domain.EvaluationRecord;
import com.jzo2o.customer.model.dto.response.EvaluationRateResDTO;
import com.jzo2o.customer.model.enums.EvaluationTypeEnum;
import com.jzo2o.customer.model.enums.EvaluationVisibilityEnum;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class EvaluationRateBatchCalculator {

    private EvaluationRateBatchCalculator() {
    }

    static Map<Long, EvaluationRateResDTO> calculate(List<Long> workerIds,
                                                       List<EvaluationRecord> records) {
        Map<Long, long[]> counts = new LinkedHashMap<>();
        for (Long workerId : workerIds) {
            if (workerId != null) {
                counts.putIfAbsent(workerId, new long[2]);
            }
        }
        for (EvaluationRecord record : records) {
            long[] count = counts.get(record.getServeProviderId());
            if (count == null || !EvaluationVisibilityEnum.VISIBLE.getCode().equals(record.getVisibleStatus())) {
                continue;
            }
            if (EvaluationTypeEnum.GOOD.getCode().equals(record.getEvaluationType())) {
                count[0]++;
            } else if (EvaluationTypeEnum.BAD.getCode().equals(record.getEvaluationType())) {
                count[1]++;
            }
        }
        Map<Long, EvaluationRateResDTO> result = new LinkedHashMap<>();
        counts.forEach((workerId, count) -> result.put(workerId, EvaluationRateResDTO.of(count[0], count[1])));
        return result;
    }
}
