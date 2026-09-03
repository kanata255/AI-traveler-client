// 自动填充数据库字段

package com.example.demo.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

// java example
@Slf4j
// 往容器中注入对象的方式，把类创建的对象往spring boot容器中注入
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        log.info("开始插入填充...");
//        this.strictInsertFill(metaObject, "createUserId", Long.class, 123456L)
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        log.info("开始更新填充...");
//        this.strictInsertFill(metaObject, "updateUserId", Long.class, 123456L)
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}