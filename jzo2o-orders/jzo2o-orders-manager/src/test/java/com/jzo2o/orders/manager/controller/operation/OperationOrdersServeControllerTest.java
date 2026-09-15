package com.jzo2o.orders.manager.controller.operation;

import com.jzo2o.common.model.PageResult;
import com.jzo2o.orders.manager.model.dto.request.OrdersServePageQueryByServeProviderReqDTO;
import com.jzo2o.orders.manager.model.dto.response.ServeProviderServeResDTO;
import com.jzo2o.orders.manager.service.IOrdersServeManagerService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OperationOrdersServeControllerTest {

    @Test
    void pageQueryByServeProvider_returnsTheFinishedOrdersForTheSelectedWorker() {
        IOrdersServeManagerService ordersServeManagerService = mock(IOrdersServeManagerService.class);
        OrdersServePageQueryByServeProviderReqDTO request = new OrdersServePageQueryByServeProviderReqDTO();
        request.setServeProviderId(1001L);
        ServeProviderServeResDTO order = new ServeProviderServeResDTO();
        order.setId(2001L);
        PageResult<ServeProviderServeResDTO> expected =
                new PageResult<>(1L, 1L, Collections.singletonList(order));
        when(ordersServeManagerService.pageQueryByServeProvider(same(request))).thenReturn(expected);

        OperationOrdersServeController controller = new OperationOrdersServeController();
        ReflectionTestUtils.setField(controller, "ordersServeManagerService", ordersServeManagerService);

        assertSame(expected, controller.pageQueryByServeProvider(request));
    }
}
