-- =====================================================
-- V4: 数据分析引擎 + 指标管理
-- =====================================================

-- === 1. 分析任务表 ===
CREATE TABLE IF NOT EXISTS analysis_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    status ENUM('DRAFT','RUNNING','SUCCESS','FAILED') NOT NULL DEFAULT 'DRAFT',
    owner_id BIGINT NOT NULL,
    config TEXT COMMENT 'JSON: 任务全局配置',
    result_summary TEXT COMMENT 'JSON: 执行结果摘要',
    error_message TEXT,
    executed_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_task_owner (owner_id),
    INDEX idx_task_status (status),
    CONSTRAINT fk_task_owner FOREIGN KEY (owner_id) REFERENCES user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- === 2. 分析步骤表 ===
CREATE TABLE IF NOT EXISTS analysis_step (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    step_order INT NOT NULL,
    step_type ENUM('CLEAN','FILTER','STATS','ADVANCED_STATS','ML') NOT NULL,
    step_name VARCHAR(200) NOT NULL,
    config TEXT NOT NULL COMMENT 'JSON: 步骤配置',
    result TEXT COMMENT 'JSON: 步骤执行结果',
    status ENUM('PENDING','SUCCESS','FAILED') NOT NULL DEFAULT 'PENDING',
    INDEX idx_step_task (task_id),
    CONSTRAINT fk_step_task FOREIGN KEY (task_id) REFERENCES analysis_task(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- === 3. 分析数据源关联表 ===
CREATE TABLE IF NOT EXISTS analysis_data_source (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    data_source_id BIGINT NOT NULL,
    alias VARCHAR(100) NOT NULL,
    table_name VARCHAR(200),
    query_sql TEXT,
    INDEX idx_ads_task (task_id),
    CONSTRAINT fk_ads_task FOREIGN KEY (task_id) REFERENCES analysis_task(id) ON DELETE CASCADE,
    CONSTRAINT fk_ads_ds FOREIGN KEY (data_source_id) REFERENCES data_source(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- === 4. 指标表 ===
CREATE TABLE IF NOT EXISTS indicator (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    type ENUM('ATOMIC','DERIVED','COMPOSITE') NOT NULL DEFAULT 'ATOMIC',
    value_type ENUM('NUMBER','STRING','JSON','TABLE') NOT NULL DEFAULT 'NUMBER',
    value TEXT COMMENT '指标值(JSON)',
    data_source_id BIGINT,
    task_id BIGINT,
    owner_id BIGINT NOT NULL,
    tags VARCHAR(500),
    visibility ENUM('PRIVATE','DEPARTMENT_SHARED','PUBLIC') NOT NULL DEFAULT 'PRIVATE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_indicator_owner (owner_id),
    INDEX idx_indicator_type (type),
    CONSTRAINT fk_indicator_owner FOREIGN KEY (owner_id) REFERENCES user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
