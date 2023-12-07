package com.insnail.activity.persistence.intercepator;

import com.insnail.activity.component.UserContext;
import com.insnail.activity.persistence.model.BaseEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.javassist.bytecode.analysis.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.defaults.DefaultSqlSession;

import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;


@Slf4j
//@Component("ibatisAuditDataInterceptor")
@Intercepts({@Signature(method = "update", type = Executor.class, args = {MappedStatement.class, Object.class})})
public class IbatisAuditDataInterceptor implements Interceptor {
    private UserContext userContext;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 从上下文中获取用户id
        String userId = userContext.get();

        Object[] args = invocation.getArgs();
        SqlCommandType sqlCommandType = null;

        for (Object object : args) {
            // 从MappedStatement参数中获取到操作类型
            if (object instanceof MappedStatement) {
                MappedStatement ms = (MappedStatement) object;
                sqlCommandType = ms.getSqlCommandType();
                log.debug("操作类型： {}", sqlCommandType);
                continue;
            }
            // 判断参数是否是BaseEntity类型
            // 一个参数
            if (object instanceof BaseEntity) {
                if (SqlCommandType.INSERT == sqlCommandType) {
                    BaseEntity object1 = (BaseEntity) object;
                    object1.setCreator(userId);
                    object1.setCreateTime(new Date());
                    continue;
                }
                if (SqlCommandType.UPDATE == sqlCommandType) {
                    BaseEntity object1 = (BaseEntity) object;
                    object1.setUpdator(userId);
                    object1.setUpdateTime(new Date());
                    continue;
                }
            }
            // 兼容MyBatis的updateByExampleSelective(record, example);
            if (object instanceof MapperMethod.ParamMap) {
                log.debug("mybatis arg: {}", object);
                @SuppressWarnings("unchecked")
                MapperMethod.ParamMap<Object> parasMap = (MapperMethod.ParamMap<Object>) object;
                String key = "record";
                if (!parasMap.containsKey(key)) {
                    continue;
                }
                Object paraObject = parasMap.get(key);
                if (paraObject instanceof BaseEntity) {
                    if (SqlCommandType.UPDATE == sqlCommandType) {
                        BaseEntity object1 = (BaseEntity) object;
                        object1.setUpdator(userId);
                        object1.setUpdateTime(new Date());
                        continue;
                    }
                }
            }
            // 兼容批量插入
            if (object instanceof DefaultSqlSession.StrictMap) {
                log.debug("mybatis arg: {}", object);
                @SuppressWarnings("unchecked")
                DefaultSqlSession.StrictMap<ArrayList<Object>> map = (DefaultSqlSession.StrictMap<ArrayList<Object>>) object;
                String key = "collection";
                if (!map.containsKey(key)) {
                    continue;
                }
                ArrayList<Object> objs = map.get(key);
                for (Object obj : objs) {
                    if (obj instanceof BaseEntity) {
                        if (SqlCommandType.INSERT == sqlCommandType) {
                            BaseEntity object1 = (BaseEntity) object;
                            object1.setCreator(userId);
                            object1.setCreateTime(new Date());
                        }
                        if (SqlCommandType.UPDATE == sqlCommandType) {
                            BaseEntity object1 = (BaseEntity) object;
                            object1.setUpdator(userId);
                            object1.setUpdateTime(new Date());
                        }
                    }
                }
            }
        }
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }
}
