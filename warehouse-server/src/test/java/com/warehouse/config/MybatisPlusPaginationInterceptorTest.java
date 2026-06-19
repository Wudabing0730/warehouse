/**
 * Bug 复现验证:MybatisPlusConfig 必须注册 PaginationInnerInterceptor,否则所有 selectPage 查询
 * 的 IPage.total 永远是 0,前端分页"共 0 条",用户感知是"添加了用户但没显示"。
 *
 * 用户报告:"新增用户功能无法正常使用,提示添加成功,但是没有正常展示添加成功后的数据"
 *
 * 根因:
 *   1. POST /api/v1/users 成功(数据已写入 DB)
 *   2. GET /api/v1/users 返回 records=[7 条] 但 total=0
 *   3. 前端 Element Plus el-pagination 读 total=0,显示"共 0 条"
 *   4. 用户认为"没有展示添加成功后的数据",其实是分页 total 错了
 *
 * 后端日志/HTTP 验证:
 *   GET /api/v1/users?page=1&size=20 → total=0, records=[7 条]
 *
 * 修复:MybatisPlusConfig 注入 MybatisPlusInterceptor 并 add PaginationInnerInterceptor
 *      + pom.xml 加 mybatis-plus-jsqlparser 依赖(3.5.9 把 PaginationInnerInterceptor 移过去了)
 *
 * 验证策略:用反射遍历 MybatisPlusInterceptor.interceptors,断言某个拦截器的类名包含 "Pagination"
 *          (避免直接 import,允许在 3.5.9 没装 jsqlparser 时也能编译)
 */
package com.warehouse.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class MybatisPlusPaginationInterceptorTest {

    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    /**
     * 通过反射拿 MybatisPlusInterceptor 内部的 interceptors 列表(不 import 它的 inner 包)
     */
    @SuppressWarnings("unchecked")
    private List<Object> getInnerInterceptors(MybatisPlusInterceptor mpi) throws Exception {
        Field f = MybatisPlusInterceptor.class.getDeclaredField("interceptors");
        f.setAccessible(true);
        return (List<Object>) f.get(mpi);
    }

    @Test
    @DisplayName("BUG 复现:SqlSessionFactory 的拦截器链中必须包含 PaginationInnerInterceptor,否则 selectPage 的 total 永远是 0")
    void paginationInnerInterceptorMustBeRegistered() throws Exception {
        assertNotNull(sqlSessionFactory, "SqlSessionFactory 必须存在");

        org.apache.ibatis.session.Configuration configuration = sqlSessionFactory.getConfiguration();
        List<org.apache.ibatis.plugin.Interceptor> interceptors = configuration.getInterceptors();

        MybatisPlusInterceptor mybatisPlusInterceptor = null;
        for (org.apache.ibatis.plugin.Interceptor interceptor : interceptors) {
            if (interceptor instanceof MybatisPlusInterceptor) {
                mybatisPlusInterceptor = (MybatisPlusInterceptor) interceptor;
                break;
            }
        }
        assertNotNull(mybatisPlusInterceptor,
                "BUG 复现:SqlSessionFactory 拦截器链中缺少 MybatisPlusInterceptor。"
                        + "MyBatis-Plus 的所有 inner 拦截器(分页/乐观锁/防全表更新)都依赖它。");

        List<Object> innerInterceptors = getInnerInterceptors(mybatisPlusInterceptor);
        assertNotNull(innerInterceptors, "MybatisPlusInterceptor.interceptors 不能为 null");
        assertFalse(innerInterceptors.isEmpty(),
                "MybatisPlusInterceptor.interceptors 不能为空,至少要包含 PaginationInnerInterceptor");

        // 关键断言:用类名匹配,避免硬 import 3.5.9 不存在的类
        boolean hasPagination = innerInterceptors.stream()
                .anyMatch(i -> i.getClass().getName().contains("Pagination"));
        assertTrue(hasPagination,
                "BUG 复现:MybatisPlusInterceptor 缺少 PaginationInnerInterceptor!"
                        + " 后果是 selectPage(...) 不会执行 count 查询,IPage.total 永远是 0,"
                        + " 前端 el-pagination 显示「共 0 条」,用户感觉「添加的数据没显示」。"
                        + " 实际注册的 innerInterceptors: "
                        + innerInterceptors.stream().map(o -> o.getClass().getName()).reduce("", (a, b) -> a + "\n  - " + b));
    }
}
