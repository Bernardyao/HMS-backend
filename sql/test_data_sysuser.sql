-- ============================================
-- 系统用户表测试数据
-- ============================================

-- 说明：密码均为 admin123 的 BCrypt 加密结果
-- 表名：his_sysuser
-- 字段：username, password, name, role_code, department_main_id, status, created_at, updated_at

-- 1. 管理员账户
INSERT INTO his_sysuser (username, password, name, role_code, department_main_id, status, created_at, updated_at)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb96VdOYCec6NRSkWBGHQXxP3y3GK3kCrVbEi1Jt6', '系统管理员', 'ADMIN', NULL, 1, NOW(), NOW());

-- 2. 医生账户（需要关联 department_main_id 到医生表的 main_id）
INSERT INTO his_sysuser (username, password, name, role_code, department_main_id, status, created_at, updated_at)
VALUES ('doctor001', '$2a$10$N.zmdr9k7uOCQb96VdOYCec6NRSkWBGHQXxP3y3GK3kCrVbEi1Jt6', '张医生', 'DOCTOR', NULL, 1, NOW(), NOW());

-- 3. 护士账户
INSERT INTO his_sysuser (username, password, name, role_code, department_main_id, status, created_at, updated_at)
VALUES ('nurse001', '$2a$10$N.zmdr9k7uOCQb96VdOYCec6NRSkWBGHQXxP3y3GK3kCrVbEi1Jt6', '李护士', 'NURSE', NULL, 1, NOW(), NOW());

-- 4. 药师账户
INSERT INTO his_sysuser (username, password, name, role_code, department_main_id, status, created_at, updated_at)
VALUES ('pharmacist001', '$2a$10$N.zmdr9k7uOCQb96VdOYCec6NRSkWBGHQXxP3y3GK3kCrVbEi1Jt6', '王药师', 'PHARMACIST', NULL, 1, NOW(), NOW());

-- 5. 收费员账户
INSERT INTO his_sysuser (username, password, name, role_code, department_main_id, status, created_at, updated_at)
VALUES ('cashier001', '$2a$10$N.zmdr9k7uOCQb96VdOYCec6NRSkWBGHQXxP3y3GK3kCrVbEi1Jt6', '赵收费员', 'CASHIER', NULL, 1, NOW(), NOW());

-- 查询验证
SELECT main_id, username, name, role_code, status, created_at FROM his_sysuser ORDER BY main_id;
