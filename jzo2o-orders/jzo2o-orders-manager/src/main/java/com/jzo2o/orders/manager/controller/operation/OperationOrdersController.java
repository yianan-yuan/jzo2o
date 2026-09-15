package com.jzo2o.orders.manager.controller.operation;

import cn.hutool.core.bean.BeanUtil;
import com.jzo2o.api.orders.dto.request.OrderCancelReqDTO;
import com.jzo2o.api.orders.dto.response.OrderSimpleResDTO;
import com.jzo2o.common.model.CurrentUserInfo;
import com.jzo2o.common.model.PageResult;
import com.jzo2o.common.utils.UserContext;
import com.jzo2o.orders.manager.model.dto.request.OrderPageQueryReqDTO;
import com.jzo2o.orders.manager.model.dto.OrderCancelDTO;
import com.jzo2o.orders.manager.model.dto.response.OperationOrdersDetailResDTO;
import com.jzo2o.orders.manager.service.IOrdersManagerService;
import com.jzo2o.orders.base.model.domain.Orders;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 */
@RestController("operationOrdersController")
@Api(tags = "运营端-订单相关接口")
@RequestMapping("/operation/orders")
public class OperationOrdersController {

    @Resource
    private IOrdersManagerService ordersManagerService;

    @GetMapping("/page")
    @ApiOperation("运营端分页查询当前订单")
    public PageResult<Orders> page(OrderPageQueryReqDTO orderPageQueryReqDTO) {
        return ordersManagerService.operationPageQuery(orderPageQueryReqDTO);
    }

    @GetMapping("/aggregation/{id}")
    @ApiOperation("运营端查询订单详情")
    public OperationOrdersDetailResDTO aggregation(@PathVariable("id") Long id) {
        return ordersManagerService.operationDetail(id);
    }

    @PutMapping("/cancel")
    @ApiOperation("运营端取消订单并发起退款")
    public void cancel(@RequestBody OrderCancelReqDTO orderCancelReqDTO) {
        OrderCancelDTO orderCancelDTO = BeanUtil.toBean(orderCancelReqDTO, OrderCancelDTO.class);
        CurrentUserInfo currentUserInfo = UserContext.currentUser();
        orderCancelDTO.setCurrentUserId(currentUserInfo.getId());
        orderCancelDTO.setCurrentUserName(currentUserInfo.getName());
        orderCancelDTO.setCurrentUserType(currentUserInfo.getUserType());
        ordersManagerService.cancel(orderCancelDTO);
    }

}
