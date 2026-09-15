package com.jzo2o.orders.manager.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jzo2o.api.foundations.ServeItemApi;
import com.jzo2o.api.foundations.dto.response.ServeItemSimpleResDTO;
import com.jzo2o.common.model.PageResult;
import com.jzo2o.orders.base.enums.ServeStatusEnum;
import com.jzo2o.orders.base.mapper.OrdersServeMapper;
import com.jzo2o.orders.base.model.domain.OrdersServe;
import com.jzo2o.orders.manager.model.dto.request.OrdersServePageQueryByServeProviderReqDTO;
import com.jzo2o.orders.manager.model.dto.response.ServeProviderServeResDTO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrdersServeManagerServiceImplTest {

    @Test
    void returnsCompletedServiceRecordsWithoutCallingTheLegacyScoreService() {
        OrdersServeManagerServiceImpl service = new OrdersServeManagerServiceImpl();
        OrdersServeMapper mapper = mock(OrdersServeMapper.class);
        ServeItemApi serveItemApi = mock(ServeItemApi.class);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        ReflectionTestUtils.setField(service, "serveItemApi", serveItemApi);

        OrdersServe serve = new OrdersServe();
        serve.setId(2608010000000000241L);
        serve.setServeProviderId(1696338624494202882L);
        serve.setServeItemId(1685894105234755585L);
        serve.setServeStatus(ServeStatusEnum.SERVE_FINISHED.getStatus());
        serve.setRealServeEndTime(LocalDateTime.of(2026, 8, 1, 22, 1, 32));
        Page<OrdersServe> completedServices = new Page<>(1, 10);
        completedServices.setRecords(List.of(serve));
        completedServices.setTotal(1);
        when(mapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(completedServices);

        ServeItemSimpleResDTO serveItem = new ServeItemSimpleResDTO();
        serveItem.setId(1685894105234755585L);
        serveItem.setName("日常保洁");
        when(serveItemApi.listByIds(List.of(1685894105234755585L))).thenReturn(List.of(serveItem));
        OrdersServePageQueryByServeProviderReqDTO request = new OrdersServePageQueryByServeProviderReqDTO();
        request.setServeProviderId(1696338624494202882L);
        request.setPageNo(1L);
        request.setPageSize(10L);
        PageResult<ServeProviderServeResDTO> result = service.pageQueryByServeProvider(request);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getList().size());
        assertNotNull(result.getList().get(0));
        assertEquals("日常保洁", result.getList().get(0).getServeItemName());
    }
}
