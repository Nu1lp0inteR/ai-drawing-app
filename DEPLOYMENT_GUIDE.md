# 🚀 "幻境画廊" AI绘画平台 - 部署指南

## 📋 概述

本指南将帮助您部署"幻境画廊"AI绘画平台，包括Redis缓存、MySQL数据库、RabbitMQ消息队列等服务的配置。

## 🛠️ 系统要求

- **操作系统**: Linux/macOS/Windows (推荐Linux)
- **Docker**: 20.10+ 
- **Docker Compose**: 2.0+
- **Java**: 17+
- **Node.js**: 20.19.0+
- **内存**: 最少4GB，推荐8GB+
- **存储**: 20GB+可用空间

## 🔧 Phase 1 部署步骤

### 步骤1: 启动基础服务 (Redis, MySQL, RabbitMQ)

```bash
# 进入项目根目录
cd /home/mlchen/ai-drawing-app

# 启动所有基础服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看服务日志
docker-compose logs -f
```

**启动的服务包括：**
- **Redis** (端口6379) - 缓存服务
- **MySQL** (端口3306) - 数据库服务
- **RabbitMQ** (端口5672, 管理界面15672) - 消息队列
- **Redis Commander** (端口8081) - Redis管理界面

### 步骤2: 配置验证

#### 验证Redis连接
```bash
# 方法1：使用Redis CLI
docker exec -it ai-drawing-redis redis-cli ping
# 应该返回: PONG

# 方法2：访问Redis管理界面
# 打开浏览器访问: http://localhost:8081
```

#### 验证MySQL连接
```bash
# 连接MySQL
docker exec -it ai-drawing-mysql mysql -uroot -pDB_PASSWORD_PLACEHOLDER

# 在MySQL中执行
SHOW DATABASES;
USE ai_drawing_db;
SHOW TABLES;
```

#### 验证RabbitMQ连接
```bash
# 访问RabbitMQ管理界面
# 打开浏览器访问: http://localhost:15672
# 用户名: guest, 密码: guest
```

### 步骤3: 启动后端服务

```bash
# 进入后端目录
cd backend

# 编译项目
mvn clean compile

# 启动后端服务
mvn spring-boot:run

# 或者打包后运行
mvn clean package
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

**后端服务启动后应该看到：**
- Spring Boot启动日志
- Redis连接成功
- MySQL数据库表自动创建
- RabbitMQ连接建立

### 步骤4: 启动前端服务

```bash
# 进入前端目录
cd frontend

# 安装依赖（首次运行）
npm install

# 启动开发服务器
npm run dev

# 访问应用
# 打开浏览器访问: http://localhost:5173
```

## 🎯 Phase 1 功能验证

### 1. Redis缓存功能测试

1. **画廊缓存测试**:
   - 访问画廊页面，观察加载时间
   - 刷新页面，应该明显更快（缓存命中）
   - 在Redis Commander中查看缓存keys

2. **缓存失效测试**:
   - 分享一个新作品到画廊
   - 画廊页面应该立即显示新作品（缓存已清除）

### 2. 作品详情模态框测试

1. 点击画廊中的任意作品
2. 应该弹出详情模态框，显示：
   - 高清图片预览
   - 完整的生成参数
   - 元数据信息
   - "复用参数"按钮

### 3. 参数复用功能测试

1. 在画廊详情中点击"复用参数到创作中心"
2. 应该自动跳转到创作中心
3. 所有参数应该自动填充到表单中

## 🔍 监控和日志

### 查看应用日志

```bash
# 后端日志
tail -f backend/logs/application.log

# Docker服务日志
docker-compose logs -f redis
docker-compose logs -f mysql
docker-compose logs -f rabbitmq
```

### 性能监控

```bash
# Redis性能监控
docker exec -it ai-drawing-redis redis-cli monitor

# MySQL慢查询日志
docker exec -it ai-drawing-mysql mysql -uroot -pDB_PASSWORD_PLACEHOLDER -e "SHOW PROCESSLIST;"
```

## 🚨 故障排除

### 常见问题

1. **Redis连接失败**
   ```bash
   # 检查Redis是否启动
   docker ps | grep redis
   
   # 重启Redis
   docker-compose restart redis
   ```

2. **MySQL连接失败**
   ```bash
   # 检查MySQL状态
   docker-compose logs mysql
   
   # 重置MySQL密码
   docker exec -it ai-drawing-mysql mysql -uroot -pDB_PASSWORD_PLACEHOLDER -e "ALTER USER 'root'@'%' IDENTIFIED BY 'DB_PASSWORD_PLACEHOLDER';"
   ```

3. **端口冲突**
   ```bash
   # 检查端口占用
   netstat -tlnp | grep :6379  # Redis
   netstat -tlnp | grep :3306  # MySQL
   netstat -tlnp | grep :5672  # RabbitMQ
   ```

4. **内存不足**
   ```bash
   # 检查系统内存
   free -h
   
   # 检查Docker内存使用
   docker stats
   ```

### 日志文件位置

- **后端**: `backend/logs/`
- **Redis**: Docker容器内 `/data/`
- **MySQL**: Docker卷 `mysql_data`
- **RabbitMQ**: Docker卷 `rabbitmq_data`

## 🔄 数据备份

### 定期备份脚本

```bash
#!/bin/bash
# backup.sh - 每日备份脚本

DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/backup/ai-drawing-$DATE"

mkdir -p $BACKUP_DIR

# 备份MySQL数据
docker exec ai-drawing-mysql mysqldump -uroot -pDB_PASSWORD_PLACEHOLDER ai_drawing_db > $BACKUP_DIR/mysql_backup.sql

# 备份Redis数据
docker exec ai-drawing-redis redis-cli BGSAVE
docker cp ai-drawing-redis:/data/dump.rdb $BACKUP_DIR/redis_backup.rdb

# 备份图片文件
cp -r storage/ $BACKUP_DIR/

echo "Backup completed: $BACKUP_DIR"
```

## 📊 性能优化建议

### Redis优化
```conf
# redis.conf 优化配置
maxmemory 2gb
maxmemory-policy allkeys-lru
save 900 1
save 300 10
save 60 10000
```

### MySQL优化
```ini
# my.cnf 优化配置
[mysqld]
innodb_buffer_pool_size = 1G
innodb_log_file_size = 256M
query_cache_size = 128M
max_connections = 200
```

## 🔐 安全配置

### 生产环境安全措施

1. **更改默认密码**
   ```bash
   # MySQL root密码
   # RabbitMQ用户密码
   # Redis认证密码
   ```

2. **网络安全**
   ```bash
   # 配置防火墙，只开放必要端口
   # 使用SSL/TLS加密
   # 配置反向代理
   ```

3. **访问控制**
   ```bash
   # 限制数据库访问IP
   # 配置Redis访问控制
   # 设置RabbitMQ虚拟主机
   ```

---

## 🎉 恭喜！

如果您已成功完成以上步骤，您的"幻境画廊"AI绘画平台Phase 1版本已成功部署！

**Phase 1已实现的企业级功能：**
- ✅ Redis缓存系统，大幅提升画廊性能
- ✅ 作品详情模态框，提供完整参数查看
- ✅ 参数复用功能，增强用户体验
- ✅ 容器化部署，确保环境一致性
- ✅ 企业级错误处理和用户反馈

**下一步：Phase 2开发**
- 个人作品历史管理
- API速率限制
- 社区互动功能（点赞、排行榜）

有任何问题请参考故障排除部分或查看详细的技术文档。


