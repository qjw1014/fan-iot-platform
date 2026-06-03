-- Phase 8: seed RBAC roles and baseline system log.

INSERT INTO roles (role_code, role_name, description, enabled)
SELECT 'admin', '系统管理员', '拥有平台管理权限', true
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE role_code = 'admin');

INSERT INTO roles (role_code, role_name, description, enabled)
SELECT 'operator', '运维人员', '负责设备、告警和数据运维', true
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE role_code = 'operator');

INSERT INTO roles (role_code, role_name, description, enabled)
SELECT 'viewer', '只读用户', '仅查看监控、历史和告警数据', true
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE role_code = 'viewer');

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.role_code = 'admin'
WHERE u.username = 'admin'
  AND NOT EXISTS (
      SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id
  );

INSERT INTO system_logs (log_type, log_level, module, operation, message)
VALUES ('system', 'info', 'Phase8', 'migration', 'Phase 8 RBAC and system log migration applied');
