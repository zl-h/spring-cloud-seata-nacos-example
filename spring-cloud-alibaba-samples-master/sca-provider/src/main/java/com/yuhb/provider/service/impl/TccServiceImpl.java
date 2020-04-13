package com.yuhb.provider.service.impl;

import com.yuhb.common.domain.TbUser;

import com.yuhb.provider.mapper.TbUserMapper;
import com.yuhb.provider.service.TccService;
import io.seata.core.context.RootContext;
import io.seata.rm.tcc.api.BusinessActionContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class TccServiceImpl implements TccService {

    @Autowired
    TbUserMapper tbUserMapper;

    static final Map<String,Boolean> result = new ConcurrentHashMap<>();

    static final Map<String,Boolean> buchang = new ConcurrentHashMap<>();

    static final Map<String,Boolean> execute = new ConcurrentHashMap<>();

    /**
     * tcc服务t（try）方法
     * 实际业务方法
     *
     * @return String
     */
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public String sub(Integer id,Integer age){
        log.info("------------------> xid = " + RootContext.getXID());
        //实际的操作，或操作MQ、redis等
//        tccDAO.insert(params);
        //throw new RuntimeException("服务tcc测试回滚");

        TbUser tbUser = tbUserMapper.selectByPrimaryKey(id);
        if(tbUser == null || tbUser.getAge() == null){
            throw new RuntimeException("tbUser == null");
        }
        tbUser.setAge(tbUser.getAge() - age);
        tbUser.setSubAge(age + tbUser.getSubAge());
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
        //todo 这里写中间件、非关系型数据库的回滚操作
        System.out.println("please manually rollback this data:");
        Integer id = Integer.valueOf(String.valueOf(context.getActionContext("id")));
        Integer age = Integer.valueOf(String.valueOf(context.getActionContext("age")));
        log.info("id=" + id + "age=" + age);
        if(BooleanUtils.isTrue(result.get(context.getXid()))){
            log.info("加回去。事务补偿id=" + id + "age=" + age);
            if(!buchang.containsKey(context.getXid())){
                synchronized (buchang){
                    if(!buchang.containsKey(context.getXid())){
                            if (BooleanUtils.isTrue(result.get(context.getXid()))) {
                                try {
                                    // 加回去。事务补偿
                                    TbUser tbUser = tbUserMapper.selectByPrimaryKey(id);
                                    if(tbUser == null || tbUser.getAge() == null){
                                        throw new RuntimeException("tbUser == null");
                                    }
                                    tbUser.setAge(tbUser.getAge() + age );
                                    tbUser.setSubAge( tbUser.getSubAge() - age);
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