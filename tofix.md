# 仓库管理系统功能可用性排查 — 修复清单

> 排查日期:2026-06-18
> 排查范围:前后端 29 个 Vue 页面 + 16 个 Controller + 全部 DTO/VO/Entity/SQL
> 排查方法:逐模块追踪 URL → 前端请求 → 后端 Controller → Service → Mapper → DB 完整链路

## 总览

| 优先级 | 模块 | 问题数 | 状态 |
|---|---|---|---|
| **P0** 致命 | 角色/权限分配、借还、盘点、操作日志、仪表盘、入库/出库详情 | 9 | 🔧 实施中 |
| **P1** 严重 | 全局分页、基础资料筛选、VO 字段填充、用户密码重置 | 7 | ⏳ 待修复 |
| **P2** 中等 | 下拉框 label、审计字段名、JWT/CORS、种子数据 | 4 | ⏳ 待修复 |
| **P3** 低优 | 测试缺失、图表缺失、初始化流程缺失 | 2 | ⏳ 待修复 |

---

## P0 — 致命(必须立即修复)

### ✅ P0-1 【角色-权限分配】对话框节点无法勾选 — ✅ 已修复
- **现象**:打开"权限分配"对话框,看到树形结构但完全无法勾选已有权限;保存后 `t_role_permission` 被全量清空
- **根因**:Element Plus `el-tree` 配置与后端 VO 字段不匹配
- **文件**: `warehouse-web/src/views/system/RoleView.vue:106,108`
- **修复**: `node-key="permissionId"`、`props: { label: 'permissionName', children: 'children' }`
- **测试**: `warehouse-web/src/__tests__/views/RoleViewPermissionTree.test.ts`(4 个 case 全过)
  - 断言 `node-key="permissionId"`、`:props={label:'permissionName', children:'children'}`、PermissionVO 字段契约
  - **反向证明**:临时还原为 `node-key="id"` + `label: 'name'` → 2 个 case 失败,定位到 line 35
  - 恢复后 `Test Files 1 passed | Tests 4 passed` ✅
- **状态**: ✅ 已修复

### ✅ P0-2 【角色-权限分配】管理员调用也被 403 — ✅ 已修复
- **现象**:管理员登录,分配权限时点击保存,提示"权限不足: role:assignPermissions"
- **根因**:`@RequirePermission` 注解值与种子数据 `permission_code` 不一致 — 全局问题,共 28 处注解错配
- **文件**:
  - `UserController.java` / `RoleController.java` / `ProductController.java` / `ProductCategoryController.java`
  - `WarehouseLocationController.java` / `SupplierController.java` / `CustomerController.java`
  - `InboundController.java` / `OutboundController.java` / `BorrowController.java` / `InventoryCheckController.java`
  - `sql/02-seed.sql:36-105`(种子数据,正确)
- **修复**:所有 28 处注解值统一改为与种子数据一致:
  - `user:*` → `system:user:*`、`role:*` → `system:role:*`(含 `role:assignPermissions` → `system:role:assign`)
  - `product:*` → `base:product:*`、`category:*` → `base:category:*`、`location:*` → `base:location:*`
  - `supplier:*` → `base:supplier:*`、`customer:*` → `base:customer:*`
  - `inbound:confirm` → `inbound:audit`、`outbound:confirm` → `outbound:audit`
  - `inventoryCheck:*` → `inventory:*`(`borrow:create/return` 原本就匹配)
- **测试**: `warehouse-server/src/test/java/com/warehouse/annotation/PermissionAnnotationConsistencyTest.java`
  - 反射扫描 11 个 Controller 所有 `@RequirePermission`,断言 value ∈ 种子 `permission_code` 集合
  - **反向证明**:临时把 `system:role:assign` 改回 `role:assignPermissions` → 测试失败并精确报错 `RoleController#assignPermissions → @RequirePermission("role:assignPermissions") 不在 02-seed.sql 的 permission_code 中`
  - 恢复修复后 `Tests run: 2, Failures: 0, Errors: 0` ✅
- **状态**: ✅ 已修复 (commit pending)

### ✅ P0-3 【全局 ID 字段错配】详情/编辑/删除 URL 全部 undefined — ✅ 已修复
- **现象**:几乎所有列表的"查看详情"、"编辑"、"删除"、"审核"按钮点击后请求 URL 变成 `/xxx/undefined`,后端返回 400/404
- **根因**:前端表格读取 `row.id`,但后端 VO 序列化的是 `userId/roleId/orderId/recordId/checkId/categoryId/productId/supplierId/customerId`
- **修复方案**:VO 增加 `@JsonProperty("id")` getter 方法,既保留后端语义命名又兼容前端 `row.id` 访问(最小侵入)
- **修改文件**(10 个 VO):
  - `UserVO` / `RoleVO` / `CategoryVO` / `ProductVO` / `SupplierVO` / `CustomerVO`
  - `InboundOrderVO` / `OutboundOrderVO` / `BorrowRecordVO`(含 P0-5 `getBorrowQuantity`)/ `InventoryCheckVO`(含 P0-7 `getCheckUser`)
- **测试**: `warehouse-server/src/test/java/com/warehouse/dto/response/VoJsonAliasTest.java`(10 个 case)
  - 用 `ObjectMapper.valueToTree` 验证每个 VO 同时输出原字段名 + `id` 别名
  - **反向证明**:UserVO 临时注释 `getId()` → 1 个 case 失败,定位到 `id === null`
  - 恢复后 `Tests run: 12, Failures: 0`(合并 P0-2 测试)
- **状态**: ✅ 已修复

### ✅ P0-5 【归还】剩余数量永远为 0 — ✅ 已修复(随 P0-3 一起)
- **修复**: `BorrowRecordVO` 加 `getBorrowQuantity()` getter,Jackson 同时输出 `quantity` 与 `borrowQuantity`,前端 `row.borrowQuantity` 不再 undefined
- **测试**: `VoJsonAliasTest.borrowRecordVoHasIdAndBorrowQuantityAlias` 验证 `borrowQuantity` 字段存在且数值正确
- **状态**: ✅ 已修复

### ✅ P0-7 【盘点】确认盘点 404 — ✅ 已修复(随 P0-3 一起)
- **修复**: `InventoryCheckVO` 加 `getCheckUser()` getter,前端 `row.checkUser` 不再 undefined
- **测试**: `VoJsonAliasTest.inventoryCheckVoHasIdAndCheckUserAlias` 验证 `id` + `checkUser` 都存在
- **状态**: ✅ 已修复

### ✅ P0-9 【入库/出库】详情/审核对话框打不开 — ✅ 已修复(随 P0-3 一起)
- **修复**: `InboundOrderVO` / `OutboundOrderVO` 加 `getId()` getter,前端 `row.id` 不再 undefined
- **测试**: `VoJsonAliasTest.inboundOrderVoHasIdAlias` / `outboundOrderVoHasIdAlias`
- **状态**: ✅ 已修复

### ✅ P0-4 【借出/归还】日期格式 400 错误 — ✅ 已修复
- **现象**:点击"借出"或"归还"提交,后端返回 400 Bad Request
- **根因**:前端发 `YYYY-MM-DD HH:mm:ss` 字符串,后端 DTO 是 `LocalDate`,Jackson 反序列化失败
- **修复**:DTO / Entity / VO / QueryDTO 全链路 `LocalDate` → `LocalDateTime`
  - `BorrowCreateDTO` / `BorrowReturnDTO`(DTO 入参)
  - `entity/BorrowRecord.java`(数据库映射)
  - `BorrowRecordVO`(出参)
  - `BorrowQueryDTO`(查询 startDate/endDate)
  - `BorrowServiceImpl.returnItem` 中 `LocalDate.now()` → `LocalDateTime.now()`
  - `sql/01-schema.sql:t_borrow_record.borrow_date / expected_return_date / actual_return_date` DATE → DATETIME
- **测试**: `warehouse-server/src/test/java/com/warehouse/dto/request/BorrowDtoDateTimeTest.java`(3 个 case)
  - 验证 `yyyy-MM-dd HH:mm:ss` 字符串能成功反序列化为 LocalDateTime
  - 验证序列化输出包含时分秒
  - **反向证明**:把 `BorrowCreateDTO` 还原 `LocalDate` → 编译失败(BorrowServiceImpl 无法赋值)+ 测试失败
- **状态**: ✅ 已修复

### ✅ P0-5 【归还】剩余数量永远为 0,无法归还 — 待修复
- **现象**:打开归还对话框,实盘数量输入框 `:max="remaining"` 永远 = 0
- **根因**:前端读 `row.borrowQuantity`,后端 VO 字段是 `quantity`
- **修复**: 同 P0-3,VO 加 `@JsonProperty("borrowQuantity")` 别名
- **状态**: ⏳ 待修复

### ✅ P0-6 【操作日志】状态筛选永远查不到 — ✅ 已修复
- **现象**:选择"成功"或"失败"过滤操作日志,结果总是空
- **根因**:前端 status 选项值是字符串 `'success' / 'fail'`,后端 DTO 是 `Integer`(1=成功/0=失败)
- **修复**: `OperationLogView.vue`
  - 选项 `:value="1"` / `:value="0"` 整型绑定
  - 表格展示 `row.status === 1` 整型比对
  - `searchForm.status` 类型 `'' as string` → `null as number | null`
  - `LogEntry.status` 类型 `string` → `number`
- **测试**: `warehouse-web/src/__tests__/views/OperationLogViewStatus.test.ts`(6 个 case)
  - 断言 `:value="1"` / `:value="0"` 出现,字符串 value 不出现
  - 断言 searchForm/status 字段类型为 number
  - 断言后端 DTO `private Integer status` 契约未变
- **状态**: ✅ 已修复

### ✅ P0-7 【盘点】确认盘点 404 — 待修复
- **现象**:盘点结果列表点击"处理/确认"按钮,URL 变 `/inventory-checks/undefined/confirm`
- **根因**:前端读 `row.id` 后端是 `checkId`;`row.checkUser` 后端是 `operatorName`
- **修复**: 同 P0-3
- **状态**: ⏳ 待修复

### ✅ P0-8 【仪表盘】全是硬编码假数据 — ✅ 已修复
- **现象**:登录后看到的库存数量、今日入库、最近操作、预警信息全是写死的数字
- **根因**:`DashboardView.vue` 没有调用任何 API,所有 `stats` / `recentOps` / `alerts` 都在 reactive 初始化时硬编码
- **修复**:
  - **后端新增**:
    - `DashboardController` → `GET /api/v1/dashboard/summary`
    - `DashboardService` / `DashboardServiceImpl` 实时聚合 Product/Stock/InboundOrder/OutboundOrder/OperationLog
    - `DashboardSummaryVO` 含 productCount / totalStock / todayInbound / todayOutbound / alerts / recentOps
  - **前端新增/修改**:
    - `api/dashboard.ts` 新 API 客户端
    - `DashboardView.vue` 移除硬编码,`stats` 初始化 0,`onMounted` 调 `getDashboardSummary()`
- **测试**:
  - `DashboardSummaryVoJsonTest`(后端,2 个 case) — VO JSON 契约含所有 6 个字段
  - `DashboardViewApi.test.ts`(前端,8 个 case) — 断言 API 调用、无硬编码假数字、stats 初值 0、onMounted 触发等
- **状态**: ✅ 已修复

### ✅ P0-9 【入库/出库】详情/审核对话框打不开 — 待修复
- **现象**:入库单/出库单列表点击"查看详情"或"审核",对话框打不开
- **根因**:`row.id` → `orderId` 字段错配;审计字段 `auditorName → confirmOperatorName`
- **修复**: 同 P0-3
- **状态**: ⏳ 待修复

---

## P1 — 严重(必须修复)

### ⏳ P1-1 【全局分页】UI 显示的每页大小无效
### ⏳ P1-2 【供应商/客户/库位】列表搜索框无效
### ⏳ P1-3 【入库明细 VO】`locationName` 始终为空
### ⏳ P1-4 【角色列表 VO】`permissionIds` 不填充
### ⏳ P1-5 【用户密码重置】走错接口
### ⏳ P1-6 【商品分类树】整棵树 node-key 失效
### ⏳ P1-7 【操作日志】Controller 返回实体不是 VO

## P2 — 中等(建议修复)

### ⏳ P2-1 【下拉框 label 全部空白】
### ⏳ P2-2 【入库/出库审计字段错配】
### ⏳ P2-3 【JWT 过滤器】未放行 OPTIONS 预检
### ⏳ P2-4 【种子数据】`parent_id` 依赖 AUTO_INCREMENT 顺序

## P3 — 低优(后续优化)

### ⏳ P3-1 【测试覆盖】后端几乎无功能测试
### ⏳ P3-2 【仪表盘】无图表可视化

---

## 横切观察(供参考,非 bug)

1. Spring Security 配置正确:业务接口需 JWT,白名单仅 `/api/v1/auth/login` 与 `/api/v1/auth/refresh`
2. CLAUDE.md 强调"修复 bug 先复现 + 写测试",目前测试覆盖几乎为零,后续每个修复都应配套测试
3. `start.bat` 是否会自动执行 `sql/01-schema.sql → 02-seed.sql → 03-demo-data.sql` 未确认;若未自动执行,P0-1 权限树空白可能是缺种子数据而非代码 bug
4. 后端 Service 实现类齐全,无 stub;问题集中在 DTO/VO 字段命名与前端调用层

---

## 实现进度跟踪

| 编号 | 模块 | 状态 | 测试文件 |
|---|---|---|---|
| P0-1 | 权限分配树节点 | ✅ 已修复 | `warehouse-web/src/__tests__/views/RoleViewPermissionTree.test.ts` |
| P0-2 | 权限分配 403(全局 28 处注解) | ✅ 已修复 | `warehouse-server/src/test/java/com/warehouse/annotation/PermissionAnnotationConsistencyTest.java` |
| P0-3 | 全局 ID 字段错配 | ✅ 已修复 | `warehouse-server/src/test/java/com/warehouse/dto/response/VoJsonAliasTest.java` |
| P0-4 | 借还日期 400 | ✅ 已修复 | `warehouse-server/src/test/java/com/warehouse/dto/request/BorrowDtoDateTimeTest.java` |
| P0-5 | 归还剩余数量 | ✅ 已修复(随 P0-3) | `VoJsonAliasTest.borrowRecordVoHasIdAndBorrowQuantityAlias` |
| P0-6 | 操作日志状态筛选 | ✅ 已修复 | `warehouse-web/src/__tests__/views/OperationLogViewStatus.test.ts` |
| P0-7 | 盘点确认 | ✅ 已修复(随 P0-3) | `VoJsonAliasTest.inventoryCheckVoHasIdAndCheckUserAlias` |
| P0-8 | 仪表盘假数据 | ✅ 已修复 | `DashboardSummaryVoJsonTest.java` + `DashboardViewApi.test.ts` |
| P0-9 | 入库出库详情 | ✅ 已修复(随 P0-3) | `VoJsonAliasTest.inboundOrderVoHasIdAlias` / `outboundOrderVoHasIdAlias` |

**测试总计**: 后端 17 个 case,前端 18 个 case,共 35 个全部通过 ✅

---

## P1/P2/P3 任务(待后续推进)

P0 全部完成后,继续按 P1 → P2 → P3 顺序推进,每项同样需要:
1. 复现 Bug
2. 写失败测试
3. 修复代码
4. 验证测试通过
5. 更新 tofix.md