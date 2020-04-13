package com.yuhb.customer.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Created by yu.hb on 2019-10-30
 */
@FeignClient(value = "sca-provider")
public interface ProviderFeignService {

    @GetMapping("/feign/echo")
    String feignEcho(@RequestParam("name") String name);

    @GetMapping("/feign/user/add")
    String add(@RequestParam("name") String name) ;

    @GetMapping("/feign/subAge/{id}/{age}")
    String subAge(@PathVariable Integer id, @PathVariable Integer age) ;


    @GetMapping("/feign/tcc/{id}/{age}")
    String tcc(@PathVariable Integer id, @PathVariable Integer age) ;
}
