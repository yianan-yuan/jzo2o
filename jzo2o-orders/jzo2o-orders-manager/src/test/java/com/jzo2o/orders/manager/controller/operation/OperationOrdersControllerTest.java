package com.jzo2o.orders.manager.controller.operation;

import com.jzo2o.api.orders.dto.request.OrderCancelReqDTO;
import com.jzo2o.common.model.CurrentUserInfo;
import com.jzo2o.common.model.PageResult;
import com.jzo2o.common.utils.UserContext;
import com.jzo2o.orders.base.model.domain.Orders;
import com.jzo2o.orders.manager.model.dto.OrderCancelDTO;
import com.jzo2o.orders.manager.model.dto.request.OrderPageQueryReqDTO;
import com.jzo2o.orders.manager.model.dto.response.OperationOrdersDetailResDTO;
import com.jzo2o.orders.manager.service.IOrdersManagerService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperationOrdersControllerTest {

    @Mock
    private IOrdersManagerService ordersManagerService;

    @InjectMocks
    private OperationOrdersController controller;

    @AfterEach
    void clearUserContext() {
        UserContext.clear();
    }

    @Test
    void pageDelegatesTheFiltersAndReturnsThePagedOrderList() {
        OrderPageQueryReqDTO request = new OrderPageQueryReqDTO();
        request.setPageNo(1L);
        request.setPageSize(10L);
        request.setContactsPhone("13800138000");
        PageResult<Orders> expected = new PageResult<>(1L, 1L,
                Collections.singletonList(new Orders().setId(1L)));

        when(ordersManagerService.operationPageQuery(request)).thenReturn(expected);

        assertSame(expected, controller.page(request));
        verify(ordersManagerService).operationPageQuery(request);
    }

    @Test
    void aggregationDelegatesAndReturnsTheOperationDetail() {
        OperationOrdersDetailResDTO expected = new OperationOrdersDetailResDTO();
        when(ordersManagerService.operationDetail(1L)).thenReturn(expected);

        assertSame(expected, controller.aggregation(1L));
        verify(ordersManagerService).operationDetail(1L);
    }

    @Test
    void cancelUsesTheLoggedInAdministratorAndDelegatesToTheExistingCancelService() {
        UserContext.set(new CurrentUserInfo(9L, "管理员", null, 4));
        OrderCancelReqDTO request = new OrderCancelReqDTO();
        request.setId(1L);
        request.setCancelReason("用户申请退款");

        controller.cancel(request);

        ArgumentCaptor<OrderCancelDTO> captor = ArgumentCaptor.forClass(OrderCancelDTO.class);
        verify(ordersManagerService).cancel(captor.capture());
        assertEquals(1L, captor.getValue().getId());
        assertEquals("用户申请退款", captor.getValue().getCancelReason());
        assertEquals(9L, captor.getValue().getCurrentUserId());
        assertEquals(4, captor.getValue().getCurrentUserType());
    }
}
