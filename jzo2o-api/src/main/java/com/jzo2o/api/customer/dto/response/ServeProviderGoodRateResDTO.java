package com.jzo2o.api.customer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServeProviderGoodRateResDTO {
    private Boolean hasEvaluation;
    private Double goodRate;
}
