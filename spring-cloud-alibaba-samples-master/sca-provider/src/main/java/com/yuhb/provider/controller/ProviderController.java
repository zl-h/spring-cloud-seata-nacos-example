package com.yuhb.provider.controller;

import com.yuhb.common.domain.TbUser;
import com.yuhb.provider.mapper.TbUserMapper;
import com.yuhb.provider.service.TccService;
import io.seata.core.context.RootContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by yu.hb on 2019-10-30
 */
@RestController
@RefreshScope
@Slf4j
public class ProviderController {

    @Autowired
    private TbUserMapper tbUserMapper;

    @Autowired
    TccService tccService;

    @Value("${useLocalCache:false}")
    private boolean useLocalCache;

    @RequestMapping("/get")
    public boolean get() {
        return useLocalCache;
    }

    @GetMapping("/feign/echo")
    public String feignEcho(String name) {
        return "feignEcho() hi " + name;
    }

    @GetMapping("/feign/user/add")
    public String add(String name) {
        tbUserMapper.insert(new TbUser(name,2));
        return "success";
    }

    /**
     * 幂等性设计
     */
    static final Map<String,Boolean> executeMap = new ConcurrentHashMap<>();

    @GetMapping("/feign/subAge/{id}/{age}")
    @Transactional
    public String subAge(@PathVariable Integer id, @PathVariable Integer age) {
        TbUser tbUser = tbUserMapper.selectByPrimaryKey(id);
        if(tbUser == null || tbUser.getAge() == null){
            throw new RuntimeException("tbUser == null");
        }
        if(executeMap.containsKey( RootContext.getXID())){
            log.info("subAge重复请求",RootContext.getXID(),id,age);
            return "success";
        }else {
            executeMap.put( RootContext.getXID(), true);
        }
        tbUser.setAge(tbUser.getAge() - age);
        tbUser.setSubAge(age + tbUser.getSubAge());
        tbUserMapper.updateByPrimaryKeySelective(tbUser);
        return "success";
    }

    @GetMapping("/feign/tcc/{id}/{age}")
    public String tcc(@PathVariable Integer id, @PathVariable Integer age) {
        if(executeMap.containsKey( RootContext.getXID())){
            log.info("tcc重复请求",RootContext.getXID(),id,age);
            return "success";
        }else {
            executeMap.put(RootContext.getXID(), true);
        }
        tccService.sub(id,age);
        return "success";
    }
}
