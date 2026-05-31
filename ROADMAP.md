# ROADMAP — 幻境画廊 AI 绘图平台

## v1.0 （当前版本）

- [x] ComfyUI 文生图核心流程（提交 → WebSocket 推送 → 前端展示）
- [x] 多模型选择（PCA / Anima，工作流 JSON 配置化）
- [x] JWT 认证 + BCrypt + Refresh Token 轮换
- [x] 画廊社交（点赞、评论、热搜排行榜、关键词/模型筛选）
- [x] 关注/粉丝体系
- [x] 用户个人主页（本地 IndexedDB + 后端共享）
- [x] 分布式会话管理（JWT Redis 黑名单）
- [x] 积分系统（签到、消耗、退款流水）
- [x] 频率限制 + 缓存加速
- [x] 移动端响应式布局

---

## v1.1 — 高级绘图能力

### 服务抽象层重构
> **所有后续功能的基础**，建议最先实施

- [ ] 抽离 `AiDrawingService` 接口（`queuePrompt / cancel / getStatus / getResult`）
- [ ] `ComfyUIService` 适配接口（当前实现迁移到接口下）
- [ ] `DrawingTaskConsumer` 中的硬编码调用切换为服务路由
- [ ] `DrawingRequest` 增加 `taskType` 字段（`TEXT_TO_IMAGE / IMAGE_TO_IMAGE / INPAINTING / CONTROLNET / EDIT`）
- [ ] `DrawingRequest` 增加 `inputImage` / `maskImage` / `controlNetParams` 字段

### 图生图（img2img）
- [ ] ComfyUI 工作流 JSON（**需提供**）
- [ ] 前端 Studio 模式切换（文生图 / 图生图 / 局部重绘 / ControlNet）
- [ ] 图片上传组件（拖拽 + 点击）

### 局部重绘（Inpainting）
- [ ] ComfyUI 工作流 JSON（**需提供**）
- [ ] 前端 Canvas 画刷遮罩绘制

### ControlNet
- [ ] ComfyUI 工作流 JSON（**需提供**）
- [ ] 前端 ControlNet 参数面板（预处理器选择、权重、起止步数）

---

## v1.2 — LLM 提示词助手

> 独立可交付，无外部依赖阻塞

- [ ] 后端 `POST /api/v1/llm/assist` 端点
  - 请求：`{ prompt, instruction, model }`
  - 响应：`{ enhancedPrompt, suggestions[] }`
  - 支持指令：扩写、翻译、风格化、润色
- [ ] 配置：OpenAI 兼容格式，支持任意 LLM API
- [ ] 前端 Studio 右下角悬浮气泡入口
  - 聊天式面板（类似 ChatGPT 浮窗）
  - 两个 tab：提示词助手 + 快速模板
  - 输出自动填充到 prompt 输入框

---

## v1.3 — 图片编辑界面

> 新增完整功能视图，工程量最大

### 前端 Editor.vue（路由 `/editor`）
- [ ] 中间画布区域：上传图片 + 编辑预览 + 导出
- [ ] 左侧参数面板：模式选择 / 图片上传 / 遮罩绘制 / 编辑指令输入
- [ ] 右侧历史记录：编辑版本对比（缩略图 + 参数）
- [ ] 导航栏增加"图片编辑"入口

### 后端编辑服务
- [ ] 新增 `ImageEditService` 接口
  - `POST /api/v1/edit/submit` — 提交编辑任务
  - `GET /api/v1/edit/{id}/status` — 轮询状态
  - `GET /api/v1/edit/{id}/result` — 获取结果
- [ ] WebSocket 频道 `/topic/edit_complete` 推送编辑完成

### 后台接入
| 平台 | 类型 | 典型模型 |
|------|------|----------|
| ComfyUI（自定义工作流） | 自托管 | Qwen-image-edit 等开源模型 |
| Nanobanana | 外部 API | 闭源图像编辑模型 |
| GPT-Image2 | 外部 API | 闭源图像编辑模型 |

- [ ] 每种平台一个 Adapter 类，实现 `ImageEditService`
- [ ] 模型注册表扩展类型：`TEXT_TO_IMAGE` / `IMAGE_TO_IMAGE` / `EDIT`

---

## 远期规划（v2.0+）

- [ ] 用户收藏夹（私人画廊）
- [ ] 批量生成任务队列
- [ ] 图片压缩/水印/EXIF 处理
- [ ] 管理后台（用户管理、作品审核）
- [ ] 国际化（i18n）
- [ ] PWA 离线支持
