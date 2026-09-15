package com.jzo2o.api.customer;

import com.jzo2o.api.customer.dto.response.AddressBookResDTO;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * contextId 指定FeignClient实例的上下文id，如果不设置默认为类名，value指定微服务的名称，path:指定接口地址
 */
@FeignClient(contextId = "jzo2o-customer",value = "jzo2o-customer",path = "/customer/inner/address-book")
public interface AddressBookApi {

    /**
     * 根据地址簿ID获取地址详情信息
     * @return
     */
    @GetMapping("/{id}")
    AddressBookResDTO detail(@PathVariable("id") Long id);

    @GetMapping("/getByUserIdAndCity")
    @ApiOperation("根据用户id和城市获取用户地址列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "userId", value = "用户id", required = true, dataTypeClass = Long.class),
            @ApiImplicitParam(name = "city", value = "城市名称", required = true, dataTypeClass = String.class)

    })
    List<AddressBookResDTO> getByUserIdAndCity(@RequestParam("userId") Long userId, @RequestParam("city") String city);
}