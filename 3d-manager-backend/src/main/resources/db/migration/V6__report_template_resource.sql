-- =====================================================
-- V6: 资源报表 — 报表管理 + 模板管理 + 资源库
-- =====================================================

-- === 1. 报表表 ===
CREATE TABLE IF NOT EXISTS report (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    type ENUM('DASHBOARD','TABLE','CHART','CUSTOM') NOT NULL DEFAULT 'DASHBOARD',
    config TEXT COMMENT 'JSON: 报表配置(布局/组件/数据源绑定)',
    scene_id BIGINT COMMENT '关联场景ID(可选)',
    status ENUM('DRAFT','PUBLISHED','ARCHIVED') NOT NULL DEFAULT 'DRAFT',
    owner_id BIGINT NOT NULL,
    visibility ENUM('PRIVATE','DEPARTMENT_SHARED','PUBLIC') NOT NULL DEFAULT 'PRIVATE',
    thumbnail_key VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_report_owner (owner_id),
    INDEX idx_report_status (status),
    INDEX idx_report_scene (scene_id),
    CONSTRAINT fk_report_owner FOREIGN KEY (owner_id) REFERENCES user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- === 2. 模板表 ===
CREATE TABLE IF NOT EXISTS template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    category ENUM('SCENE','REPORT','DASHBOARD') NOT NULL DEFAULT 'SCENE',
    config TEXT NOT NULL COMMENT 'JSON: 模板配置',
    preview_image VARCHAR(500),
    is_official TINYINT(1) DEFAULT 0 COMMENT '官方模板',
    use_count INT DEFAULT 0,
    owner_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_template_category (category),
    INDEX idx_template_owner (owner_id),
    CONSTRAINT fk_template_owner FOREIGN KEY (owner_id) REFERENCES user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- === 3. 资源库表（图片/图标/材质等视觉资源） ===
CREATE TABLE IF NOT EXISTS resource (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    type ENUM('IMAGE','ICON','TEXTURE','MATERIAL','AUDIO','VIDEO') NOT NULL DEFAULT 'IMAGE',
    file_key VARCHAR(500) NOT NULL COMMENT 'MinIO存储key',
    file_size BIGINT,
    mime_type VARCHAR(100),
    width INT,
    height INT,
    tags VARCHAR(500) COMMENT '标签(逗号分隔)',
    owner_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_resource_type (type),
    INDEX idx_resource_owner (owner_id),
    CONSTRAINT fk_resource_owner FOREIGN KEY (owner_id) REFERENCES user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
