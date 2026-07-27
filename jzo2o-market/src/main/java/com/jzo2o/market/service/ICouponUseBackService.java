package com.jzo2o.market.service;

import com.jzo2o.market.model.domain.CouponUseBack;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDateTime;

/**
* 优惠券使用回退记录 服务类
*
 */
public interface ICouponUseBackService extends IService<CouponUseBack> {

    void add(Long couponId, Long userId, LocalDateTime writeOffTime);
}
