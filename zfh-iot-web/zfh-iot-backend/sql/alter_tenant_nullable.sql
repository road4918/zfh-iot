-- ============================================
-- 修改用户表结构
-- ============================================

-- 先删除原有的唯一索引
ALTER TABLE sys_user DROP INDEX uk_tenant_username;

-- 添加新的唯一索引（仅 username）
ALTER TABLE sys_user ADD UNIQUE KEY uk_username (username);

-- 修改 tenant_id 为可为空
ALTER TABLE sys_user MODIFY COLUMN tenant_id BIGINT DEFAULT NULL COMMENT 'Tenant ID (null for platform users)';

-- 添加新的索引
ALTER TABLE sys_user ADD INDEX idx_user_type (user_type);

-- ============================================
-- 修改角色表结构
-- ============================================

-- 先删除原有的唯一索引
ALTER TABLE sys_role DROP INDEX uk_tenant_code;

-- 添加新的唯一索引（仅 role_code）
ALTER TABLE sys_role ADD UNIQUE KEY uk_role_code (role_code);

-- 修改 tenant_id 为可为空
ALTER TABLE sys_role MODIFY COLUMN tenant_id BIGINT DEFAULT NULL COMMENT 'Tenant ID (null for platform roles)';

-- 添加新的索引
ALTER TABLE sys_role ADD INDEX idx_role_type (role_type);

-- ============================================
-- 可选：更新现有数据（如有需要）
-- ============================================

-- 将 admin 用户设置为平台用户类型
UPDATE sys_user SET user_type = 0, tenant_id = NULL WHERE username = 'admin';

-- 将 admin 角色设置为平台角色类型
UPDATE sys_role SET role_type = 0, tenant_id = NULL WHERE role_code = 'admin';
