# 3D Manager - 三维数字孪生低代码平台

提供从数据接入、数据分析、三维模型管理、场景搭建到大屏预览的数字孪生应用构建能力。

> 功能状态基于当前代码库（2026-07-18）整理。标记为“已实现”的功能表示已有前后端页面、API或核心服务；标记为“部分实现”的功能仍缺少完整业务闭环或生产级能力。

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
| **用户认证** | JWT 登录/注册、Token 刷新、当前用户信息、BCrypt 密码、禁用用户拦截 |
| **用户与组织** | 用户信息与状态管理、部门树、用户分配、分类管理、标签管理 |
| **模型库** | GLB/GLTF 上传、MinIO 存储、缩略图、在线预览、分页搜索、分类/标签、下载、编辑和删除 |
| **场景管理** | 场景 CRUD、分页搜索、复制、草稿/发布/归档状态、可见性配置、预览密码校验、预览和 GLB 导出接口 |
| **三维编辑器** | Three.js 场景编辑、模型/基础几何体/灯光、平移/旋转/缩放、材质、背景/雾效、场景保存与加载 |
| **编辑器动画与交互** | 旋转/浮动/脉冲/颜色变化动画、点击高亮、显示切换、数据弹窗、链接跳转 |
| **图表与指标绑定** | ECharts 柱状图/折线图/饼图/仪表盘、静态数据、指标绑定、指标值覆盖层与定时刷新 |
| **部门管理** | 多级部门树、部门 CRUD、部门成员查询、批量分配和移除用户 |
| **数据源管理** | MySQL/PostgreSQL/Oracle/SQL Server/达梦连接、连接测试、表/字段浏览、数据预览、只读 SQL 查询、密码 AES 加密 |
| **数据分析** | 任务与步骤管理、数据清洗、过滤、基础统计、相关性分析、分组聚合、交叉表、线性回归、K-Means、决策树、结果查看和保存为指标 |
| **指标管理** | 指标 CRUD、分类/标签/共享字段管理、按 ID 查询和场景绑定 |
| **资源库基础能力** | 图片/图标/纹理/材质/音频/视频资源上传、MinIO 存储、标签、搜索、预览/下载、单个和批量删除 |
| **报表基础管理** | 报表 CRUD、类型/状态筛选、复制、草稿/发布/归档状态、从场景生成报表配置 |
| **模板管理** | 场景/报表/大屏模板 CRUD、分类筛选、官方标记、模板应用和使用次数统计 |
| **系统管理** | 角色 CRUD、角色分配、权限 JSON 保存、系统配置单项/批量更新、操作日志查询和清理 |

### 部分实现

| 模块 | 当前状态 | 尚未完成 |
|------|----------|----------|
| **角色权限** | 已有角色 CRUD、角色分配和权限数据保存 | 权限码尚未统一接入所有后端接口；目前主要依赖登录鉴权、管理员路由和业务层的部分所有权校验 |
| **共享与可见性** | 场景、报表等对象已有私有/部门共享/公开字段 | 各类资源的部门共享、全员共享和数据范围过滤尚未形成统一权限闭环 |
| **资源库** | 已支持基础上传、标签、搜索、下载和删除 | 引用跟踪、删除保护、内置资源包、资源编辑和完整视觉资源预览尚未完成 |
| **报表** | 已支持配置 JSON 的 CRUD、复制、状态管理和从场景生成 | 拖拽式报表设计器、动态数据渲染、PDF/Excel 导出、打印、订阅推送尚未完成 |
| **模板** | 已支持模板配置保存和应用 | 应用模板目前返回配置并由前端复制/继续创建，不是完整的一键实例化流程 |
| **数据刷新** | 场景预览按 30 秒轮询指标接口 | 尚未接入 WebSocket/实时数据推送、复杂联动刷新和数据变化触发策略 |
| **场景导出** | 已提供编辑器端 GLB 导出和后端导出接口 | 复杂场景配置、交互、指标绑定和非模型组件的完整导出兼容性仍需完善 |

### 未实现

- API、CSV/Excel/JSON/Word/视频文件等非数据库数据源接入。
- WebSocket/RTSP/RTMP/HLS 等实时数据或媒体流接入。
- 三维模型 Draco/KTX2 压缩、LOD 自动生成、异步压缩队列、IFC/3D Tiles/LAS/LAZ/PCD 等格式支持。
- 地图组件、2D 组态、3D 组态、组态图库、自定义 SVG/脚本组件导入和远程控制指令。
- 报表在线设计、真实数据渲染、PDF/Excel 导出和消息/邮件订阅。
- 全局资源/模型/指标/报表引用关系、引用阻断删除和影响范围可视化。
- SSO（SAML/OAuth2/CAS）、OTP/验证码、角色继承和细粒度模块/操作/数据权限。
- 数据脱敏规则配置及从数据源到分析、指标、报表、场景的全链路继承。
- 生产级备份恢复、灰度发布、实时监控告警和完整移动端只读适配。

## 项目结构

```
3D-Manager/
├── 3d-manager-backend/          # 后端
│   ├── src/main/java/com/manager3d/
│   │   ├── aspect/              # 操作日志切面
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
│       └── db/migration/        # Flyway迁移脚本(V1-V6)
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
| V4 | 分析任务、分析步骤、分析数据源关联、指标 |
| V5 | 系统角色、系统配置、操作日志 |
| V6 | 资源库、报表、模板 |

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
| 资源 | `/api/resources` | 资源上传、搜索、下载、单个/批量删除 |
| 报表 | `/api/reports` | 报表 CRUD、复制、状态更新、从场景生成 |
| 模板 | `/api/templates` | 模板 CRUD、搜索、应用 |
| 角色 | `/api/roles` | 角色 CRUD、用户分配 |
| 系统配置 | `/api/system/config` | 配置查询、单项/批量更新 |
| 操作日志 | `/api/system/logs` | 日志查询、按保留天数清理 |

## 当前已知限制

- 后端构建要求 JDK 17+；前端构建使用 `vue-tsc -b && vite build`。
- 运行数据源、模型和资源相关功能需要正确配置 MySQL 与 MinIO；本地敏感配置使用未纳入 Git 的 `application-local.yml`。
- 当前前端生产构建已通过；Vite 仍会提示 Three.js 与 Element Plus 分包体积较大，后续可继续做性能优化。
- README 只描述代码中已有的能力，不代表 PRD 中所有规划能力均已交付。

## License

Private
