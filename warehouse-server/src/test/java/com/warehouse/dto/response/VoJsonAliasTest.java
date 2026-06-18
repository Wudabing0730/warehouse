package com.warehouse.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P0-3 / P0-5 / P0-7 / P0-9 修复验证:
 * 所有 VO 的主 ID 字段必须额外序列化一个 "id" 别名,以兼容前端 row.id 的访问方式。
 *
 * Bug 复现:前端表格读 row.id,后端 VO 用 userId/roleId/orderId/recordId/checkId/
 *          categoryId/productId/supplierId/customerId,导致 row.id === undefined,
 *          进而所有"编辑/删除/详情/审核"按钮触发的 URL 变成 /xxx/undefined,后端 404/400。
 *
 * 修复策略:VO 主 ID 字段加 @JsonProperty("id") 别名,Jackson 同时输出两套字段名,
 *          前端 row.id 不再是 undefined,且后端语义化命名得到保留。
 *
 * 附带验证:
 *   P0-5 BorrowRecordVO.quantity → "borrowQuantity"
 *   P0-7 InventoryCheckVO.operatorName → "checkUser"
 */
class VoJsonAliasTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("UserVO 必须额外序列化 id 字段(原 userId 保留)")
    void userVoHasIdAlias() throws Exception {
        UserVO vo = new UserVO();
        setField(vo, "userId", 100L);
        JsonNode json = mapper.valueToTree(vo);
        assertNotNull(json.get("id"), "UserVO JSON 缺少 'id' 字段,前端 row.id 会是 undefined");
        assertTrue(json.get("id").asLong() == 100L);
        assertNotNull(json.get("userId"), "UserVO JSON 必须保留原始 userId 字段");
    }

    @Test
    @DisplayName("RoleVO 必须额外序列化 id 字段")
    void roleVoHasIdAlias() throws Exception {
        RoleVO vo = new RoleVO();
        setField(vo, "roleId", 5L);
        JsonNode json = mapper.valueToTree(vo);
        assertNotNull(json.get("id"));
        assertTrue(json.get("id").asLong() == 5L);
        assertNotNull(json.get("roleId"));
    }

    @Test
    @DisplayName("CategoryVO 必须额外序列化 id 字段(el-tree 用 categoryId 作 node-key,前端 row.id 用于按钮)")
    void categoryVoHasIdAlias() throws Exception {
        CategoryVO vo = new CategoryVO();
        setField(vo, "categoryId", 7L);
        JsonNode json = mapper.valueToTree(vo);
        assertNotNull(json.get("id"));
        assertTrue(json.get("id").asLong() == 7L);
    }

    @Test
    @DisplayName("ProductVO 必须额外序列化 id 字段")
    void productVoHasIdAlias() throws Exception {
        ProductVO vo = new ProductVO();
        setField(vo, "productId", 42L);
        JsonNode json = mapper.valueToTree(vo);
        assertNotNull(json.get("id"));
        assertTrue(json.get("id").asLong() == 42L);
    }

    @Test
    @DisplayName("SupplierVO 必须额外序列化 id 字段")
    void supplierVoHasIdAlias() throws Exception {
        SupplierVO vo = new SupplierVO();
        setField(vo, "supplierId", 3L);
        JsonNode json = mapper.valueToTree(vo);
        assertNotNull(json.get("id"));
        assertTrue(json.get("id").asLong() == 3L);
    }

    @Test
    @DisplayName("CustomerVO 必须额外序列化 id 字段")
    void customerVoHasIdAlias() throws Exception {
        CustomerVO vo = new CustomerVO();
        setField(vo, "customerId", 9L);
        JsonNode json = mapper.valueToTree(vo);
        assertNotNull(json.get("id"));
        assertTrue(json.get("id").asLong() == 9L);
    }

    @Test
    @DisplayName("InboundOrderVO 必须额外序列化 id 字段(入库详情/审核 row.id)")
    void inboundOrderVoHasIdAlias() throws Exception {
        InboundOrderVO vo = new InboundOrderVO();
        setField(vo, "orderId", 1001L);
        JsonNode json = mapper.valueToTree(vo);
        assertNotNull(json.get("id"));
        assertTrue(json.get("id").asLong() == 1001L);
    }

    @Test
    @DisplayName("OutboundOrderVO 必须额外序列化 id 字段(出库详情/审核 row.id)")
    void outboundOrderVoHasIdAlias() throws Exception {
        OutboundOrderVO vo = new OutboundOrderVO();
        setField(vo, "orderId", 2002L);
        JsonNode json = mapper.valueToTree(vo);
        assertNotNull(json.get("id"));
        assertTrue(json.get("id").asLong() == 2002L);
    }

    @Test
    @DisplayName("BorrowRecordVO 必须额外序列化 id + borrowQuantity 字段(P0-3 + P0-5)")
    void borrowRecordVoHasIdAndBorrowQuantityAlias() throws Exception {
        BorrowRecordVO vo = new BorrowRecordVO();
        setField(vo, "recordId", 333L);
        setField(vo, "quantity", new java.math.BigDecimal("50"));
        JsonNode json = mapper.valueToTree(vo);
        // P0-3
        assertNotNull(json.get("id"), "BorrowRecordVO JSON 缺少 'id',前端 row.id 是 undefined");
        assertTrue(json.get("id").asLong() == 333L);
        // P0-5:归还对话框 :max="remaining" 计算依赖 borrowQuantity
        assertNotNull(json.get("borrowQuantity"),
                "BorrowRecordVO JSON 缺少 'borrowQuantity',前端 row.borrowQuantity 为 undefined → remaining 永远是 0");
        assertTrue(json.get("borrowQuantity").asDouble() == 50.0);
    }

    @Test
    @DisplayName("InventoryCheckVO 必须额外序列化 id + checkUser 字段(P0-3 + P0-7)")
    void inventoryCheckVoHasIdAndCheckUserAlias() throws Exception {
        InventoryCheckVO vo = new InventoryCheckVO();
        setField(vo, "checkId", 88L);
        setField(vo, "operatorName", "张三");
        JsonNode json = mapper.valueToTree(vo);
        // P0-3
        assertNotNull(json.get("id"), "InventoryCheckVO JSON 缺少 'id',确认盘点 URL 变 undefined");
        assertTrue(json.get("id").asLong() == 88L);
        // P0-7
        assertNotNull(json.get("checkUser"),
                "InventoryCheckVO JSON 缺少 'checkUser',盘点表格/详情操作人列为空白");
        assertTrue(json.get("checkUser").asText().equals("张三"));
    }

    /** 通过反射给 Lombok @Data 类的 private 字段赋值 */
    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }
}