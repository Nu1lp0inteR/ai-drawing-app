# **幻境画廊 (Illusion Gallery) \- AI 艺术创作与分享平台**

欢迎来到“幻境画廊”项目！这是一个功能完备的全栈Web应用，旨在为用户提供一个稳定、高效的AI艺术创作工具，并构建一个充满活力的社区分享平台。

## **✨ 项目特色 (Features)**

* **🎨 强大的创作能力**: 提供精细化的参数控制，与强大的 [ComfyUI](https://github.com/comfyanonymous/ComfyUI) 引擎深度集成。  
* **⚡ 异步任务处理**: 基于 **RabbitMQ** 消息队列，实现任务的异步提交与处理，用户无需漫长等待。  
* **🚀 实时结果通知**: 基于 **WebSocket**，AI生成任务完成后，前端可立即收到实时通知并展示结果。  
* **🔥 高性能缓存**: 引入 **Redis** 作为核心缓存层，极大提升画廊等热点数据的访问速度。  
* **🔐 安全的用户认证**: 使用 **Spring Security** 和 **JWT**，保障用户账户与数据的安全。  
* **🖼️ 社区画廊**: 用户可以一键将满意的作品分享到公共画廊，激发社区创作灵感。

## **🏛️ 技术架构 (Architecture)**

本项目采用业界主流的 **前后端分离** 架构，并通过消息队列和缓存实现服务解耦与性能优化。

* **前端**: 基于 Vue 3 \+ Vite \+ Element Plus 构建响应式的用户界面。  
* **后端**: 基于 Spring Boot 3 构建健壮、可扩展的RESTful API和业务逻辑。  
* **数据库**: 使用 MySQL 8 进行核心业务数据的持久化存储。  
* **消息队列**: 使用 RabbitMQ 管理异步的AI绘画任务。  
* **缓存**: 使用 Redis 缓存热点数据，提升系统性能。  
* **容器化**: 所有服务均可通过 Docker 和 docker-compose 进行一键部署和管理。

*您可以参考项目文档中的 [概要设计文档](https://www.google.com/search?q=ai-drawing-project-docs/project_high_level_design_v1.1_full.md) 获取更详细的架构图和流程说明。*

## **🚀 快速开始 (Getting Started)**

### **环境依赖**

* [Git](https://git-scm.com/)  
* [Java 17+](https://www.oracle.com/java/technologies/downloads/) (推荐使用SDKMAN进行管理)  
* [Node.js 18+](https://nodejs.org/) (推荐使用nvm进行管理)  
* [Docker](https://www.docker.com/products/docker-desktop/) & Docker Compose  
* 本地运行的 [ComfyUI](https://github.com/comfyanonymous/ComfyUI) 实例，并确保已通过 \--listen \--enable-cors 参数启动。

### **本地开发运行**

1. **克隆仓库并配置环境变量**
   ```bash
   git clone https://github.com/Nu1lp0inteR/ai-drawing-app.git
   cd ai-drawing-app
   cp .env.example .env
   # 编辑 .env 文件，填入您的 MySQL 密码、JWT 密钥和 ComfyUI 地址
   ```

2. **启动基础设施（MySQL, Redis, RabbitMQ）**
   ```bash
   export $(cat .env | xargs) && docker-compose up -d
   ```

3. **启动后端**
   ```bash
   cd backend && export $(cat ../.env | xargs) && ./mvnw spring-boot:run
   ```

4. **启动前端**
   ```bash
   cd frontend && npm install && npm run dev
   ```

5. **访问应用**
   * 打开浏览器，访问 http://localhost:5173

## **🗺️ 项目蓝图 (Roadmap)**

我们有一个清晰的迭代计划，旨在不断完善平台功能和用户体验。

* **Phase 1**: 性能优化与体验完善 (画廊详情、个人历史、Redis缓存)。  
* **Phase 2**: 社区化与服务治理 (API速率限制、点赞排行、模型选择)。  
* **Phase 3**: 长期演进与商业化探索 (高级绘图功能、积分系统)。

*您可以参考项目文档中的 [开发蓝图](https://www.google.com/search?q=ai-drawing-project-docs/project_roadmap_v1.1_full.md) 获取详细的开发计划。*