package com.jzo2o.customer.controller.operation;

import com.jzo2o.api.customer.dto.request.EvaluationSubmitReqDTO;
import com.jzo2o.common.model.CurrentUserInfo;
import com.jzo2o.customer.model.dto.request.AuditReqDTO;
import com.jzo2o.customer.model.dto.request.EvaluationPageByTargetReqDTO;
import com.jzo2o.customer.model.dto.request.LikeOrCancelReqDTO;
import com.jzo2o.customer.model.dto.response.EvaluationAndOrdersResDTO;
import com.jzo2o.customer.model.dto.response.EvaluationResDTO;
import com.jzo2o.customer.model.dto.response.EvaluationTokenDto;
import com.jzo2o.customer.model.dto.response.EvaluationRateResDTO;
import com.jzo2o.customer.service.EvaluationService;
import com.jzo2o.customer.service.UnifiedEvaluationService;
import com.jzo2o.common.model.PageResult;
import com.jzo2o.customer.model.dto.request.EvaluationVisibilityReqDTO;
import com.jzo2o.customer.model.dto.request.UnifiedEvaluationPageReqDTO;
import com.jzo2o.customer.model.dto.response.UnifiedEvaluationResDTO;
import com.jzo2o.common.utils.UserContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 评价相关接口
 *
 * @create 2023/9/11 16:14
 **/
@RestController("operationEvaluationController")
@RequestMapping("/operation/evaluation")
@Api(tags = "运营端 - 评价相关接口")
public class EvaluationController {
    @Resource
    private EvaluationService evaluationService;
    @Resource
    private UnifiedEvaluationService unifiedEvaluationService;

    @GetMapping("/page")
    @ApiOperation("运营端分页查询评价")
    public PageResult<UnifiedEvaluationResDTO> page(UnifiedEvaluationPageReqDTO req) {
        return unifiedEvaluationService.pageForOperation(req);
    }

    @GetMapping("/{id}")
    @ApiOperation("运营端查看评价详情")
    public UnifiedEvaluationResDTO detail(@PathVariable("id") Long id) {
        return unifiedEvaluationService.detailForOperation(id);
    }

    @PutMapping("/{id}/visibility")
    @ApiOperation("隐藏或恢复评价")
    public void updateVisibility(@PathVariable("id") Long id,
                                 @RequestBody EvaluationVisibilityReqDTO req) {
        unifiedEvaluationService.updateVisibility(id, req);
    }

    @GetMapping("/summary")
    @ApiOperation("查询服务人员好评率")
    public EvaluationRateResDTO summary(@RequestParam("serveProviderId") Long serveProviderId) {
        return unifiedEvaluationService.rateForWorker(serveProviderId);
    }


    @GetMapping("/token")
    @ApiOperation("获取评价系统token")
    public EvaluationTokenDto getToken() {
        return evaluationService.getEvaluationInfo();
    }
}
