-- Dashboard visibility permission for restaurant tenant roles.
INSERT IGNORE INTO permissions (code, name, description) VALUES
  ('dashboard.read', 'View dashboard', 'See the restaurant dashboard overview');

-- Grant dashboard.read to every tenant-facing system role so they can see the Dashboard tab.
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'dashboard.read' AND p.is_active = 1 AND p.is_delete = 0
WHERE r.code IN ('TENANT_OWNER', 'TENANT_MANAGER', 'BRANCH_MANAGER', 'STAFF', 'KITCHEN')
  AND r.tenant_id IS NULL
  AND r.is_active = 1 AND r.is_delete = 0;

-- SUPER_ADMIN receives dashboard.read automatically via the global all-permissions grant.
