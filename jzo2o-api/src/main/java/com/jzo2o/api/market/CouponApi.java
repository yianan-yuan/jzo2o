package com.jzo2o.api.market;

import com.jzo2o.api.market.dto.request.CouponUseBackReqDTO;
import com.jzo2o.api.market.dto.request.CouponUseReqDTO;
import com.jzo2o.api.market.dto.response.AvailableCouponsResDTO;
import com.jzo2o.api.market.dto.response.CouponUseResDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;

/**
 * 内部接口 - 优惠券相关接口
 *
 * @author Mr.M
 */
@FeignClient(contextId = "jzo2o-market", value = "jzo2o-market", path = "/market/inner/coupon")
public interface CouponApi {

    /**
     * 获取可用优惠券列表
     *
     * @param userId 用户ID
     * @param totalAmount 总金额，单位分
     * @return 可用优惠券列表
     */
    @GetMapping("/getAvailable")
    List<AvailableCouponsResDTO> getAvailable(@RequestParam("userId") Long userId,
                                              @RequestParam("totalAmount") BigDecimal totalAmount);

    /**
     * 使用优惠券，并返回优惠金额
     *
     * @param couponUseReqDTO 优惠券使用请求参数
     * @return 优惠券使用结果
     */
    @PostMapping("/use")
    CouponUseResDTO use(@RequestBody CouponUseReqDTO couponUseReqDTO);

    /**
     * 优惠券退回接口
     *
     * @param couponUseBackReqDTO 优惠券退回请求参数
     */
    @PostMapping("/useBack")
    void useBack(@RequestBody CouponUseBackReqDTO couponUseBackReqDTO);
}

