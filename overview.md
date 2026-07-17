# 第一批次功能实现概述

## 完成内容

### 后端 (Spring Boot 3.2.5)

**数据库迁移** (`V3__expand_base_and_datasource.sql`)
- `user` 表扩展：email, phone, status, last_login_at
- `scene` 表扩展：description, category_id, owner_id, visibility, preview_password, resolution, status
- 新建 `department` 表（自引用树形结构）
- 新建 `user_department` 关联表
- 新建 `data_source` 表（多数据库类型支持）

**新增/修改的实体** (4个)
- `Department.java`（新建）：树形部门实体
- `DataSource.java`（新建）：数据源连接配置，支持 MySQL/PostgreSQL/Oracle/SQL Server/达梦
- `User.java`（修改）：扩展字段 + 部门多对多关联
- `Scene.java`（修改）：扩展描述/分类/可见性/状态等字段

**新增/修改的 Service** (4个)
- `DataSourceService`（新建）：CRUD + JDBC 连接测试 + 表结构发现 + 数据预览 + SQL 查询（SELECT安全检查）
- `DepartmentService`（新建）：CRUD + 递归树构建 + 用户分配/移除
- `UserService`（修改）：新增 profile 更新/状态管理/登录时间记录
- `SceneService`（修改）：新增搜索/复制/状态更新

**新增/修改的 Controller** (4个)
- `DataSourceController`（新建）：`/api/datasources` 完整 REST API
- `DepartmentController`（新建）：`/api/departments` 完整 REST API
- `UserController`（修改）：`/api/users/me` GET/PUT, `/api/users/{id}/status` PUT
- `SceneController`（修改）：搜索参数 + 复制 + 状态更新

**安全与配置**
- `JwtAuthFilter`：禁用用户 Token 拦截
- `UserDetailsServiceImpl`：用户状态映射 enabled
- `AesEncryptionUtil`（新建）：数据源密码 AES 加密
- `application.yml`：新增加密密钥配置

**编译验证**：Maven 编译通过（JDK 22），零错误

---

### 前端 (Vue 3.5 + TypeScript)

**Types + API + Store**
- `types/datasource.ts`（新建）：数据源完整类型
- `types/department.ts`（新建）：部门树形类型
- `api/datasource.ts`（新建）：数据源完整 API
- `api/department.ts`（新建）：部门完整 API
- `stores/datasource.ts`（新建）：数据源 Pinia Store
- `types/api.ts`（更新）：UserInfo 扩展字段
- `api/user.ts`（更新）：新增 profile/status API
- `api/scene.ts`（更新）：分页搜索 + 复制 + 状态更新
- `types/scene.ts`（更新）：新增 visibility/status 等字段

**新增页面** (2个)
- `DataSourceManageView.vue`：数据源管理页面（表格列表 + 创建/编辑弹窗 + 连接测试 + 表浏览器）
- `DepartmentManageView.vue`：部门管理页面（树形展示 + 创建/编辑 + 成员管理）

**路由 + 侧边栏 + 现有页面增强**
- 路由新增：`/datasources`, `/departments`(admin)
- 侧边栏新增：数据源管理、部门管理(admin) 导航
- `SceneManageView` 增强：服务端搜索、复制按钮、状态标签

**编译验证**：vue-tsc --noEmit 通过，零错误

---

## 文件清单

### 后端新建文件 (11个)
1. `db/migration/V3__expand_base_and_datasource.sql`
2. `entity/Department.java`
3. `entity/DataSource.java`
4. `util/AesEncryptionUtil.java`
5. `repository/DepartmentRepository.java`
6. `repository/DataSourceRepository.java`
7. `dto/request/DataSourceCreateRequest.java`
8. `dto/request/DataSourceTestRequest.java`
9. `dto/request/DepartmentCreateRequest.java`
10. `dto/response/DataSourceResponse.java`
11. `dto/response/DepartmentResponse.java`
12. `dto/response/DataPreviewResponse.java`
13. `service/DataSourceService.java`
14. `service/DepartmentService.java`
15. `controller/DataSourceController.java`
16. `controller/DepartmentController.java`

### 后端修改文件 (12个)
1. `entity/User.java`
2. `entity/Scene.java`
3. `repository/UserRepository.java`
4. `repository/SceneRepository.java`
5. `service/UserService.java`
6. `service/AuthService.java`
7. `service/SceneService.java`
8. `controller/UserController.java`
9. `controller/SceneController.java`
10. `controller/AuthController.java`
11. `security/JwtAuthFilter.java`
12. `security/UserDetailsServiceImpl.java`
13. `resources/application.yml`
14. `dto/request/SceneSaveRequest.java`
15. `dto/response/SceneResponse.java`
16. `dto/request/UserProfileUpdateRequest.java`
17. `dto/request/UserStatusUpdateRequest.java`

### 前端新建文件 (7个)
1. `types/datasource.ts`
2. `types/department.ts`
3. `api/datasource.ts`
4. `api/department.ts`
5. `stores/datasource.ts`
6. `views/DataSourceManageView.vue`
7. `views/DepartmentManageView.vue`

### 前端修改文件 (6个)
1. `types/api.ts`
2. `types/scene.ts`
3. `api/user.ts`
4. `api/scene.ts`
5. `router/index.ts`
6. `components/layout/AppSidebar.vue`
7. `views/SceneManageView.vue`

---

## 后续可实现的批次

| 批次 | 内容 | 模块 |
|------|------|------|
| 第二批 | 数据分析（2D图表搭建、数据绑定） | M2 |
| 第三批 | 场景编辑器增强（动画配置、组件系统） | M3-M4 |
| 第四批 | 组态交互、大屏展示 | M4 |
| 第五批 | 资源报表、增强优化 | M5-M6 |
