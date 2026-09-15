package com.jzo2o.orders.dispatch.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServeProviderDTO {
    /**
     * 服务人员或机构id
     */
    private Long id;
    /**
     * 类型，2：服务人员，3：机构
     */
    private Integer serveProviderType;
    /**
     * 评分
     */
    private Integer evaluationScore;
    /**
     * 是否存在可展示的好评或差评。
     */
    private Boolean hasEvaluation;
    /**
     * 可展示评价计算出的好评率，取值范围 0 至 100。
     */
    private Double goodRate;
    /**
     * 当前接单数量
     */
    private Integer acceptanceNum;
    /**
     * 接单距离
     */
    private Integer acceptanceDistance;
}
