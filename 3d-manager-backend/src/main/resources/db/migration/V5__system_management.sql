-- =====================================================
-- V5: 系统管理模块 — 角色权限 + 系统配置 + 操作日志
-- =====================================================

-- === 1. 角色表 ===
CREATE TABLE IF NOT EXISTS `role` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL UNIQUE,
    `code` VARCHAR(50) NOT NULL UNIQUE,
    `description` VARCHAR(500),
    `permissions` TEXT COMMENT 'JSON: 权限列表',
    `is_system` TINYINT(1) DEFAULT 0 COMMENT '系统内置角色不可删除',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_role_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 初始角色数据
INSERT INTO `role` (`name`, `code`, `description`, `permissions`, `is_system`) VALUES
('管理员', 'ADMIN', '系统管理员，拥有全部权限', '["*"]', 1),
('编辑者', 'EDITOR', '可管理模型、场景、数据源', '["model:*","scene:*","datasource:*","analysis:*"]', 1),
('查看者', 'VIEWER', '只读权限', '["model:read","scene:read"]', 1);

-- === 2. 用户-角色关联表 ===
CREATE TABLE IF NOT EXISTS `user_role` (
    `user_id` BIGINT NOT NULL,
    `role_id` BIGINT NOT NULL,
    PRIMARY KEY (`user_id`, `role_id`),
    INDEX `idx_ur_role` (`role_id`),
    CONSTRAINT `fk_ur_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_ur_role` FOREIGN KEY (`role_id`) REFERENCES `role`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 为现有管理员用户分配 ADMIN 角色
INSERT INTO `user_role` (`user_id`, `role_id`)
SELECT u.id, 1 FROM `user` u WHERE u.role = 'ADMIN'
AND NOT EXISTS (SELECT 1 FROM `user_role` ur WHERE ur.user_id = u.id AND ur.role_id = 1);

-- === 3. 系统配置表 ===
CREATE TABLE IF NOT EXISTS `system_config` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `config_key` VARCHAR(200) NOT NULL UNIQUE,
    `config_value` TEXT,
    `config_type` ENUM('STRING','NUMBER','BOOLEAN','JSON') NOT NULL DEFAULT 'STRING',
    `description` VARCHAR(500),
    `category` VARCHAR(100) DEFAULT 'general',
    `is_editable` TINYINT(1) DEFAULT 1,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_config_key` (`config_key`),
    INDEX `idx_config_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 初始配置项
INSERT INTO `system_config` (`config_key`, `config_value`, `config_type`, `description`, `category`) VALUES
('site_name', '3D Manager', 'STRING', '站点名称', 'general'),
('site_description', '三维数字孪生低代码平台', 'STRING', '站点描述', 'general'),
('default_scene_visibility', 'PRIVATE', 'STRING', '默认场景可见性', 'scene'),
('max_upload_size_mb', '500', 'NUMBER', '最大上传大小(MB)', 'storage'),
('session_timeout_minutes', '15', 'NUMBER', '会话超时时间(分钟)', 'security'),
('enable_registration', 'false', 'BOOLEAN', '是否允许自注册', 'security');

-- === 4. 操作日志表 ===
CREATE TABLE IF NOT EXISTS `operation_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT,
    `username` VARCHAR(100),
    `module` VARCHAR(50) NOT NULL COMMENT '模块: user/scene/model/datasource等',
    `action` VARCHAR(50) NOT NULL COMMENT '操作: create/update/delete/login等',
    `target_type` VARCHAR(50),
    `target_id` VARCHAR(100),
    `description` TEXT,
    `request_url` VARCHAR(500),
    `request_method` VARCHAR(10),
    `ip_address` VARCHAR(50),
    `status` ENUM('SUCCESS','FAILED') NOT NULL DEFAULT 'SUCCESS',
    `error_message` TEXT,
    `duration_ms` INT,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_log_user` (`user_id`),
    INDEX `idx_log_module` (`module`),
    INDEX `idx_log_action` (`action`),
    INDEX `idx_log_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
