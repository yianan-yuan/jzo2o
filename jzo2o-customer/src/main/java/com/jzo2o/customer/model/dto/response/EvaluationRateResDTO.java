package com.jzo2o.customer.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 服务人员可见评价的好评率统计。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationRateResDTO {

    private Boolean hasEvaluation;

    private Double goodRate;

    private String displayRate;

    public static EvaluationRateResDTO of(long goodCount, long badCount) {
        long total = goodCount + badCount;
        if (total == 0) {
            return new EvaluationRateResDTO(false, null, "暂无评价");
        }
        double rate = BigDecimal.valueOf(goodCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP)
                .doubleValue();
        return new EvaluationRateResDTO(true, rate, rate + "%");
    }
}
