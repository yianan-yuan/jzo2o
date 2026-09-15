package com.jzo2o.customer.model.dto.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/** 客户、服务人员和管理端共用的评价详情。 */
@Data
@ApiModel("统一评价响应")
public class UnifiedEvaluationResDTO {
    private Long id;
    private Long ordersId;
    private Long consumerId;
    private Long serveProviderId;
    @ApiModelProperty("1好评，2差评")
    private Integer evaluationType;
    @ApiModelProperty("0隐藏，1展示")
    private Integer visibleStatus;
    private String content;
    private String[] pictureArray;
    private String ordersNo;
    private String serveItemName;
    private String serveItemImg;
    private String serveAddress;
    private LocalDateTime serveStartTime;
    private String consumerName;
    private String consumerPhone;
    private String serveProviderName;
    private Integer isAnonymous;
    private LocalDateTime createTime;
}
