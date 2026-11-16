-- Insert default roles (only when missing: every script runs on every startup)
LET role_user = SELECT FROM Role WHERE id = 'role_user';
IF ($role_user.size() = 0) {
INSERT INTO Role SET
    id = 'role_user',
    name = 'USER',
    description = 'Standard user with basic link management permissions',
    permissions = ['CREATE_LINK', 'CREATE_COLLECTION', 'UPDATE_OWN_COLLECTION', 'DELETE_OWN_COLLECTION', 'READ_OWN_LINKS', 'UPDATE_OWN_LINKS', 'DELETE_OWN_LINKS'],
    isActive = true;
}

LET role_admin = SELECT FROM Role WHERE id = 'role_admin';
IF ($role_admin.size() = 0) {
INSERT INTO Role SET
    id = 'role_admin',
    name = 'ADMIN',
    description = 'Administrator with full system access',
    permissions = ['CREATE_LINK', 'CREATE_COLLECTION', 'UPDATE_OWN_COLLECTION', 'DELETE_OWN_COLLECTION', 'READ_ALL_LINKS', 'UPDATE_ALL_LINKS', 'DELETE_ALL_LINKS', 'MANAGE_USERS', 'MANAGE_ROLES'],
    isActive = true;
}

LET role_moderator = SELECT FROM Role WHERE id = 'role_moderator';
IF ($role_moderator.size() = 0) {
INSERT INTO Role SET
    id = 'role_moderator',
    name = 'MODERATOR',
    description = 'Moderator with limited administrative access',
    permissions = ['CREATE_LINK', 'CREATE_COLLECTION', 'UPDATE_OWN_COLLECTION', 'DELETE_OWN_COLLECTION', 'READ_ALL_LINKS', 'UPDATE_ALL_LINKS', 'DELETE_ALL_LINKS'],
    isActive = true;
}

-- Create a default system user for existing links (migration purpose)
LET systemUser = SELECT FROM User WHERE id = 'system_user';
IF ($systemUser.size() = 0) {
INSERT INTO User SET
    id = 'system_user',
    username = 'system',
    email = 'system@linklift.local',
    passwordHash = 'N/A',
    salt = 'N/A',
    createdAt = date('2024-01-01 00:00:00'),
    updatedAt = date('2024-01-01 00:00:00'),
    isActive = false,
    firstName = 'System',
    lastName = 'User';
}

-- Assign the default system user role
LET systemRole = SELECT FROM HasRole WHERE @out.id = 'system_user' AND @in.name = 'USER';
IF ($systemRole.size() = 0) {
CREATE EDGE HasRole FROM (SELECT FROM User WHERE id = 'system_user') TO (SELECT FROM Role WHERE name = 'USER')
    SET assignedAt = date('2024-01-01 00:00:00'), assignedBy = 'system';
}

-- Additional performance indexes
CREATE INDEX IF NOT EXISTS ON User(createdAt) NOTUNIQUE;
CREATE INDEX IF NOT EXISTS ON User(lastLoginAt) NOTUNIQUE;
CREATE INDEX IF NOT EXISTS ON AuthToken(createdAt) NOTUNIQUE;
CREATE INDEX IF NOT EXISTS ON AuthToken(userId, tokenType, expiresAt, isRevoked) NOTUNIQUE    ;
