package com.jzo2o.orders.manager.service.impl;

import com.jzo2o.orders.base.enums.ServeStatusEnum;
import com.jzo2o.orders.base.model.domain.OrdersServe;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServeDataVisibilityTest {

    @Test
    void includesServiceThatJustFinishedForTheRequestedWorker() {
        OrdersServe serve = new OrdersServe();
        serve.setServeProviderId(1001L);
        serve.setServeStatus(ServeStatusEnum.SERVE_FINISHED.getStatus());
        serve.setRealServeEndTime(LocalDateTime.now().minusMinutes(2));

        assertTrue(ServeDataVisibility.isVisibleToWorker(1001L, serve));
    }

    @Test
    void excludesOtherWorkersAndUnfinishedServices() {
        OrdersServe serve = new OrdersServe();
        serve.setServeProviderId(1002L);
        serve.setServeStatus(ServeStatusEnum.NO_SERVED.getStatus());

        assertFalse(ServeDataVisibility.isVisibleToWorker(1001L, serve));
    }
}
