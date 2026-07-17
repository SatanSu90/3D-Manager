# 3D Manager - 三维数字孪生低代码平台

提供从数据接入、数据分析、三维模型管理、场景搭建到大屏展示的全链路数字孪生应用构建能力。

## 技术栈

### 后端
- Spring Boot 3.2.5 + JPA + Spring Security + JWT
- MySQL 8.0 + Flyway（数据库迁移管理）
- MinIO（对象存储，用于3D模型文件）
- Apache Commons Math 3.6.1（统计分析）

### 前端
- Vue 3.5 + TypeScript + Vite 5
- Element Plus + Tailwind CSS
- Three.js（三维渲染引擎）
- Pinia（状态管理）+ Vue Router

### 部署
- Docker Compose（MySQL + MinIO）

## 功能模块

### 已实现

| 模块 | 功能 |
|------|------|
| **用户认证** | JWT登录/注册、Token刷新、用户禁用拦截 |
| **模型管理** | GLB/GLTF模型上传、预览、分类、标签管理 |
| **场景管理** | 场景CRUD、搜索、复制、状态管理、预览 |
| **场景编辑器** | Three.js三维编辑、模型拖拽、场景配置 |
| **部门管理** | 树形组织架构、用户分配 |
| **数据源管理** | 多数据库连接(MySQL/PostgreSQL/Oracle/SQLServer/DM)、连接测试、表结构浏览、数据预览、SQL查询 |
| **数据分析** | 数据清洗、数据过滤、基础统计、高级统计(相关性/分组聚合/交叉分析)、分析结果保存为指标 |
| **指标管理** | 指标CRUD、分类管理、供可视化应用绑定 |
| **系统管理** | 用户管理、部门管理 |

### 规划中
- 机器学习分析（线性回归/K-Means/决策树）
- 资源库（图片/动效/边框等视觉资源）
- 报表管理与模板管理
- 角色权限、系统配置、操作日志

## 项目结构

```
3D-Manager/
├── 3d-manager-backend/          # 后端
│   ├── src/main/java/com/manager3d/
│   │   ├── config/              # 配置类(MinIO/Security)
│   │   ├── controller/          # REST API控制器
│   │   ├── dto/                 # 数据传输对象
│   │   │   ├── request/
│   │   │   └── response/
│   │   ├── entity/              # JPA实体
│   │   ├── repository/          # JPA Repository
│   │   ├── security/            # JWT认证/授权
│   │   ├── service/             # 业务逻辑层
│   │   └── util/                # 工具类(AES加密等)
│   └── src/main/resources/
│       ├── application.yml      # 应用配置
│       └── db/migration/        # Flyway迁移脚本(V1-V4)
│
├── 3d-manager-frontend/         # 前端
│   ├── src/
│   │   ├── api/                 # API请求封装
│   │   ├── components/          # 公共组件(layout/model/scene)
│   │   ├── config/              # 菜单配置
│   │   ├── router/              # 路由配置
│   │   ├── stores/              # Pinia状态管理
│   │   ├── types/               # TypeScript类型定义
│   │   ├── views/               # 页面视图
│   │   └── utils/               # 工具函数
│   └── vite.config.ts
│
└── docker-compose.yml           # MySQL + MinIO
```

## 快速开始

### 环境要求

- JDK 17+（推荐 JDK 22）
- Maven 3.6+
- Node.js 18+
- MySQL 8.0
- MinIO（可选，不影响基础功能）

### 1. 启动数据库

使用 Docker Compose：
```bash
docker-compose up -d
```

或使用本地 MySQL（需修改 `application.yml` 中的连接配置）。

### 2. 启动后端

```bash
cd 3d-manager-backend
mvn spring-boot:run
```

后端启动在 `http://localhost:8080`，Flyway 会自动执行数据库迁移。

### 3. 启动前端

```bash
cd 3d-manager-frontend
npm install
npm run dev
```

前端启动在 `http://localhost:5173`，API 请求自动代理到后端。

### 4. 访问系统

浏览器打开 `http://localhost:5173`，使用默认管理员账号登录。

## 数据库迁移

| 版本 | 说明 |
|------|------|
| V1 | 基础表：user, category, tag, model, model_tag, scene |
| V2 | 角色字段扩展 |
| V3 | 用户扩展/部门/数据源/场景增强 |
| V4 | 分析任务/分析步骤/分析数据源关联/指标 |

## API 概览

| 模块 | 前缀 | 说明 |
|------|------|------|
| 认证 | `/api/auth` | 登录、注册、Token刷新 |
| 用户 | `/api/users` | 用户信息、状态管理 |
| 模型 | `/api/models` | 模型CRUD、上传 |
| 场景 | `/api/scenes` | 场景CRUD、搜索、复制 |
| 数据源 | `/api/datasources` | 数据源CRUD、连接测试、表浏览、数据预览 |
| 部门 | `/api/departments` | 部门CRUD、用户分配 |
| 分析 | `/api/analysis` | 分析任务、步骤、执行、结果 |
| 指标 | `/api/indicators` | 指标CRUD |

## License

Private
