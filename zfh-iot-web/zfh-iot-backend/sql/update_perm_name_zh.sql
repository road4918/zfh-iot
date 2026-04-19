-- ============================================
-- 更新权限名称为中文
-- ============================================

-- 更新主菜单
UPDATE sys_permission SET perm_name = '首页' WHERE perm_code = 'dashboard';
UPDATE sys_permission SET perm_name = '系统管理' WHERE perm_code = 'system';
UPDATE sys_permission SET perm_name = '档案管理' WHERE perm_code = 'archive';
UPDATE sys_permission SET perm_name = '抄表数据' WHERE perm_code = 'data';

-- 更新系统管理子菜单
UPDATE sys_permission SET perm_name = '租户管理' WHERE perm_code = 'tenant:list';
UPDATE sys_permission SET perm_name = '用户管理' WHERE perm_code = 'user:list';
UPDATE sys_permission SET perm_name = '角色管理' WHERE perm_code = 'role:list';
UPDATE sys_permission SET perm_name = '操作日志' WHERE perm_code = 'log:list';

-- 更新档案管理子菜单
UPDATE sys_permission SET perm_name = '网关管理' WHERE perm_code = 'gateway:list';
UPDATE sys_permission SET perm_name = '表计管理' WHERE perm_code = 'meter:list';
UPDATE sys_permission SET perm_name = '群组管理' WHERE perm_code = 'group:list';
UPDATE sys_permission SET perm_name = '协议管理' WHERE perm_code = 'protocol:list';
UPDATE sys_permission SET perm_name = '厂商管理' WHERE perm_code = 'manufacturer:list';

-- 更新抄表数据子菜单
UPDATE sys_permission SET perm_name = '当前数据' WHERE perm_code = 'reading:current';
UPDATE sys_permission SET perm_name = '历史数据' WHERE perm_code = 'reading:history';

-- 添加 reading:list 权限代码（用于菜单显示控制）
INSERT INTO sys_permission (parent_id, perm_code, perm_name, perm_type, path, component, icon, sort_order) 
SELECT parent_id, 'reading:list', '抄表数据查看', perm_type, path, component, icon, sort_order 
FROM sys_permission WHERE perm_code = 'data';

-- 给管理员角色分配新权限
INSERT INTO sys_role_permission (role_id, perm_id)
SELECT 1, id FROM sys_permission WHERE perm_code = 'reading:list';
