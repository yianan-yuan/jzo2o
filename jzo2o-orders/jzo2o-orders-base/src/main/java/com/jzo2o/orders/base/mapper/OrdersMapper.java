package com.jzo2o.orders.base.mapper;

import com.jzo2o.orders.base.model.domain.Orders;
import com.jzo2o.orders.base.model.dto.OrderUpdateStatusDTO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
* 订单表 Mapper 接口
*
 */
public interface OrdersMapper extends BaseMapper<Orders> {


}
