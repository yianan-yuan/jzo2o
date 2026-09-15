package com.jzo2o.customer.model.dto.request;

import com.jzo2o.common.model.dto.PageQueryDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 统一评价分页查询条件。 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("统一评价分页查询")
public class UnifiedEvaluationPageReqDTO extends PageQueryDTO {
    @ApiModelProperty("评价类型：1好评，2差评")
    private Integer evaluationType;

    @ApiModelProperty("管理端可传：0隐藏，1展示")
    private Integer visibleStatus;

    @ApiModelProperty("订单编号")
    private String ordersNo;

    @ApiModelProperty("客户电话")
    private String consumerPhone;

    @ApiModelProperty("服务人员id")
    private Long serveProviderId;
}
