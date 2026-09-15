package com.jzo2o.customer.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("evaluation_record")
public class EvaluationRecord {
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    private Long ordersId;
    private Long consumerId;
    private Long serveProviderId;
    private Integer evaluationType;
    private Integer visibleStatus;
    private String content;
    private String pictureArray;
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
    private LocalDateTime updateTime;
}
