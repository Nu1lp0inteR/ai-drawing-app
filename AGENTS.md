# AGENTS.md — 幻境画廊 AI 绘图平台

## 项目架构

- **前后端分离**：`frontend/`（Vue 3 + Vite）和 `backend/`（Spring Boot 3.5.5 / Java 17 / Maven）
- **基础设施**：MySQL 8、Redis 7、RabbitMQ — 全部通过根目录 `docker-compose up -d` 启动
- **AI 绘图引擎**：外部 ComfyUI 服务器，通过 REST API + 原生 WebSocket 对接
- **实时推送**：STOMP over SockJS，端点 `/ws`，用于向前端推送绘图完成通知
- **缓存策略**：Redis Cache-Aside 模式，用于画廊接口加速
- **认证方式**：JWT 无状态认证 + BCrypt(12) + Refresh Token 轮换
- **注意**：`backend-java/` 是残留的空壳目录（无源码），忽略即可

## 启动顺序

```bash
# 1. 启动基础设施（MySQL、Redis、RabbitMQ）
docker-compose up -d

# 2. 启动后端（端口 8080）
cd backend && ./mvnw spring-boot:run

# 3. 启动前端（端口 5173）
cd frontend && npm install && npm run dev
```

## 常用命令

| 用途 | 命令 |
|------|------|
| 后端测试 | `cd backend && ./mvnw test` |
| 前端 Lint | `cd frontend && npm run lint` |
| 前端格式化 | `cd frontend && npm run format` |
| 前端构建 | `cd frontend && npm run build` |

说明：后端仅有一个上下文加载测试（`BackendApplicationTests.contextLoads`），前端无测试框架。

## 后端注意事项

- **无数据库迁移工具**：Hibernate `ddl-auto=update` 在启动时自动创建/更新表结构。没有 Flyway 或 Liquibase。
- **JSON 命名转换**：`application.properties` 中配置了 `spring.jackson.property-naming-strategy=SNAKE_CASE`，所有接口 JSON 使用 snake_case，Jackson 自动映射到 Java 的 camelCase。
- **密钥/配置硬编码**：JWT 密钥、ComfyUI 地址、数据库/Redis/RabbitMQ 凭证均硬编码在 `application.properties` 中。
- **ComfyUI 对接方式**：使用原生 Java-WebSocket 客户端（`ComfyUIWebSocketClient.java`）轮询队列状态，队列排空后通过 REST `/history/{id}` 获取生成结果。
- **图片存储**：仅在用户主动分享到画廊时保存至 `storage/`（gitignored），通过 `GET /api/v1/images/{filename}` 对外提供。生成过程中图片不落盘、不写数据库，直接以 base64 通过 WebSocket 推送到前端。
- **浏览器端存储**：使用 IndexedDB（`frontend/src/utils/indexedDB.js`）存储未分享的个人作品。刷新页面不丢失，退出登录时清空。最大保留 50 条记录。
- **缓存 TTL**：在 `RedisConfig.java` 中定义，而非 `application.properties`。画廊缓存 5 分钟，绘图详情缓存 1 小时，默认 10 分钟。

## 前端注意事项

- **无 Vue Router**：使用 `App.vue` 中手写的 `pushState`/`popstate` 路由。路由映射：`/` 和 `/studio` → `Studio.vue`、`/gallery` → `Gallery.vue`、`/profile` → `Profile.vue`、`/user/:id` → `UserProfile.vue`、`/following` → `FollowingList.vue`、`/followers` → `FollowersList.vue`。
- **无 Pinia / Vuex**：状态管理用 `provide`/`inject` + `localStorage`。认证 Token 存储在 `localStorage` 的 `accessToken`、`refreshToken`、`userInfo` 键中。
- **无统一 API 层**：每个组件直接引入 axios 并硬编码后端地址 `http://localhost:8080`。项目中没有 `.env` 文件或后端地址配置变量。修改后端地址需要全局搜索替换。
- **`<keep-alive>` 包裹视图**：从其他页面返回时使用 `onActivated` 生命周期钩子来刷新数据。
- **组件间通信**：通过 `window.dispatchEvent` / `window.addEventListener` 实现（事件名如 `drawingCompleted`、`userLogout`、`followStatusChange`）。
- **纯 JavaScript**（无 TypeScript），ESLint v9 flat config，Prettier 配置：`semi: false`、`singleQuote: true`、`printWidth: 100`，缩进 2 空格。

## 基础设施端口

| 服务 | 端口 | 备注 |
|------|------|------|
| 后端 | 8080 | Spring Boot |
| 前端开发服务器 | 5173 | Vite |
| MySQL | 3306 | root / DB_PASSWORD_PLACEHOLDER，库名 `ai_drawing_db` |
| Redis | 6379 | 无密码 |
| RabbitMQ AMQP | 5672 | guest / guest |
| RabbitMQ 管理界面 | 15672 | Web 控制台 |
| Redis Commander | 8081 | Redis Web 管理工具 |

## AI 绘图对接架构

当前项目支持对多种绘图 API 的对接，核心流程为：前端提交任务 → `DrawingTaskService` 发布到 RabbitMQ → `DrawingTaskConsumer` 消费并调用绘图服务 → 图片 base64 通过 WebSocket 直接推送前端（不落盘、不写库）→ 前端存入 IndexedDB。

用户主动分享时，调用 `POST /api/v1/ai-drawing/share`（multipart），后端才会将图片保存至 `storage/` 并写入 `drawings` 表（`shared_to_gallery=true`）。

### 现有对接：ComfyUI（本地 / 远程服务器）

- **配置**：`application.properties` 中的 `comfyui.api.address`，指向 ComfyUI 服务器地址
- **核心服务**：`ComfyUIService.java`，通过 REST `/prompt` 提交任务、原生 WebSocket 监听队列状态、`/history/{id}` 获取结果、`/view` 下载图片
- **工作流模板**：`src/main/resources/ComfyUI_api.json`，包含高分辨率出图、FaceDetailer 等节点定义
- **支持两种部署方式**：
  - 本地 ComfyUI：`--listen --enable-cors` 启动后直接配置 `http://localhost:8188`
  - 远程 ComfyUI：通过 SSH 端口转发或 damodel 等云平台代理访问

### 扩展其他 API 平台（如 GPT-Image2、Nanobanana2）

项目架构天然支持接入多种绘图 API，添加新平台只需以下步骤：

1. **新增配置**：在 `application.properties` 中添加新平台的 API 地址和认证密钥（如 `gptimage.api.address`、`gptimage.api.key`）
2. **新建 Service**：参照 `ComfyUIService.java`，创建新的 Service 类（如 `GPTImageService.java`），实现：
   - `submitPrompt()` — 将用户参数转为目标 API 格式并提交
   - `waitForCompletion()` — 轮询或 WebSocket 等待任务完成
   - `getImage()` — 下载图片返回 `byte[]`
3. **修改 Consumer**：在 `DrawingTaskConsumer.java` 中根据请求参数或配置判断调用哪个绘图服务
4. **前端适配**（可选）：如新平台有不同参数，在 `Studio.vue` 的参数面板中添加对应控件

关键接口规范：
- 输入统一为 `DrawingRequest`（prompt、negative_prompt、steps、cfg、sampler_name、seed）
- 输出格式：WebSocket 消息包含 `image_base64` + 所有生成参数，前端直接展示并存入 IndexedDB
- 分享接口：`POST /api/v1/ai-drawing/share`，multipart 上传图片文件 + params JSON

## Git 忽略的特殊文件

## Git 忽略的特殊文件

- 根目录的 `*.sql` 文件（包括数据库迁移脚本）已被 gitignore
- `storage/` 目录已被 gitignore
- `target/`（Maven 构建输出）已被 gitignore
