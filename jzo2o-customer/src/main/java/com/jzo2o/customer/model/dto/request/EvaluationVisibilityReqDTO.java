package com.jzo2o.customer.model.dto.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("评价展示状态更新请求")
public class EvaluationVisibilityReqDTO {
    @ApiModelProperty(value = "展示状态：0隐藏，1展示", required = true)
    private Integer visibleStatus;
}
