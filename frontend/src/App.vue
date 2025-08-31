<script setup>
import { ref, provide, onMounted, onUnmounted } from 'vue'
import Studio from './views/Studio.vue'
import Gallery from './views/Gallery.vue'
import LoginDialog from './components/LoginDialog.vue'
import { User, Picture as IconPicture, Brush } from '@element-plus/icons-vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { ElNotification } from 'element-plus'

// --- 状态管理 ---
const activeView = ref('Studio')
const isLoggedIn = ref(false)
const loginDialogVisible = ref(false)
const lastCompletedDrawing = ref(null) // 用于存放最新完成的绘图数据

// --- WebSocket 客户端实例 ---
let stompClient = null

// --- provide (依赖注入) ---
provide('isLoggedIn', isLoggedIn)
provide('showLoginDialog', () => { loginDialogVisible.value = true })
provide('lastCompletedDrawing', lastCompletedDrawing) // 将最新绘图数据注入子组件

// --- WebSocket 连接逻辑 ---
const connectWebSocket = () => {
  // 后端WebSocket服务的地址
  const socketUrl = 'http://localhost:8080/ws'
  
  // 创建一个Stomp客户端实例
  stompClient = new Client({
    webSocketFactory: () => new SockJS(socketUrl), // 使用SockJS作为备用方案
    debug: (str) => {
      console.log('STOMP Debug:', str) // 在控制台打印调试信息
    },
    reconnectDelay: 5000, // 5秒后自动重连
  });

  // 定义连接成功后的回调
  stompClient.onConnect = (frame) => {
    console.log('Connected to WebSocket:', frame)
    // 订阅公共广播频道
    stompClient.subscribe('/topic/drawing_complete', (message) => {
      console.log('🎉 [App] Received drawing completion message:', message.body)
      const completedDrawing = JSON.parse(message.body)
      console.log('🎉 [App] Parsed drawing data:', completedDrawing)
      
      // 更新最新完成的绘图数据
      lastCompletedDrawing.value = completedDrawing
      console.log('🎉 [App] Updated lastCompletedDrawing.value:', lastCompletedDrawing.value)

      // 弹出一个成功的通知
      ElNotification({
        title: '绘图完成！',
        message: '您的新作品已生成，快去看看吧！',
        type: 'success',
      })
    });
  };

  // 定义连接错误时的回调
  stompClient.onStompError = (frame) => {
    console.error('Broker reported error:', frame.headers['message'])
    console.error('Additional details:', frame.body)
  };

  // 激活连接
  stompClient.activate()
}

const disconnectWebSocket = () => {
  if (stompClient) {
    stompClient.deactivate()
    console.log('WebSocket disconnected.')
  }
}

// --- Vue 生命周期钩子 ---
onMounted(() => {
  connectWebSocket() // 组件挂载时，建立WebSocket连接
})

onUnmounted(() => {
  disconnectWebSocket() // 组件卸载时，断开WebSocket连接
})

// --- 其他函数 ---
const handleLoginSuccess = () => {
  isLoggedIn.value = true
  loginDialogVisible.value = false
}
</script>

<template>
  <el-container class="main-container">
    <!-- 顶部导航栏 (无变动) -->
    <el-header class="header">
      <div class="header-content">
        <div class="logo">
          <img src="https://vuejs.org/images/logo.png" alt="Vue Logo" />
          <h1>AI 绘画工作室</h1>
        </div>
        <el-menu :default-active="activeView" mode="horizontal" @select="(index) => activeView = index" :ellipsis="false">
          <el-menu-item index="Studio">
            <el-icon><Brush /></el-icon>
            创作中心
          </el-menu-item>
          <el-menu-item index="Gallery">
            <el-icon><IconPicture /></el-icon>
            画廊
          </el-menu-item>
        </el-menu>
        <div class="user-section">
          <el-button v-if="!isLoggedIn" @click="loginDialogVisible = true" :icon="User" round>
            登录 / 注册
          </el-button>
          <el-dropdown v-else>
            <el-avatar src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" />
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item>个人中心</el-dropdown-item>
                <el-dropdown-item divided @click="isLoggedIn = false">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </el-header>

    <!-- 主内容区 (无变动) -->
    <el-main class="main-content">
      <keep-alive>
        <component :is="activeView === 'Studio' ? Studio : Gallery" />
      </keep-alive>
    </el-main>

    <!-- 登录对话框组件 (无变动) -->
    <LoginDialog v-model="loginDialogVisible" @login-success="handleLoginSuccess" />
  </el-container>
</template>

<style scoped>
/* 样式 (无变动) */
.main-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
}
.header {
  border-bottom: 1px solid #dcdfe6;
  background-color: #fff;
  padding: 0 20px;
}
.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
}
.logo {
  display: flex;
  align-items: center;
}
.logo img {
  height: 32px;
  margin-right: 12px;
}
.logo h1 {
  font-size: 20px;
  margin: 0;
}
.el-menu {
  flex-grow: 1;
  justify-content: center;
  border-bottom: none;
  background-color: transparent;
}
.user-section {
  width: 150px;
  display: flex;
  justify-content: flex-end;
}
.main-content {
  flex-grow: 1;
  background-color: #f0f2f5;
  padding: 0;
  height: calc(100vh - 60px);
}
</style>
