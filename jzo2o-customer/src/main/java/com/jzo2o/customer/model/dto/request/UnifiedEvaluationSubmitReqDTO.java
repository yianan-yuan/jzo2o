package com.jzo2o.customer.model.dto.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/** 客户对本次上门服务的统一评价。 */
@Data
@ApiModel("统一评价提交请求")
public class UnifiedEvaluationSubmitReqDTO {
    @ApiModelProperty(value = "订单id", required = true)
    private Long ordersId;

    @ApiModelProperty(value = "评价类型：1好评，2差评", required = true)
    private Integer evaluationType;

    @ApiModelProperty(value = "评价内容", required = true)
    private String content;

    @ApiModelProperty("评价图片，可不传")
    private String[] pictureArray;

    @ApiModelProperty("是否匿名：0否，1是")
    private Integer isAnonymous = 0;
}
