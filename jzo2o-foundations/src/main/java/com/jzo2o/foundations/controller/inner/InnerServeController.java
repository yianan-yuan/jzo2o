package com.jzo2o.foundations.controller.inner;

import com.jzo2o.api.foundations.ServeApi;
import com.jzo2o.api.foundations.dto.response.ServeAggregationResDTO;
import com.jzo2o.foundations.service.IServeService;
import com.jzo2o.foundations.service.ServeAggregationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author itcast
 */
@RestController
@RequestMapping("/inner/serve")
@Api(tags = "内部接口 - 服务相关接口")
public class InnerServeController implements ServeApi {
    @Resource
    private IServeService serveService;
    @Resource
    private ServeAggregationService serveAggregationService;

    @Override
    @GetMapping("/{id}")
    @ApiOperation("根据id查询服务")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "服务项id", required = true, dataTypeClass = Long.class)
    })
    public ServeAggregationResDTO findById(@NotNull(message = "id不能为空") @PathVariable("id") Long id) {
        return serveService.findServeDetailById(id);
    }

    @Override
    @GetMapping("/search")
    public List<ServeAggregationResDTO> searchActiveServes(@RequestParam("cityCode") String cityCode,
                                                            @RequestParam("keyword") String keyword,
                                                            @RequestParam(value = "limit", required = false) Integer limit) {
        int safeLimit = Math.max(1, Math.min(limit == null ? 20 : limit, 20));
        return serveAggregationService.findServeList(cityCode, null, keyword).stream()
                .map(item -> serveService.findServeDetailById(item.getId()))
                .filter(item -> item != null
                        && Integer.valueOf(2).equals(item.getSaleStatus())
                        && cityCode.equals(item.getCityCode()))
                .limit(safeLimit)
                .collect(Collectors.toList());
    }
}
