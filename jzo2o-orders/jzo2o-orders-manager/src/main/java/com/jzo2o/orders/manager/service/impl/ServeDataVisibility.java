package com.jzo2o.orders.manager.service.impl;

import com.jzo2o.orders.base.enums.ServeStatusEnum;
import com.jzo2o.orders.base.model.domain.OrdersServe;

import java.util.Objects;

final class ServeDataVisibility {

    private ServeDataVisibility() {
    }

    static int finishedStatus() {
        return ServeStatusEnum.SERVE_FINISHED.getStatus();
    }

    static boolean isVisibleToWorker(Long workerId, OrdersServe serve) {
        return serve != null
                && Objects.equals(workerId, serve.getServeProviderId())
                && Objects.equals(finishedStatus(), serve.getServeStatus());
    }
}
