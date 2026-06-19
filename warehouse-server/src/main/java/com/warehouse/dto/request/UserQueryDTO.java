package com.warehouse.dto.request;

import lombok.Data;

@Data
public class UserQueryDTO {

    private String username;
    private String realName;
    private String phone;
    private String email;
    private Integer status;

    /**
     * P0 修复:支持按指定字段排序
     * 原因:前端 el-table 需要可点击列头切换升降序,后端必须能接收排序参数
     * 取值白名单(由 UserServiceImpl.page 校验):userId / username / realName / createTime
     * 默认值:userId(自然升序,符合用户对"ID 排序"的预期)
     */
    private String orderBy;

    /**
     * P0 修复:排序方向
     * 取值:asc / desc
     * 默认值:asc
     */
    private String order;
}
