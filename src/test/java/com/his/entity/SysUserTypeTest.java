package com.his.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SysUser 类型验证测试
 *
 * <p>验证 SysUser.status 字段从 Integer 改为 Short 后的正确性</p>
 *
 * @author HIS 开发团队
 */
@DisplayName("SysUser 类型验证测试")
class SysUserTypeTest {

    @Test
    @DisplayName("status 字段应该是 Short 类型")
    void statusFieldTypeShouldBeShort() {
        SysUser user = new SysUser();

        // 验证可以设置 Short 类型
        Short enabledStatus = (short) 1;
        Short disabledStatus = (short) 0;

        user.setStatus(enabledStatus);
        assertEquals(enabledStatus, user.getStatus(), "启用状态应该是 1");

        user.setStatus(disabledStatus);
        assertEquals(disabledStatus, user.getStatus(), "停用状态应该是 0");
    }

    @Test
    @DisplayName("status 字段应该支持 null")
    void statusFieldShouldSupportNull() {
        SysUser user = new SysUser();

        // 验证可以设置为 null（虽然数据库定义为 NOT NULL）
        user.setStatus(null);
        assertNull(user.getStatus(), "status 应该支持 null");
    }

    @Test
    @DisplayName("status 字段应该正确匹配数据库 SMALLINT 类型")
    void statusFieldShouldMatchDatabaseSmallInt() {
        SysUser user = new SysUser();

        // Short 类型对应 PostgreSQL 的 SMALLINT (int2)
        // 范围：-32768 到 32767
        user.setStatus((short) 1);
        assertTrue(user.getStatus() instanceof Short, "status 应该是 Short 类型");

        // 验证 Short 类型的范围
        user.setStatus(Short.MAX_VALUE);
        assertEquals(Short.MAX_VALUE, user.getStatus());

        user.setStatus(Short.MIN_VALUE);
        assertEquals(Short.MIN_VALUE, user.getStatus());
    }

    @Test
    @DisplayName("status 字段应该支持自动装箱和拆箱")
    void statusFieldShouldSupportAutoBoxing() {
        SysUser user = new SysUser();

        // 自动装箱：short → Short
        user.setStatus((short) 1);

        // 自动拆箱：Short → short
        short status = user.getStatus();
        assertEquals(1, status);

        // 比较 Short 对象
        assertTrue(user.getStatus() == (short) 1);
    }
}
