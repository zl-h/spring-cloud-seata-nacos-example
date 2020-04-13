package com.yuhb.customer.service.impl;

import com.yuhb.common.domain.TbUser;
import com.yuhb.customer.mapper.TbUserMapper;
import com.yuhb.customer.service.TccService;
import io.seata.core.context.RootContext;
import io.seata.rm.tcc.api.BusinessActionContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestController
public class TccServiceImpl implements TccService {

    @Autowired
    TbUserMapper tbUserMapper;

    static final Map<String,Boolean> result = new ConcurrentHashMap<>();

    static final Map<String,Boolean> execute = new ConcurrentHashMap<>();

    static final Map<String,Boolean> buchang = new ConcurrentHashMap<>();

    /**
     * tcc服务t（try）方法
     * 实际业务方法
     *
     * @return String
     */
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public String add(Integer id,Integer age) {
        log.info("------------------> xid = " + RootContext.getXID());
        //实际的操作，或操作MQ、redis等
//        tccDAO.insert(params);
        //throw new RuntimeException("服务tcc测试回滚");
        TbUser tbUser = tbUserMapper.selectByPrimaryKey(id);
        if(tbUser == null || tbUser.getAge() == null){
            throw new RuntimeException("tbUser == null");
        }

        tbUser.setAge(age + tbUser.getAge());
        tbUser.setAddAge(age + tbUser.getAddAge());
        tbUserMapper.updateByPrimaryKeySelective(tbUser);
        result.put(RootContext.getXID(),true);
        execute.put(RootContext.getXID(),true);
        return "success";
    }

    /**
     * tcc服务 confirm方法
     * 可以空确认
     *
     * @param context 上下文
     * @return boolean
     */
    @Override
    public boolean commitTcc(BusinessActionContext context) {
        log.info("xid = " + context.getXid() + "提交成功");
        Integer id = Integer.valueOf(String.valueOf(context.getActionContext("id")));
        Integer age = Integer.valueOf(String.valueOf(context.getActionContext("age")));
        log.info("commitTcc id=" + id + "age=" + age);
        result.remove(context.getXid());
        return true;
    }

    /**
     * tcc 服务 cancel方法
     *
     * @param context 上下文
     * @return boolean
     */
    @Override
    @Transactional
    public boolean cancel(BusinessActionContext context) {
        // 存在并发，AT模式字段锁住了行
        //todo 这里写中间件、非关系型数据库的回滚操作
        log.info("please manually rollback this data:");
        Integer id = Integer.valueOf(String.valueOf(context.getActionContext("id")));
        Integer age = Integer.valueOf(String.valueOf(context.getActionContext("age")));
        log.info("id=" + id + "age=" + age);
        if(BooleanUtils.isTrue(result.get(context.getXid()))){
            log.info("加回去。事务补偿id=" + id + "age=" + age);
            if(!buchang.containsKey(context.getXid())){
                synchronized (buchang){
                    if(!buchang.containsKey(context.getXid())){
                        if(BooleanUtils.isTrue(result.get(context.getXid()))){
                            // 减回去。事务补偿
                            try {
                                log.info("减回去。事务补偿id=" + id + "age=" + age);
                                TbUser tbUser = tbUserMapper.selectByPrimaryKey(id);
                                if(tbUser == null || tbUser.getAge() == null){
                                    throw new RuntimeException("tbUser == null");
                                }
                                tbUser.setAge(tbUser.getAge() - age );
                                tbUser.setAddAge( tbUser.getAddAge() -age);
                                tbUserMapper.updateByPrimaryKeySelective(tbUser);
                                result.remove(context.getXid());
                                buchang.put(context.getXid(),true);
                                log.info("补偿成功,执行条数" + execute.size() + "已补偿条数"+  buchang.size());
                            }catch (Exception e){
                                log.error("补偿失败", e);
                                throw e;
                            }
                        }
                    }else {
                        log.error("重复补偿synchronized");
                    }
                }
            }else {
                log.error("重复补偿");
            }
        }
        return true;
    }


}