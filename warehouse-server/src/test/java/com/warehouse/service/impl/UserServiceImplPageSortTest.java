/**
 * Bug 复现验证:UserServiceImpl.page 必须支持前端传来的 orderBy/order 参数,而不是写死 orderByDesc(createTime)
 *
 * 用户报告:"用户管理中的id显示排序有问题,不能做到升序和降序切换"
 *
 * 根因(代码 review 定位):
 *   - UserServiceImpl.page() 第 56 行 wrapper.orderByDesc(User::getCreateTime) 写死
 *   - UserController.list() 第 31 行只接收 page/size,没有 orderBy/order 参数
 *   - UserQueryDTO 没有 orderBy/order 字段
 *   - 前端 UserView.vue 的 <el-table> 没有 sortable 属性和 @sort-change 处理器
 *
 * 后果:无论前端怎么点 ID 列头,接口永远按 createTime DESC 返回,无法切换
 *
 * 修复策略:
 *   1. UserQueryDTO 加 orderBy (String) + order (String, "asc"/"desc") 字段
 *   2. UserServiceImpl.page 用 orderBy/order 构建 wrapper,白名单防 SQL 注入
 *   3. UserController.list 把 orderBy/order 传给 UserQueryDTO
 *   4. 前端 UserView.vue 给 id/username/createTime 列加 sortable + @sort-change
 *
 * 验证策略:插入 3 个用户,通过不同 orderBy/order 组合调用 page(),断言顺序符合预期
 */
package com.warehouse.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.warehouse.dto.request.UserQueryDTO;
import com.warehouse.dto.response.UserVO;
import com.warehouse.entity.User;
import com.warehouse.mapper.UserMapper;
import com.warehouse.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("user-sort-test")
class UserServiceImplPageSortTest {

    @Autowired private UserService userService;
    @Autowired private UserMapper userMapper;
    @Autowired private DataSource dataSource;

    @BeforeEach
    void setUpSchema() throws Exception {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            // 清理 + 建表(只覆盖 page() 涉及的表)
            stmt.execute("DROP TABLE IF EXISTS t_user_role");
            stmt.execute("DROP TABLE IF EXISTS t_role");
            stmt.execute("DROP TABLE IF EXISTS t_user");
            stmt.execute("CREATE TABLE t_user (" +
                    "  user_id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "  username VARCHAR(50) NOT NULL," +
                    "  password VARCHAR(255) NOT NULL," +
                    "  real_name VARCHAR(50)," +
                    "  phone VARCHAR(20)," +
                    "  email VARCHAR(100)," +
                    "  status TINYINT NOT NULL DEFAULT 1," +
                    "  create_by BIGINT," +
                    "  update_by BIGINT," +
                    "  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                    "  update_time DATETIME" +
                    ")");
            stmt.execute("CREATE TABLE t_role (" +
                    "  role_id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "  role_name VARCHAR(50) NOT NULL," +
                    "  role_desc VARCHAR(200)," +
                    "  status TINYINT," +
                    "  create_by BIGINT," +
                    "  update_by BIGINT," +
                    "  create_time DATETIME," +
                    "  update_time DATETIME" +
                    ")");
            stmt.execute("CREATE TABLE t_user_role (" +
                    "  id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "  user_id BIGINT NOT NULL," +
                    "  role_id BIGINT NOT NULL," +
                    "  create_time DATETIME" +
                    ")");

            // 插 3 个测试用户(显式 userId,按 createTime 不同顺序)
            // userId=1(最后建), userId=2(最早建), userId=3(中间建)
            stmt.execute("INSERT INTO t_user (user_id, username, real_name, password, status, create_time) VALUES " +
                    "(2, 'bob', '鲍勃', 'x', 1, '2026-06-19 10:00:00')");
            stmt.execute("INSERT INTO t_user (user_id, username, real_name, password, status, create_time) VALUES " +
                    "(3, 'charlie', '查理', 'x', 1, '2026-06-19 11:00:00')");
            stmt.execute("INSERT INTO t_user (user_id, username, real_name, password, status, create_time) VALUES " +
                    "(1, 'alice', '爱丽丝', 'x', 1, '2026-06-19 12:00:00')");
        }
    }

    @Test
    @DisplayName("BUG 复现:UserServiceImpl.page 必须支持 orderBy=userId & order=asc 升序排列,而不是写死 createTime DESC")
    void pageMustSupportOrderByUserIdAsc() {
        UserQueryDTO query = new UserQueryDTO();
        query.setOrderBy("userId");
        query.setOrder("asc");

        var result = userService.page(new Page<>(1, 10), query);
        List<UserVO> records = result.getRecords();

        assertEquals(3, records.size(), "应该返回 3 条");
        // 期望顺序:userId 1, 2, 3(升序)
        assertEquals(1L, records.get(0).getUserId());
        assertEquals(2L, records.get(1).getUserId());
        assertEquals(3L, records.get(2).getUserId());
    }

    @Test
    @DisplayName("UserServiceImpl.page 必须支持 orderBy=userId & order=desc 降序排列")
    void pageMustSupportOrderByUserIdDesc() {
        UserQueryDTO query = new UserQueryDTO();
        query.setOrderBy("userId");
        query.setOrder("desc");

        var result = userService.page(new Page<>(1, 10), query);
        List<UserVO> records = result.getRecords();

        assertEquals(3, records.size());
        // 期望顺序:userId 3, 2, 1(降序)
        assertEquals(3L, records.get(0).getUserId());
        assertEquals(2L, records.get(1).getUserId());
        assertEquals(1L, records.get(2).getUserId());
    }

    @Test
    @DisplayName("UserServiceImpl.page 默认行为(orderBy 未传)应该是稳定的,不能 500")
    void pageMustHaveStableDefaultBehavior() {
        UserQueryDTO query = new UserQueryDTO();
        // 不设 orderBy/order

        var result = userService.page(new Page<>(1, 10), query);
        assertEquals(3, result.getRecords().size(), "默认行为不应报错,应返回 3 条");
        assertTrue(result.getTotal() > 0, "total 应该 > 0(分页拦截器要工作)");
    }
}
