#!/bin/bash
# 停止 AI 绘图平台所有服务
# 此脚本会强制停止后端 Spring Boot 和前端 Vite 开发服务器

echo "=== 停止所有服务 ==="

# 停止 screen 会话（如果有的话）
screen -ls | grep -E "^\s" | awk '{print $1}' | while read s; do
    screen -S "$s" -X quit 2>/dev/null
done

# 强制杀死 Spring Boot (mvnw spring-boot:run) 和 Vite 进程
KILLED=0

# Spring Boot 应用进程
for pid in $(pgrep -f "BackendApplication"); do
    echo "Killing BackendApplication pid=$pid"
    kill -9 "$pid" 2>/dev/null && KILLED=$((KILLED+1))
done

# Maven Wrapper (spring-boot:run)
for pid in $(pgrep -f "mvnw.*spring-boot:run"); do
    echo "Killing Maven Wrapper pid=$pid"
    kill -9 "$pid" 2>/dev/null && KILLED=$((KILLED+1))
done

# Vite 开发服务器
for pid in $(pgrep -f "vite"); do
    echo "Killing Vite pid=$pid"
    kill -9 "$pid" 2>/dev/null && KILLED=$((KILLED+1))
done

# 等待进程退出
sleep 1

# 强制释放端口
for port in 8080 5173; do
    pid=$(lsof -ti:$port 2>/dev/null)
    if [ -n "$pid" ]; then
        echo "Force releasing port $port (pid=$pid)"
        kill -9 "$pid" 2>/dev/null
    fi
done

# 清理死 screen 会话
screen -wipe 2>/dev/null

# 验证
echo ""
echo "=== 验证端口状态 ==="
for port in 8080 5173; do
    if ss -tlnp | grep -q ":$port "; then
        echo "[WARN] Port $port still in use!"
    else
        echo "[OK]   Port $port free"
    fi
done

echo ""
echo "共终止 $KILLED 个进程"
