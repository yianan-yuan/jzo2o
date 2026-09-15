package com.jzo2o.aigc.controller.consumer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationCardDTO {

    private Long serveId;
    private String serveItemName;
    private String serveItemImg;
    private BigDecimal price;
    private String priceUnit;
    private String recommendationReason;
    private String actionType;
}
