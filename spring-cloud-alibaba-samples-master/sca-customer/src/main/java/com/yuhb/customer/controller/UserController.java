package com.yuhb.customer.controller;

import com.yuhb.common.domain.TbUser;
import com.yuhb.customer.feign.ProviderFeignService;
import com.yuhb.customer.mapper.TbUserMapper;
import com.yuhb.customer.service.TccService;
import io.seata.core.context.RootContext;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Map;

/**
 * Description:数据库新建表
 *
CREATE TABLE `tb_user` (
 `id` int(11) NOT NULL AUTO_INCREMENT,
 `name` varchar(25) NOT NULL,
 `age` int(3) DEFAULT NULL,
 PRIMARY KEY (`id`)
 ) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8

 * author: yu.hb
 * Date: 2019-11-01
 */
@RestController
@Slf4j
public class UserController {

    @Autowired
    private TbUserMapper userMapper;

    @Autowired
    private ProviderFeignService providerFeignService;

    @Autowired
    TccService tccService;


    /**
     * seata 全局事务控制
     * @param
     */
    @RequestMapping("/seata/user/add")
    @GlobalTransactional(rollbackFor = Exception.class) // 开启全局事务
    public void add() {
        log.info("globalTransactional begin, Xid:{}", RootContext.getXID());
        // local save
        TbUser user = new TbUser(new Date().toString(), 1);
        localSave(user);

        // call provider save
        providerFeignService.add(user.getName());


        // test seata globalTransactional
        throw new RuntimeException("rollback test");
    }

    @RequestMapping("/seata/user/add1")
    @GlobalTransactional(rollbackFor = Exception.class) // 开启全局事务
    @ResponseBody
    public String add1() {
        log.info("globalTransactional begin, Xid:{}", RootContext.getXID());
        // local save
        TbUser user = new TbUser(new Date().toString(), 0);
        localSave(user);

        // call provider save
        providerFeignService.add(user.getName());
        return "全局事务执行成功";
    }

    @RequestMapping("/seata/user/addAge/{id}/{age}")
    @GlobalTransactional(rollbackFor = Exception.class) // 开启全局事务
    @ResponseBody
    public String addAge(@PathVariable Integer id,@PathVariable Integer age) {
        log.info("globalTransactional begin, Xid:{}", RootContext.getXID());
        TbUser tbUser = userMapper.selectByPrimaryKey(id);
        if(tbUser == null || tbUser.getAge() == null){
            throw new RuntimeException("tbUser == null");
        }


        tbUser.setAge(age + tbUser.getAge());
        tbUser.setAddAge(age + tbUser.getAddAge());
        userMapper.updateByPrimaryKeySelective(tbUser);

        // call provider save
        providerFeignService.subAge(id, age);
        if(age == 100){
            throw new RuntimeException("age = 100 is exception condition");
        }
        return "全局事务执行成功";
    }

    @GetMapping("/seata/tcc/{id}/{age}")
    @GlobalTransactional(rollbackFor = Exception.class) // 开启全局事务
    @ResponseBody
    public String tcc(@PathVariable Integer id, @PathVariable Integer age) {
        tccService.add(id,age);
        providerFeignService.tcc(id, age);
        if(age == 100){
            throw new RuntimeException("age = 100 is exception condition");
        }
        return "success";
    }

    /**
     * 本地事务
     */
    @Transactional
    @RequestMapping("/local/save")
    public void localSave() {
        localSave(new TbUser("localTest", 1));
        throw new RuntimeException("rollback test");
    }

    private void localSave(TbUser user) {
        user.setName("customer");
        userMapper.insert(user);
//        userMapper.testUpdateForExists();
//        userMapper.updateTest();
//        userMapper.batchInsert();

//        TbUser u1 = new TbUser();
//        u1.setId(1);
//        u1.setName("test");
//        u1.setAge(100);
//
//        TbUser u2 = new TbUser();
//        u2.setId(1);
//        u2.setName("test");
//        u2.setAge(100);
//
//        List<TbUser> users = Arrays.asList(u1,u2);
//        userMapper.batchUpdate(users);
    }
}
