-- =====================================================
-- V3: 基础能力扩展 + 数据接入管理
-- =====================================================

-- === 1. User 表扩展 ===
ALTER TABLE `user` ADD COLUMN `email` VARCHAR(100) AFTER `avatar`;
ALTER TABLE `user` ADD COLUMN `phone` VARCHAR(20) AFTER `email`;
ALTER TABLE `user` ADD COLUMN `status` ENUM('ENABLED','DISABLED') NOT NULL DEFAULT 'ENABLED' AFTER `role`;
ALTER TABLE `user` ADD COLUMN `last_login_at` DATETIME AFTER `status`;
ALTER TABLE `user` ADD INDEX `idx_status` (`status`);

-- === 2. Scene 表扩展 ===
ALTER TABLE `scene` ADD COLUMN `description` TEXT AFTER `name`;
ALTER TABLE `scene` ADD COLUMN `category_id` BIGINT AFTER `description`;
ALTER TABLE `scene` ADD COLUMN `owner_id` BIGINT AFTER `category_id`;
ALTER TABLE `scene` ADD COLUMN `visibility` ENUM('PRIVATE','DEPARTMENT_SHARED','PUBLIC') NOT NULL DEFAULT 'PRIVATE' AFTER `owner_id`;
ALTER TABLE `scene` ADD COLUMN `preview_password` VARCHAR(200) AFTER `visibility`;
ALTER TABLE `scene` ADD COLUMN `resolution` VARCHAR(20) DEFAULT '1920x1080' AFTER `preview_password`;
ALTER TABLE `scene` ADD COLUMN `status` ENUM('DRAFT','PUBLISHED','ARCHIVED') NOT NULL DEFAULT 'DRAFT' AFTER `resolution`;
ALTER TABLE `scene` ADD INDEX `idx_scene_category_id` (`category_id`);
ALTER TABLE `scene` ADD INDEX `idx_scene_status` (`status`);
ALTER TABLE `scene` ADD INDEX `idx_scene_owner_id` (`owner_id`);
-- 迁移已有场景的 owner_id 为 creator_id
UPDATE `scene` SET `owner_id` = `creator_id` WHERE `owner_id` IS NULL;

-- === 3. Department 表 ===
CREATE TABLE IF NOT EXISTS `department` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL,
    `parent_id` BIGINT,
    `sort_order` INT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_dept_parent_id` (`parent_id`),
    CONSTRAINT `fk_dept_parent` FOREIGN KEY (`parent_id`) REFERENCES `department`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- === 4. User-Department 关联表 ===
CREATE TABLE IF NOT EXISTS `user_department` (
    `user_id` BIGINT NOT NULL,
    `department_id` BIGINT NOT NULL,
    PRIMARY KEY (`user_id`, `department_id`),
    INDEX `idx_ud_dept` (`department_id`),
    CONSTRAINT `fk_ud_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_ud_dept` FOREIGN KEY (`department_id`) REFERENCES `department`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- === 5. Data Source 表 ===
CREATE TABLE IF NOT EXISTS `data_source` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(200) NOT NULL,
    `type` VARCHAR(50) NOT NULL COMMENT 'MYSQL/POSTGRESQL/ORACLE/SQLSERVER/DM',
    `host` VARCHAR(200) NOT NULL,
    `port` INT NOT NULL,
    `database_name` VARCHAR(200),
    `username` VARCHAR(100) NOT NULL,
    `password` VARCHAR(500) NOT NULL COMMENT 'AES加密存储',
    `pool_config` TEXT COMMENT 'JSON: 连接池配置',
    `ssl_enabled` BOOLEAN DEFAULT FALSE,
    `status` VARCHAR(20) DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE/ERROR',
    `owner_id` BIGINT NOT NULL,
    `visibility` ENUM('PRIVATE','DEPARTMENT_SHARED','PUBLIC') NOT NULL DEFAULT 'PRIVATE',
    `last_test_time` DATETIME,
    `last_test_result` BOOLEAN,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_ds_owner` (`owner_id`),
    INDEX `idx_ds_type` (`type`),
    CONSTRAINT `fk_ds_owner` FOREIGN KEY (`owner_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- === 初始部门数据 ===
INSERT INTO `department` (`name`, `parent_id`, `sort_order`) VALUES ('总公司', NULL, 1);
