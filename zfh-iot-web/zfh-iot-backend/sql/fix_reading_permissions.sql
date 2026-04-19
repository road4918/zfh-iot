INSERT INTO sys_permission (parent_id, perm_code, perm_name, perm_type, path, component, sort_order, status)
SELECT id, 'reading:current', '当前数据', 1, '/data/current', 'data/current/index', 1, 1
FROM sys_permission WHERE perm_code = 'data' AND NOT EXISTS (
    SELECT 1 FROM sys_permission WHERE perm_code = 'reading:current'
);

INSERT INTO sys_permission (parent_id, perm_code, perm_name, perm_type, path, component, sort_order, status)
SELECT id, 'reading:history', '历史数据', 1, '/data/history', 'data/history/index', 2, 1
FROM sys_permission WHERE perm_code = 'data' AND NOT EXISTS (
    SELECT 1 FROM sys_permission WHERE perm_code = 'reading:history'
);

UPDATE sys_permission SET status = 1 WHERE perm_code IN ('reading:current', 'reading:history');

INSERT INTO sys_role_permission (role_id, perm_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.role_code = 'admin' AND p.perm_code IN ('reading:current', 'reading:history')
AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission WHERE role_id = r.id AND perm_id = p.id
);
