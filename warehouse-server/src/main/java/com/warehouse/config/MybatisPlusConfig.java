package com.warehouse.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.warehouse.security.SecurityUtils;
import org.apache.ibatis.reflection.MetaObject;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
@MapperScan("com.warehouse.mapper")
public class MybatisPlusConfig {

    /**
     * P0 修复:注册 MybatisPlusInterceptor 并加入 PaginationInnerInterceptor
     *
     * 用户报告:"新增用户功能无法正常使用,提示添加成功,但是没有正常展示添加成功后的数据"
     *
     * 根因(已通过 HTTP 验证):
     *   GET /api/v1/users?page=1&size=20 → total=0, records=[7 条]
     *   总数永远是 0,前端 el-pagination 显示"共 0 条",用户感觉"数据没显示"
     *
     * 根因:MybatisPlusConfig 之前没有注册 MybatisPlusInterceptor bean,
     *      所以 MyBatis-Plus 的分页插件从未生效,
     *      selectPage(...) 不会执行 COUNT(*) 查询,IPage.total 永远是 0
     *
     * 修复:加 @Bean MybatisPlusInterceptor,addInnerInterceptor PaginationInnerInterceptor(DbType.MYSQL)
     *      (PaginationInnerInterceptor 在 3.5.9 由 mybatis-plus-jsqlparser 模块提供,
     *       pom.xml 同步加该依赖,见 pom.xml 注释)
     *
     * 修复时间:2026-06-19  验证:MybatisPlusPaginationInterceptorTest + HTTP GET /api/v1/users
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 1. 分页拦截器:执行 selectPage 时自动 COUNT(*) + LIMIT
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        // 2. 乐观锁拦截器:配合 @Version 字段使用
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }

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
