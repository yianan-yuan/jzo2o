package com.jzo2o.orders.manager.controller.operation;

import com.jzo2o.common.model.PageResult;
import com.jzo2o.orders.manager.model.dto.request.OrdersServePageQueryByServeProviderReqDTO;
import com.jzo2o.orders.manager.model.dto.response.*;
import com.jzo2o.orders.manager.service.IOrdersServeManagerService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 */
@RestController("orders-operation")
@Api(tags = "运营端-服务单相关接口")
@RequestMapping("/operation/ordersServe")
public class OperationOrdersServeController {
    @Resource
    private IOrdersServeManagerService ordersServeManagerService;

    @GetMapping("/pageQueryByServeProvider")
    @ApiOperation("按服务人员查询已完成服务记录")
    public PageResult<ServeProviderServeResDTO> pageQueryByServeProvider(
            OrdersServePageQueryByServeProviderReqDTO request) {
        return ordersServeManagerService.pageQueryByServeProvider(request);
    }

}
