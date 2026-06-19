# 项目名称
仓库管理系统
## 1. 项目概述
基于 Web 的现代、高效仓库管理系统（WMS），采用前后端分离架构，核心解决库存不对称、批次追溯难、拣货效率低等痛点。支持多仓管理、智能出入库、动态盘点与数据大屏。

## 2. 技术栈
**后端：** Spring Boot 3.x, Spring Security, JWT, MyBatis-Plus
**前端：** Vue 3.x (Composition API), Vite, Pinia, ECharts
**数据/中间件：** MySQL 8.x, Redis

## 3. 注意事项
**修复bug时先复现这个bug,然后编写对应的测试文件,证明这个bug已经被修复**

**每次对话自动git commit一份代码,带有简略的提交信息**

**绝不提交硬编码的 API 密钥，也不得绕过 `.env.example` 的配置**

## 4. 反复踩过的坑(必须牢记,避免重复犯错)

### 4.1 MySQL AUTO_INCREMENT 不会因事务回滚而减
- **场景**:用户报"新增用户,id 直接跳到 32,而不是 7"
- **真相**:`@Transactional` 标记的 service 方法如果抛异常回滚,MySQL 已经分配的自增 ID **不会归还**
- **为什么**:MySQL 故意设计为不重用 ID,避免 FK 引用指向错误的行
- **触发链示例**:
  1. `SecurityUtils.getCurrentUserId()` 抛 `NumberFormatException`(上一个 bug)
  2. `PermissionAspect` 不 catch,异常冒泡到 controller
  3. `@Transactional` 触发回滚,但 `INSERT INTO t_user (...) VALUES ()` 已经分配了 user_id
  4. 用户重试 25 次,每次都 500,但 AUTO_INCREMENT 递增到 32
  5. 修复 SecurityUtils 后,下一次成功插入就是 id=32
- **怎么修**:**改不了**(MySQL 限制),只能:
  - 提前修主 bug,避免回滚
  - 接受 ID 不连续,不要在 UI 强求"按 ID 升序连续"
  - 真要重置:用 `TRUNCATE TABLE`(会清空数据)或 `ALTER TABLE ... AUTO_INCREMENT = N`(N 必须 >= max+1)
- **怎么验**:查 `SELECT TABLE_NAME, AUTO_INCREMENT FROM information_schema.tables WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='t_xxx'`

### 4.2 前端 `el-table` 列默认不可排序
- **场景**:用户报"id 列不能点击排序"
- **真相**:Element Plus 的 `<el-table-column>` 默认不响应点击排序,需要显式 `sortable="custom"`,并在 `<el-table>` 上加 `@sort-change="handler"`
- **错误模式**:只在 `<el-table-column>` 加 `sortable`(默认服务端排序会失败),**后端 Service** 又写死 `wrapper.orderByDesc(...)`,导致点击无效
- **正确做法**:
  1. 后端 DTO 加 `orderBy` (白名单) + `order` (asc/desc) 字段
  2. Service 用白名单构建 `wrapper.orderBy(true, isAsc, User::getXxx)`,**禁止字符串拼接**
  3. 前端 `sortable="custom"` + `@sort-change` + Element Plus 的 `ascending/descending` 翻译为后端的 `asc/desc`
  4. 列 prop 是 `id` 但后端字段是 `userId` 时,**做映射表**(`const propToField = { id: 'userId' }`)

### 4.3 MyBatis-Plus 3.5.9 分页拦截器在独立模块
- **场景**:用户报"列表显示正常,但分页 total 永远是 0"
- **真相**:MyBatis-Plus 3.5.9 把 `PaginationInnerInterceptor` 从 `mybatis-plus-extension` 移到了 `mybatis-plus-jsqlparser` 独立模块
- **必需三件套**(缺一不可):
  1. `pom.xml` 引入 `com.baomidou:mybatis-plus-jsqlparser:${mybatis-plus.version}`
  2. `@Bean MybatisPlusInterceptor` 注册(否则 SqlSessionFactory 拦截器链里压根没这玩意儿)
  3. `interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL))`
- **检测方式**:`new SqlSessionFactory().getConfiguration().getInterceptors()` 看链上有没有 `MybatisPlusInterceptor`

### 4.4 排查 BUG 必须看真实后端日志,不能信前端"成功"提示
- **场景**:用户报"修改数据后刷新看不到"
- **真相**:前端 ElMessage.success 可能源于:① 真成功;② 异常被全局 axios 拦截器吞掉;③ 后端 500 但前端包装成了 success
- **正确做法**:
  1. `nohup java -jar xxx.jar > backend.log 2>&1 &` 启动,**强制重定向日志**
  2. `tail -f backend.log | grep -A 30 "Exception\|ERROR"`
  3. 区分 `HttpMessageNotReadableException`(请求体问题)vs `NumberFormatException`(业务逻辑)vs `SQLException`(SQL 问题)

### 4.5 TDD 顺序铁律(我多次违反)
- **错误**:看完代码 → 改代码 → 写测试 → 测试通过(伪绿灯)
- **正确**:**RED 先**(测试必须因为代码缺失而失败)→ **GREEN 后**(最小化代码让测试通过)→ **REFACTOR 最后**
- **判断 GREEN 是否真 GREEN**:看错误信息是不是"找不到方法"或"expected X, got Y"(代码缺失导致),而不是 NullPointerException(测试本身写错)
