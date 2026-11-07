-- Employee & staff management: restaurant staff roles, task management, table assignments.

-- ---------------------------------------------------------------------------
-- 1. Additional restaurant staff roles (system roles, tenant_id NULL)
-- ---------------------------------------------------------------------------
INSERT INTO roles (tenant_id, code, name, description, system_role) VALUES
  (NULL, 'ORDER_CONFIRMATION', 'Order Confirmation Staff', 'Views and confirms or rejects incoming orders', b'1'),
  (NULL, 'CHEF', 'Chef', 'Prepares kitchen orders', b'1'),
  (NULL, 'KITCHEN_HELPER', 'Kitchen Helper', 'Kitchen support and restocking tasks', b'1'),
  (NULL, 'WAITER', 'Waiter / Service Staff', 'Serves assigned tables and service tasks', b'1'),
  (NULL, 'CASHIER', 'Cashier', 'Billing and payment workflows', b'1');

-- ---------------------------------------------------------------------------
-- 2. Task permissions
-- ---------------------------------------------------------------------------
INSERT INTO permissions (code, name, description) VALUES
  ('task.read', 'Read tasks', 'View assigned and branch tasks'),
  ('task.manage', 'Manage tasks', 'Create, reassign and cancel tasks');

-- Task permissions for operational roles
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN ('task.read', 'task.manage')
WHERE r.code IN ('TENANT_OWNER', 'TENANT_MANAGER');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN ('task.read', 'task.manage')
WHERE r.code = 'BRANCH_MANAGER';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code = 'task.read'
WHERE r.code IN ('STAFF', 'KITCHEN');

-- Staff roles: read-only task access plus what they need for their job
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p
  ON p.code IN ('menu.read', 'table.read', 'order.read', 'order.manage', 'task.read')
WHERE r.code = 'ORDER_CONFIRMATION';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p
  ON p.code IN ('menu.read', 'order.read', 'order.manage', 'task.read')
WHERE r.code = 'CHEF';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p
  ON p.code IN ('order.read', 'task.read')
WHERE r.code = 'KITCHEN_HELPER';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p
  ON p.code IN ('menu.read', 'table.read', 'order.read', 'task.read', 'notification.read')
WHERE r.code = 'WAITER';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p
  ON p.code IN ('order.read', 'payment.read', 'task.read')
WHERE r.code = 'CASHIER';

-- Branch managers coordinate staff: they may manage employee records for their scope.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p
  ON p.code = 'employee.manage'
WHERE r.code = 'BRANCH_MANAGER';

-- ---------------------------------------------------------------------------
-- 3. Employee extras: shift, duty status, invitation tokens on users
-- ---------------------------------------------------------------------------
ALTER TABLE employees
  ADD COLUMN shift_name VARCHAR(60) NULL,
  ADD COLUMN on_duty BIT(1) NOT NULL DEFAULT b'0',
  ADD COLUMN on_duty_since DATETIME(6) NULL;

ALTER TABLE users
  ADD COLUMN invite_token VARCHAR(64) NULL,
  ADD COLUMN invite_expires_at DATETIME(6) NULL;

-- TiDB cannot add a column and a unique key on it in the same ALTER statement.
ALTER TABLE users
  ADD UNIQUE KEY uk_users_invite_token (invite_token);

-- ---------------------------------------------------------------------------
-- 4. Staff tasks
-- ---------------------------------------------------------------------------
CREATE TABLE tasks (
  id BIGINT NOT NULL AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  branch_id BIGINT NOT NULL,
  title VARCHAR(160) NOT NULL,
  description VARCHAR(1000) NULL,
  task_type VARCHAR(40) NOT NULL DEFAULT 'CUSTOM_TASK',
  priority VARCHAR(12) NOT NULL DEFAULT 'NORMAL',
  status VARCHAR(20) NOT NULL DEFAULT 'ASSIGNED',
  assigned_to_employee_id BIGINT NULL,
  created_by_user_id BIGINT NULL,
  table_id BIGINT NULL,
  order_id BIGINT NULL,
  due_at DATETIME(6) NULL,
  accepted_at DATETIME(6) NULL,
  started_at DATETIME(6) NULL,
  completed_at DATETIME(6) NULL,
  cancelled_at DATETIME(6) NULL,
  instructions VARCHAR(1000) NULL,
  status_note VARCHAR(300) NULL,
  is_active BIT(1) NOT NULL DEFAULT b'1',
  is_delete BIT(1) NOT NULL DEFAULT b'0',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  created_by VARCHAR(100) NULL,
  updated_by VARCHAR(100) NULL,
  PRIMARY KEY (id),
  KEY idx_tasks_branch_status (branch_id, status, due_at),
  KEY idx_tasks_tenant_created (tenant_id, created_at),
  KEY idx_tasks_assignee (assigned_to_employee_id, status),
  KEY idx_tasks_table (table_id, status),
  CONSTRAINT fk_tasks_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
  CONSTRAINT fk_tasks_branch FOREIGN KEY (branch_id) REFERENCES branches (id),
  CONSTRAINT fk_tasks_assignee FOREIGN KEY (assigned_to_employee_id) REFERENCES employees (id),
  CONSTRAINT fk_tasks_table FOREIGN KEY (table_id) REFERENCES dining_tables (id),
  CONSTRAINT fk_tasks_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------------
-- 5. Table-wise service assignments (waiter responsible for a table)
-- ---------------------------------------------------------------------------
CREATE TABLE table_assignments (
  id BIGINT NOT NULL AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  branch_id BIGINT NOT NULL,
  table_id BIGINT NOT NULL,
  employee_id BIGINT NOT NULL,
  assigned_by_user_id BIGINT NULL,
  assigned_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  released_at DATETIME(6) NULL,
  is_active BIT(1) NOT NULL DEFAULT b'1',
  is_delete BIT(1) NOT NULL DEFAULT b'0',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  created_by VARCHAR(100) NULL,
  updated_by VARCHAR(100) NULL,
  PRIMARY KEY (id),
  KEY idx_table_assign_branch (branch_id, is_active),
  KEY idx_table_assign_table (table_id, is_active),
  KEY idx_table_assign_employee (employee_id, is_active),
  CONSTRAINT fk_table_assign_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
  CONSTRAINT fk_table_assign_branch FOREIGN KEY (branch_id) REFERENCES branches (id),
  CONSTRAINT fk_table_assign_table FOREIGN KEY (table_id) REFERENCES dining_tables (id),
  CONSTRAINT fk_table_assign_employee FOREIGN KEY (employee_id) REFERENCES employees (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
