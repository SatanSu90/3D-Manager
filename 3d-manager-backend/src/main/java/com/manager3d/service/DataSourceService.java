package com.manager3d.service;

import com.manager3d.dto.request.DataSourceCreateRequest;
import com.manager3d.dto.request.DataSourceTestRequest;
import com.manager3d.dto.response.DataPreviewResponse;
import com.manager3d.dto.response.DataSourceResponse;
import com.manager3d.entity.DataSource;
import com.manager3d.entity.User;
import com.manager3d.repository.DataSourceRepository;
import com.manager3d.repository.UserRepository;
import com.manager3d.util.AesEncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataSourceService {

    private final DataSourceRepository dataSourceRepository;
    private final UserRepository userRepository;
    private final AesEncryptionUtil encryptionUtil;

    // ========== CRUD ==========

    @Transactional(readOnly = true)
    public Page<DataSourceResponse> getDataSources(String keyword, String type, Long ownerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        DataSource.Type typeEnum = type != null ? DataSource.Type.valueOf(type) : null;
        Page<DataSource> dsPage = dataSourceRepository.searchDataSources(keyword, typeEnum, ownerId, pageable);
        return dsPage.map(DataSourceResponse::from);
    }

    @Transactional(readOnly = true)
    public DataSourceResponse getDataSource(Long id) {
        DataSource ds = dataSourceRepository.findByIdWithOwner(id)
                .orElseThrow(() -> new RuntimeException("数据源不存在"));
        return DataSourceResponse.from(ds);
    }

    @Transactional
    public DataSourceResponse createDataSource(DataSourceCreateRequest request, String username) {
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        DataSource ds = DataSource.builder()
                .name(request.getName())
                .type(DataSource.Type.valueOf(request.getType()))
                .host(request.getHost())
                .port(request.getPort())
                .databaseName(request.getDatabaseName())
                .username(request.getUsername())
                .password(encryptionUtil.encrypt(request.getPassword()))
                .poolConfig(request.getPoolConfig())
                .sslEnabled(request.getSslEnabled() != null ? request.getSslEnabled() : false)
                .visibility(request.getVisibility() != null ?
                        DataSource.Visibility.valueOf(request.getVisibility()) : DataSource.Visibility.PRIVATE)
                .owner(owner)
                .status(DataSource.Status.ACTIVE)
                .build();

        ds = dataSourceRepository.save(ds);
        return DataSourceResponse.from(ds);
    }

    @Transactional
    public DataSourceResponse updateDataSource(Long id, DataSourceCreateRequest request) {
        DataSource ds = dataSourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("数据源不存在"));

        if (request.getName() != null) ds.setName(request.getName());
        if (request.getType() != null) ds.setType(DataSource.Type.valueOf(request.getType()));
        if (request.getHost() != null) ds.setHost(request.getHost());
        if (request.getPort() != null) ds.setPort(request.getPort());
        if (request.getDatabaseName() != null) ds.setDatabaseName(request.getDatabaseName());
        if (request.getUsername() != null) ds.setUsername(request.getUsername());
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            ds.setPassword(encryptionUtil.encrypt(request.getPassword()));
        }
        if (request.getPoolConfig() != null) ds.setPoolConfig(request.getPoolConfig());
        if (request.getSslEnabled() != null) ds.setSslEnabled(request.getSslEnabled());
        if (request.getVisibility() != null) {
            ds.setVisibility(DataSource.Visibility.valueOf(request.getVisibility()));
        }

        ds = dataSourceRepository.save(ds);
        return DataSourceResponse.from(ds);
    }

    @Transactional
    public void deleteDataSource(Long id) {
        if (!dataSourceRepository.existsById(id)) {
            throw new RuntimeException("数据源不存在");
        }
        dataSourceRepository.deleteById(id);
    }

    // ========== 连接测试 ==========

    @Transactional
    public Map<String, Object> testConnection(Long dataSourceId) {
        DataSource ds = dataSourceRepository.findByIdWithOwner(dataSourceId)
                .orElseThrow(() -> new RuntimeException("数据源不存在"));
        String decryptedPassword = encryptionUtil.decrypt(ds.getPassword());
        return doTestConnection(ds.getType(), ds.getHost(), ds.getPort(),
                ds.getDatabaseName(), ds.getUsername(), decryptedPassword, ds.getSslEnabled(), ds);
    }

    public Map<String, Object> testConnectionDirect(DataSourceTestRequest request) {
        DataSource.Type type = DataSource.Type.valueOf(request.getType());
        return doTestConnection(type, request.getHost(), request.getPort(),
                request.getDatabaseName(), request.getUsername(), request.getPassword(),
                request.getSslEnabled(), null);
    }

    private Map<String, Object> doTestConnection(DataSource.Type type, String host, int port,
                                                  String database, String username, String password,
                                                  Boolean sslEnabled, DataSource dsEntity) {
        String url = buildJdbcUrl(type, host, port, database, sslEnabled);
        Map<String, Object> result = new HashMap<>();

        try {
            ensureDriverLoaded(type);
        } catch (ClassNotFoundException e) {
            result.put("success", false);
            result.put("message", "数据库驱动未安装: " + e.getMessage());
            updateTestResult(dsEntity, false);
            return result;
        }

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            boolean valid = conn.isValid(10);
            result.put("success", valid);
            result.put("message", valid ? "连接成功" : "连接失败");
            result.put("databaseProductName", conn.getMetaData().getDatabaseProductName());
            result.put("databaseProductVersion", conn.getMetaData().getDatabaseProductVersion());
            if (dsEntity != null) {
                updateTestResult(dsEntity, valid);
            }
        } catch (SQLException e) {
            result.put("success", false);
            result.put("message", "连接失败: " + e.getMessage());
            updateTestResult(dsEntity, false);
            log.error("数据源连接测试失败: {}", e.getMessage());
        }
        return result;
    }

    private void updateTestResult(DataSource ds, boolean success) {
        if (ds == null) return;
        ds.setLastTestTime(LocalDateTime.now());
        ds.setLastTestResult(success);
        ds.setStatus(success ? DataSource.Status.ACTIVE : DataSource.Status.ERROR);
        dataSourceRepository.save(ds);
    }

    // ========== 表结构发现 ==========

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTables(Long dataSourceId) {
        DataSource ds = getDataSourceEntity(dataSourceId);
        String password = encryptionUtil.decrypt(ds.getPassword());
        String url = buildJdbcUrl(ds.getType(), ds.getHost(), ds.getPort(),
                ds.getDatabaseName(), ds.getSslEnabled());

        List<Map<String, Object>> tables = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(url, ds.getUsername(), password)) {
            DatabaseMetaData metaData = conn.getMetaData();
            String catalog = conn.getCatalog();
            String schema = null;

            // PostgreSQL/Oracle 使用 schema 而非 catalog
            if (ds.getType() == DataSource.Type.POSTGRESQL || ds.getType() == DataSource.Type.ORACLE) {
                schema = ds.getUsername().toUpperCase();
            }

            try (ResultSet rs = metaData.getTables(catalog, schema, "%", new String[]{"TABLE", "VIEW"})) {
                while (rs.next()) {
                    Map<String, Object> table = new LinkedHashMap<>();
                    table.put("name", rs.getString("TABLE_NAME"));
                    table.put("type", rs.getString("TABLE_TYPE"));
                    table.put("remarks", rs.getString("REMARKS"));
                    tables.add(table);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("获取表列表失败: " + e.getMessage());
        }
        return tables;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getColumns(Long dataSourceId, String tableName) {
        DataSource ds = getDataSourceEntity(dataSourceId);
        String password = encryptionUtil.decrypt(ds.getPassword());
        String url = buildJdbcUrl(ds.getType(), ds.getHost(), ds.getPort(),
                ds.getDatabaseName(), ds.getSslEnabled());

        List<Map<String, Object>> columns = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(url, ds.getUsername(), password)) {
            DatabaseMetaData metaData = conn.getMetaData();
            String catalog = conn.getCatalog();
            String schema = null;

            if (ds.getType() == DataSource.Type.POSTGRESQL || ds.getType() == DataSource.Type.ORACLE) {
                schema = ds.getUsername().toUpperCase();
            }

            try (ResultSet rs = metaData.getColumns(catalog, schema, tableName, "%")) {
                while (rs.next()) {
                    Map<String, Object> col = new LinkedHashMap<>();
                    col.put("name", rs.getString("COLUMN_NAME"));
                    col.put("type", rs.getString("TYPE_NAME"));
                    col.put("size", rs.getInt("COLUMN_SIZE"));
                    col.put("nullable", rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                    col.put("remarks", rs.getString("REMARKS"));
                    col.put("defaultValue", rs.getString("COLUMN_DEF"));
                    columns.add(col);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("获取字段列表失败: " + e.getMessage());
        }
        return columns;
    }

    // ========== 数据预览 ==========

    @Transactional(readOnly = true)
    public DataPreviewResponse previewData(Long dataSourceId, String tableName, int limit) {
        DataSource ds = getDataSourceEntity(dataSourceId);
        String password = encryptionUtil.decrypt(ds.getPassword());
        String url = buildJdbcUrl(ds.getType(), ds.getHost(), ds.getPort(),
                ds.getDatabaseName(), ds.getSslEnabled());

        if (limit <= 0 || limit > 1000) {
            limit = 100;
        }

        // 根据数据库类型使用不同的 LIMIT 语法
        String limitClause = switch (ds.getType()) {
            case MYSQL, POSTGRESQL, DM -> "LIMIT " + limit;
            case ORACLE -> "WHERE ROWNUM <= " + limit;
            case SQLSERVER -> "TOP " + limit;
        };

        String sql;
        if (ds.getType() == DataSource.Type.SQLSERVER) {
            sql = "SELECT TOP " + limit + " * FROM " + quoteIdentifier(tableName, ds.getType());
        } else if (ds.getType() == DataSource.Type.ORACLE) {
            sql = "SELECT * FROM " + quoteIdentifier(tableName, ds.getType()) + " WHERE ROWNUM <= " + limit;
        } else {
            sql = "SELECT * FROM " + quoteIdentifier(tableName, ds.getType()) + " LIMIT " + limit;
        }

        return executeQueryInternal(url, ds.getUsername(), password, sql);
    }

    // ========== SQL 查询 ==========

    @Transactional(readOnly = true)
    public DataPreviewResponse executeQuery(Long dataSourceId, String sql) {
        DataSource ds = getDataSourceEntity(dataSourceId);
        String password = encryptionUtil.decrypt(ds.getPassword());
        String url = buildJdbcUrl(ds.getType(), ds.getHost(), ds.getPort(),
                ds.getDatabaseName(), ds.getSslEnabled());

        // 安全检查：只允许 SELECT 语句
        String trimmedSql = sql.trim().toUpperCase();
        if (!trimmedSql.startsWith("SELECT") && !trimmedSql.startsWith("WITH") && !trimmedSql.startsWith("SHOW")) {
            throw new RuntimeException("仅允许执行 SELECT 查询语句");
        }

        return executeQueryInternal(url, ds.getUsername(), password, sql);
    }

    private DataPreviewResponse executeQueryInternal(String url, String username, String password, String sql) {
        List<String> columns = new ArrayList<>();
        List<Map<String, Object>> rows = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(url, username, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                columns.add(metaData.getColumnLabel(i));
            }

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    Object value = rs.getObject(i);
                    row.put(metaData.getColumnLabel(i), value);
                }
                rows.add(row);
            }
        } catch (SQLException e) {
            throw new RuntimeException("SQL执行失败: " + e.getMessage());
        }

        DataPreviewResponse response = new DataPreviewResponse();
        response.setColumns(columns);
        response.setRows(rows);
        response.setTotal(rows.size());
        return response;
    }

    // ========== 辅助方法 ==========

    private DataSource getDataSourceEntity(Long id) {
        return dataSourceRepository.findByIdWithOwner(id)
                .orElseThrow(() -> new RuntimeException("数据源不存在"));
    }

    private String buildJdbcUrl(DataSource.Type type, String host, int port, String database, Boolean sslEnabled) {
        String ssl = sslEnabled != null && sslEnabled ? "?ssl=true&sslMode=require" : "";
        return switch (type) {
            case MYSQL -> String.format("jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=%s",
                    host, port, database != null ? database : "", sslEnabled != null && sslEnabled);
            case POSTGRESQL -> String.format("jdbc:postgresql://%s:%d/%s%s", host, port, database != null ? database : "", ssl);
            case ORACLE -> String.format("jdbc:oracle:thin:@%s:%d:%s", host, port, database != null ? database : "ORCL");
            case SQLSERVER -> String.format("jdbc:sqlserver://%s:%d;databaseName=%s", host, port, database != null ? database : "");
            case DM -> String.format("jdbc:dm://%s:%d/%s", host, port, database != null ? database : "");
        };
    }

    private void ensureDriverLoaded(DataSource.Type type) throws ClassNotFoundException {
        String driverClass = switch (type) {
            case MYSQL -> "com.mysql.cj.jdbc.Driver";
            case POSTGRESQL -> "org.postgresql.Driver";
            case ORACLE -> "oracle.jdbc.OracleDriver";
            case SQLSERVER -> "com.microsoft.sqlserver.jdbc.SQLServerDriver";
            case DM -> "dm.jdbc.driver.DmDriver";
        };
        Class.forName(driverClass);
    }

    private String quoteIdentifier(String identifier, DataSource.Type type) {
        return switch (type) {
            case MYSQL -> "`" + identifier + "`";
            case POSTGRESQL, ORACLE -> "\"" + identifier + "\"";
            case SQLSERVER, DM -> "[" + identifier + "]";
        };
    }
}
