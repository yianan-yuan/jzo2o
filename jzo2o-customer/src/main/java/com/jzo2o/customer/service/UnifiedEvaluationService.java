package com.jzo2o.customer.service;

import com.jzo2o.common.model.PageResult;
import com.jzo2o.customer.model.dto.request.EvaluationVisibilityReqDTO;
import com.jzo2o.customer.model.dto.request.UnifiedEvaluationPageReqDTO;
import com.jzo2o.customer.model.dto.request.UnifiedEvaluationSubmitReqDTO;
import com.jzo2o.customer.model.dto.response.BooleanResDTO;
import com.jzo2o.customer.model.dto.response.EvaluationRateResDTO;
import com.jzo2o.customer.model.dto.response.UnifiedEvaluationResDTO;

import java.util.List;
import java.util.Map;

/** 项目内统一评价服务，不依赖旧的外部评价系统。 */
public interface UnifiedEvaluationService {
    BooleanResDTO submit(Long consumerId, UnifiedEvaluationSubmitReqDTO req);
    PageResult<UnifiedEvaluationResDTO> pageForConsumer(Long consumerId, UnifiedEvaluationPageReqDTO req);
    PageResult<UnifiedEvaluationResDTO> pageForWorker(Long workerId, UnifiedEvaluationPageReqDTO req);
    PageResult<UnifiedEvaluationResDTO> pageForOperation(UnifiedEvaluationPageReqDTO req);
    UnifiedEvaluationResDTO detailForConsumer(Long id, Long consumerId);
    UnifiedEvaluationResDTO detailForWorker(Long id, Long workerId);
    UnifiedEvaluationResDTO detailForOperation(Long id);
    EvaluationRateResDTO rateForWorker(Long workerId);
    Map<Long, EvaluationRateResDTO> rateForWorkers(List<Long> workerIds);
    void updateVisibility(Long id, EvaluationVisibilityReqDTO req);
}
