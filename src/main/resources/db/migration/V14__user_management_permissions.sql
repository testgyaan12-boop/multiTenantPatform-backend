-- User management permissions for platform super-admin directory.
INSERT INTO permissions (code, name, description) VALUES
  ('user.read', 'Read users', 'View platform users and their profiles'),
  ('user.create', 'Create users', 'Invite and onboard new platform users'),
  ('user.update', 'Update users', 'Edit user profile, contact and account details'),
  ('user.lock', 'Lock/unlock users', 'Lock or unlock user accounts'),
  ('user.reset-password', 'Reset passwords', 'Reset user passwords and issue temporary credentials'),
  ('user.assign-role', 'Assign roles', 'Change the roles assigned to a user'),
  ('user.delete', 'Delete users', 'Soft-delete users from the platform'),
  ('user.suspend', 'Suspend users', 'Suspend or reactivate user accounts'),
  ('user.activate', 'Activate users', 'Activate user accounts'),
  ('user.invite', 'Invite users', 'Send user invitations');

-- Grant every user.* permission to the SUPER_ADMIN system role.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'SUPER_ADMIN'
  AND r.tenant_id IS NULL
  AND r.is_active = 1 AND r.is_delete = 0
  AND p.code LIKE 'user.%'
  AND p.is_active = 1 AND p.is_delete = 0;
