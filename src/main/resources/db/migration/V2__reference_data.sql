-- Reference data: permissions, system roles, role-permission mappings, plans.

INSERT INTO permissions (code, name, description) VALUES
  ('tenant.read', 'Read tenants', 'View tenant records'),
  ('tenant.create', 'Create tenants', 'Onboard new tenants'),
  ('tenant.update', 'Update tenants', 'Edit tenant settings'),
  ('tenant.suspend', 'Suspend tenants', 'Suspend or reactivate tenants'),
  ('tenant.delete', 'Delete tenants', 'Soft-delete tenants'),
  ('subscription.read', 'Read subscriptions', 'View plans and subscriptions'),
  ('subscription.manage', 'Manage subscriptions', 'Change plans and billing state'),
  ('restaurant.read', 'Read restaurant', 'View restaurant profile'),
  ('restaurant.update', 'Update restaurant', 'Edit restaurant profile'),
  ('branch.read', 'Read branches', 'View branches'),
  ('branch.manage', 'Manage branches', 'Create and edit branches'),
  ('employee.read', 'Read employees', 'View employees'),
  ('employee.manage', 'Manage employees', 'Create and edit employees'),
  ('menu.read', 'Read menu', 'View menu data'),
  ('menu.manage', 'Manage menu', 'Create and edit menu data'),
  ('table.read', 'Read tables', 'View tables and QR codes'),
  ('table.manage', 'Manage tables', 'Create, edit and rotate table QR codes'),
  ('order.read', 'Read orders', 'View orders'),
  ('order.manage', 'Manage orders', 'Accept and update orders'),
  ('order.cancel', 'Cancel orders', 'Cancel orders'),
  ('payment.read', 'Read payments', 'View payments and refunds'),
  ('payment.refund', 'Refund payments', 'Issue refunds'),
  ('report.read', 'Read reports', 'View sales and operational reports'),
  ('audit.read', 'Read audit log', 'View audit records'),
  ('notification.read', 'Read notifications', 'View notifications'),
  ('notification.manage', 'Manage notifications', 'Send and update notifications'),
  ('file.upload', 'Upload files', 'Upload images and documents');

INSERT INTO roles (tenant_id, code, name, description, system_role) VALUES
  (NULL, 'SUPER_ADMIN', 'Super admin', 'Platform administrator', b'1'),
  (NULL, 'TENANT_OWNER', 'Tenant owner', 'Owns a restaurant tenant', b'1'),
  (NULL, 'TENANT_MANAGER', 'Tenant manager', 'Manages restaurant operations', b'1'),
  (NULL, 'BRANCH_MANAGER', 'Branch manager', 'Manages a single branch', b'1'),
  (NULL, 'STAFF', 'Staff', 'Handles orders and tables', b'1'),
  (NULL, 'KITCHEN', 'Kitchen', 'Prepares orders', b'1'),
  (NULL, 'CUSTOMER', 'Customer', 'Registered customer', b'1');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'SUPER_ADMIN';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
  'tenant.read', 'subscription.read', 'subscription.manage',
  'restaurant.read', 'restaurant.update',
  'branch.read', 'branch.manage',
  'employee.read', 'employee.manage',
  'menu.read', 'menu.manage',
  'table.read', 'table.manage',
  'order.read', 'order.manage', 'order.cancel',
  'payment.read', 'payment.refund',
  'report.read', 'audit.read',
  'notification.read', 'notification.manage',
  'file.upload'
)
WHERE r.code = 'TENANT_OWNER';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
  'restaurant.read', 'branch.read', 'employee.read', 'employee.manage',
  'menu.read', 'menu.manage', 'table.read', 'table.manage',
  'order.read', 'order.manage', 'order.cancel',
  'payment.read', 'report.read', 'notification.read', 'file.upload'
)
WHERE r.code = 'TENANT_MANAGER';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
  'restaurant.read', 'branch.read', 'employee.read',
  'menu.read', 'menu.manage', 'table.read', 'table.manage',
  'order.read', 'order.manage', 'payment.read', 'report.read',
  'notification.read', 'file.upload'
)
WHERE r.code = 'BRANCH_MANAGER';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('menu.read', 'table.read', 'order.read', 'order.manage', 'file.upload')
WHERE r.code = 'STAFF';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('order.read', 'order.manage')
WHERE r.code = 'KITCHEN';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('menu.read', 'order.read', 'payment.read')
WHERE r.code = 'CUSTOMER';

INSERT INTO subscription_plans (code, name, description, price_monthly, currency, max_branches, max_employees, max_menu_items, features, is_public, sort_order)
VALUES
  ('STARTER', 'Starter', 'Single-location restaurants getting started.', 29.00, 'USD', 1, 10, 100,
   '["menu","tables","orders","qr-ordering","email-support"]', b'1', 10),
  ('GROWTH', 'Growth', 'Multi-branch operations with reporting.', 79.00, 'USD', 5, 50, 500,
   '["menu","tables","orders","qr-ordering","kitchen-display","reports","priority-support"]', b'1', 20),
  ('ENTERPRISE', 'Enterprise', 'Unlimited branches and advanced controls.', 199.00, 'USD', 100, 1000, 5000,
   '["menu","tables","orders","qr-ordering","kitchen-display","reports","audit","sso","dedicated-support"]', b'1', 30);
