package com.warehouse.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.warehouse.security.SecurityUtils;
import org.apache.ibatis.reflection.MetaObject;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
@MapperScan("com.warehouse.mapper")
public class MybatisPlusConfig {

    @Bean
    public MetaObjectHandler auditMetaObjectHandler() {
        return new AuditMetaObjectHandler();
    }

    static class AuditMetaObjectHandler implements MetaObjectHandler {

        @Override
        public void insertFill(MetaObject metaObject) {
            LocalDateTime now = LocalDateTime.now();
            this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
            Long userId = SecurityUtils.getCurrentUserIdSafely();
            if (userId != null) {
                this.strictInsertFill(metaObject, "createBy", Long.class, userId);
            }
        }

        @Override
        public void updateFill(MetaObject metaObject) {
            LocalDateTime now = LocalDateTime.now();
            this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, now);
            Long userId = SecurityUtils.getCurrentUserIdSafely();
            if (userId != null) {
                this.strictUpdateFill(metaObject, "updateBy", Long.class, userId);
            }
        }
    }
}
