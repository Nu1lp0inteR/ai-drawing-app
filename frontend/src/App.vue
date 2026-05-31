<script setup>
import { ref, provide, onMounted, onUnmounted, watch } from 'vue'
import Studio from './views/Studio.vue'
import Gallery from './views/Gallery.vue'
import Profile from './views/Profile.vue'
import History from './views/History.vue'
import UserProfile from './views/UserProfile.vue'
import FollowingList from './views/FollowingList.vue'
import FollowersList from './views/FollowersList.vue'
import AuthDialog from './components/AuthDialog.vue'
import { User, Picture as IconPicture, Brush, UserFilled, Avatar, SwitchButton } from '@element-plus/icons-vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { ElNotification } from 'element-plus'
import { clearAllDrawings } from './utils/indexedDB.js'
import api from '@/api'

// --- 状态管理 ---
const activeView = ref('Studio')
const isLoggedIn = ref(false)
const authDialogVisible = ref(false)
const userInfo = ref(null) // 存储用户信息
const creditsBalance = ref(0)
const lastCompletedDrawing = ref(null) // 用于存放最新完成的绘图数据
const viewingUserId = ref(null) // 当前查看的用户ID（用于用户个人主页）

// --- 路由和导航历史管理 ---
const navigationHistory = ref([]) // 导航历史栈
const isNavigatingProgrammatically = ref(false) // 标记是否为程序化导航

// --- WebSocket 客户端实例 ---
let stompClient = null
const wsConnected = ref(false)

// --- 路由管理系统 ---

// 路由映射：将路径映射到视图和参数
const parseRoute = (path) => {
  console.log('🔍 [Router] 解析路由:', path);
  
  if (path === '/' || path === '/studio') {
    return { view: 'Studio', params: {} };
  } else if (path === '/gallery') {
    return { view: 'Gallery', params: {} };
  } else if (path === '/profile') {
    return { view: 'Profile', params: {} };
  } else if (path === '/history') {
    return { view: 'History', params: {} };
  } else if (path.startsWith('/user/')) {
    const userId = path.replace('/user/', '');
    return { view: 'UserProfile', params: { userId } };
  } else if (path === '/following') {
    return { view: 'FollowingList', params: {} };
  } else if (path === '/followers') {
    return { view: 'FollowersList', params: {} };
  }
  
  // 默认路由
  return { view: 'Studio', params: {} };
};

// 生成路径：根据视图和参数生成路径
const generatePath = (view, params = {}) => {
  switch (view) {
    case 'Studio':
      return '/studio';
    case 'Gallery':
      return '/gallery';
    case 'Profile':
      return '/profile';
    case 'History':
      return '/history';
    case 'UserProfile':
      return `/user/${params.userId || ''}`;
    case 'FollowingList':
      return '/following';
    case 'FollowersList':
      return '/followers';
    default:
      return '/studio';
  }
};

// 程序化导航：更新视图并推送到历史记录
const navigateTo = (view, params = {}, addToHistory = true) => {
  console.log('🔄 [Router] 导航到:', view, params);
  
  const path = generatePath(view, params);
  
  // 更新状态
  activeView.value = view;
  if (params.userId) {
    viewingUserId.value = params.userId;
  }
  
  // 添加到导航历史
  if (addToHistory) {
    navigationHistory.value.push({
      view,
      params,
      path,
      timestamp: Date.now()
    });
    
    // 限制历史记录长度
    if (navigationHistory.value.length > 50) {
      navigationHistory.value = navigationHistory.value.slice(-50);
    }
  }
  
  // 更新浏览器URL
  isNavigatingProgrammatically.value = true;
  window.history.pushState({ view, params, path }, '', path);
  
  console.log('✅ [Router] 导航完成，当前路径:', path);
};

// 返回功能：回到上一个页面
const goBack = () => {
  console.log('🔙 [Router] 执行返回操作');
  
  if (navigationHistory.value.length > 1) {
    // 移除当前页面
    navigationHistory.value.pop();
    
    // 获取上一个页面
    const prevPage = navigationHistory.value[navigationHistory.value.length - 1];
    console.log('📍 [Router] 返回到:', prevPage);
    
    // 导航到上一个页面（不添加到历史）
    activeView.value = prevPage.view;
    if (prevPage.params?.userId) {
      viewingUserId.value = prevPage.params.userId;
    }
    
    // 更新浏览器URL
    isNavigatingProgrammatically.value = true;
    window.history.pushState(prevPage, '', prevPage.path);
    
    console.log('✅ [Router] 返回操作完成');
    return true;
  } else {
    console.log('⚠️ [Router] 无法返回，已在起始页面');
    return false;
  }
};

// 处理浏览器前进/后退按钮
const handlePopState = (event) => {
  console.log('🔄 [Router] 浏览器前进/后退事件:', event.state);
  
  if (isNavigatingProgrammatically.value) {
    isNavigatingProgrammatically.value = false;
    return;
  }
  
  if (event.state && event.state.view) {
    // 恢复到指定状态
    activeView.value = event.state.view;
    if (event.state.params?.userId) {
      viewingUserId.value = event.state.params.userId;
    }
    console.log('✅ [Router] 已恢复到浏览器历史状态:', event.state);
  } else {
    // 解析当前URL
    const route = parseRoute(window.location.pathname);
    activeView.value = route.view;
    if (route.params.userId) {
      viewingUserId.value = route.params.userId;
    }
    console.log('✅ [Router] 已根据URL解析状态:', route);
  }
};

// 初始化路由：根据当前URL设置初始状态
const initializeRouter = () => {
  console.log('🚀 [Router] 初始化路由系统');
  
  const currentPath = window.location.pathname || '/';
  const route = parseRoute(currentPath);
  
  activeView.value = route.view;
  if (route.params.userId) {
    viewingUserId.value = route.params.userId;
  }
  
  // 添加到历史记录
  navigationHistory.value.push({
    view: route.view,
    params: route.params,
    path: currentPath,
    timestamp: Date.now()
  });
  
  console.log('✅ [Router] 路由初始化完成:', route);
};

// --- 导航到用户个人主页 ---
const navigateToUserProfile = (userId) => {
  navigateTo('UserProfile', { userId });
};

// --- 导航到关注列表页面 ---
const navigateToFollowingList = () => {
  navigateTo('FollowingList');
};

// --- 导航到粉丝列表页面 ---
const navigateToFollowersList = () => {
  navigateTo('FollowersList');
};

// --- provide (依赖注入) ---
provide('isLoggedIn', isLoggedIn)
provide('userInfo', userInfo)
provide('showAuthDialog', () => { authDialogVisible.value = true })
provide('lastCompletedDrawing', lastCompletedDrawing) // 将最新绘图数据注入子组件
provide('navigateToUserProfile', navigateToUserProfile) // 导航到用户个人主页的方法
provide('navigateToFollowingList', navigateToFollowingList) // 导航到关注列表页面的方法
provide('navigateToFollowersList', navigateToFollowersList) // 导航到粉丝列表页面的方法
provide('creditsBalance', creditsBalance)
provide('fetchCredits', fetchCredits)
provide('navigateTo', navigateTo) // 通用导航方法
provide('goBack', goBack) // 返回功能

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
    wsConnected.value = true
    console.log('Connected to WebSocket:', frame)
    // 订阅公共广播频道
    stompClient.subscribe('/topic/drawing_complete', (message) => {
      console.log('📨 [App] Received drawing message:', message.body)
      const messageData = JSON.parse(message.body)
      console.log('📨 [App] Parsed message data:', messageData)
      
      // 检查是否是失败消息
      if (messageData.status === 'FAILED') {
        console.error('❌ [App] Drawing task failed:', messageData.error)
        
        // 发送失败事件给Studio组件
        window.dispatchEvent(new CustomEvent('drawingFailed', { 
          detail: { 
            error: messageData.error,
            timestamp: messageData.timestamp 
          } 
        }))
        
        // 显示失败通知
        ElNotification({
          title: '绘图失败',
          message: messageData.error || '生图服务暂时不可用，请稍后重试',
          type: 'error',
          duration: 5000
        })
        return
      }
      
      // 处理成功消息（支持新旧两种格式）
      console.log('🎉 [App] Drawing completed successfully:', messageData)

      // 新版格式：直接包含 image_base64
      // 旧版格式：包含 storedFilename，需要拼接 URL
      if (messageData.image_base64) {
        lastCompletedDrawing.value = {
          ...messageData,
          imageUrl: 'data:image/png;base64,' + messageData.image_base64,
        }
      } else if (messageData.stored_filename || messageData.storedFilename) {
        const filename = messageData.stored_filename || messageData.storedFilename
        lastCompletedDrawing.value = {
          ...messageData,
          imageUrl: `http://localhost:8080/api/v1/images/${filename}`,
        }
      } else {
        lastCompletedDrawing.value = messageData
      }
      console.log('🎉 [App] Updated lastCompletedDrawing.value:', lastCompletedDrawing.value)

      // 通知个人中心有新图片生成
      window.dispatchEvent(new CustomEvent('drawingCompleted', {
        detail: lastCompletedDrawing.value
      }))
      console.log('📡 [App] 已通知个人中心有新图片生成')

      // 弹出成功通知
      ElNotification({
        title: '绘图完成！',
        message: '您的新作品已生成，快去看看吧！',
        type: 'success',
      })
    });
  };

  // 定义连接错误时的回调
  stompClient.onStompError = (frame) => {
    wsConnected.value = false
    console.error('Broker reported error:', frame.headers['message'])
    console.error('Additional details:', frame.body)
  };

  stompClient.onWebSocketClose = () => {
    wsConnected.value = false
    console.warn('WebSocket connection closed, will auto-reconnect...')
  };

  stompClient.onDisconnect = () => {
    wsConnected.value = false
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

// --- 认证相关函数 ---
const handleLoginSuccess = (user) => {
  console.log('🎉 [App] 用户登录成功:', user);
  isLoggedIn.value = true;
  userInfo.value = user;
  authDialogVisible.value = false;
  fetchCredits();
};

const fetchCredits = async () => {
  if (!isLoggedIn.value) return;
  try {
    const response = await api.get('/api/v1/credits/balance');
    creditsBalance.value = response.data.balance || 0;
    console.log('💰 [App] 积分余额:', creditsBalance.value);
  } catch (error) {
    console.error('获取积分失败:', error);
  }
};

// --- 用户菜单命令处理 ---
const handleUserMenuCommand = (command) => {
  console.log('🔧 [App] 用户菜单命令:', command);
  
  switch (command) {
    case 'profile':
      navigateTo('Profile');
      break;
    case 'history':
      navigateTo('History');
      break;
    case 'following':
      navigateToFollowingList();
      break;
    case 'followers':
      navigateToFollowersList();
      break;
    case 'logout':
      handleLogout();
      break;
    default:
      console.warn('⚠️ [App] 未知的用户菜单命令:', command);
  }
};

const handleLogout = async () => {
  console.log('👋 [App] 用户退出登录');

  const accessToken = localStorage.getItem('accessToken');
  const refreshToken = localStorage.getItem('refreshToken');

  try {
    if (accessToken) {
      await api.post('/api/v1/auth/logout', null, {
        params: { refreshToken },
      });
      console.log('✅ [App] 后端登出成功，token 已加入黑名单');
    }
  } catch (error) {
    console.warn('⚠️ [App] 后端登出请求失败, 继续本地清理:', error.message);
  }

  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
  localStorage.removeItem('userInfo');
  localStorage.removeItem('ai_drawing_history');
  isLoggedIn.value = false;
  userInfo.value = null;

  // 清除 IndexedDB 中的本地作品缓存
  clearAllDrawings().then(() => {
    console.log('🗑️ [App] IndexedDB 本地作品已清除');
  }).catch(err => {
    console.error('❌ [App] 清除 IndexedDB 失败:', err);
  });
  
  // 通知其他组件清空历史记录
  window.dispatchEvent(new CustomEvent('clearHistory'));
  
  // 通知个人中心清空数据
  window.dispatchEvent(new CustomEvent('userLogout'));
  console.log('📡 [App] 已通知个人中心清理数据');
};

// JWT token过期检测函数
const isTokenExpired = (token) => {
  if (!token) return true;
  
  try {
    // JWT token格式: header.payload.signature
    const parts = token.split('.');
    if (parts.length !== 3) return true;
    
    // 解码payload (base64)
    const payload = JSON.parse(atob(parts[1]));
    const currentTime = Math.floor(Date.now() / 1000); // 当前时间戳(秒)
    
    // 检查是否过期 (exp字段是过期时间戳)
    return payload.exp && payload.exp < currentTime;
  } catch (error) {
    console.error('❌ [App] JWT token解析失败:', error);
    return true; // 解析失败视为过期
  }
};

const checkAuthStatus = () => {
  const token = localStorage.getItem('accessToken');
  const storedUserInfo = localStorage.getItem('userInfo');
  
  if (token && storedUserInfo) {
    // 检查token是否过期
    if (isTokenExpired(token)) {
      console.warn('⚠️ [App] JWT token已过期，清除登录状态');
      handleLogout(); // 自动登出
      ElNotification({
        title: '登录已过期',
        message: '您的登录已过期，请重新登录',
        type: 'warning',
        duration: 3000
      });
      return;
    }
    
    try {
      userInfo.value = JSON.parse(storedUserInfo);
      isLoggedIn.value = true;
      fetchCredits();
      console.log('✅ [App] 从本地存储恢复登录状态:', userInfo.value.username);
    } catch (error) {
      console.error('❌ [App] 解析用户信息失败:', error);
      handleLogout(); // 清除损坏的数据
    }
  }
};

// --- 组件路由逻辑 ---
const getActiveComponent = () => {
  switch (activeView.value) {
    case 'Studio':
      return Studio
    case 'Gallery':
      return Gallery
    case 'Profile':
      return Profile
    case 'History':
      return History
    case 'UserProfile':
      return UserProfile
    case 'FollowingList':
      return FollowingList
    case 'FollowersList':
      return FollowersList
    default:
      return Studio
  }
}

// --- 事件监听：处理从画廊跳转到创作中心的请求 ---
const handleSwitchToStudio = (event) => {
  navigateTo('Studio');
  if (event.detail?.autoFill) {
    // 延迟一点时间确保Studio组件已经挂载
    setTimeout(() => {
      window.dispatchEvent(new CustomEvent('loadAutoFillParams'));
    }, 100);
  }
};

// --- Vue 生命周期钩子 ---
onMounted(() => {
  checkAuthStatus() // 检查本地存储的登录状态
  connectWebSocket() // 组件挂载时，建立WebSocket连接
  
  // 初始化路由系统
  initializeRouter();
  
  // 监听切换到Studio的事件
  window.addEventListener('switchToStudio', handleSwitchToStudio);
  
  // 监听浏览器前进/后退按钮
  window.addEventListener('popstate', handlePopState);
})

onUnmounted(() => {
  disconnectWebSocket() // 组件卸载时，断开WebSocket连接
  
  // 清理事件监听器
  window.removeEventListener('switchToStudio', handleSwitchToStudio);
  window.removeEventListener('popstate', handlePopState);
})

// 已移动到上方的认证相关函数中
</script>

<template>
  <el-container class="main-container">
    <!-- 顶部导航栏 (无变动) -->
    <el-header class="header">
      <div class="header-content">
        <div class="logo">
          <img src="https://vuejs.org/images/logo.png" alt="Vue Logo" />
          <h1>AI 绘画工作室</h1>
          <span class="ws-indicator" :class="{ connected: wsConnected }" :title="wsConnected ? 'WebSocket 已连接' : 'WebSocket 断开, 正在重连...'"></span>
        </div>
        <el-menu :default-active="activeView" mode="horizontal" @select="(index) => navigateTo(index)" :ellipsis="false">
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
          <el-button v-if="!isLoggedIn" @click="authDialogVisible = true" :icon="User" round>
            登录 / 注册
          </el-button>
          <el-dropdown v-else @command="handleUserMenuCommand">
            <span class="user-display">
              <el-avatar src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" />
              <span class="username">{{ userInfo?.username }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>💰 积分: {{ creditsBalance }}</el-dropdown-item>
                <el-dropdown-item disabled>{{ userInfo?.email }}</el-dropdown-item>
                <el-dropdown-item divided command="profile">
                  <el-icon><User /></el-icon>
                  个人中心
                </el-dropdown-item>
                <el-dropdown-item command="history">
                  <el-icon><Picture /></el-icon>
                  我的作品
                </el-dropdown-item>
                <el-dropdown-item command="following">
                  <el-icon><UserFilled /></el-icon>
                  我的关注
                </el-dropdown-item>
                <el-dropdown-item command="followers">
                  <el-icon><Avatar /></el-icon>
                  我的粉丝
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </el-header>

    <!-- 主内容区 -->
    <el-main class="main-content">
      <keep-alive>
        <component 
          :is="getActiveComponent()" 
          :userId="activeView === 'UserProfile' ? viewingUserId : undefined"
        />
      </keep-alive>
    </el-main>

    <!-- 登录对话框组件 (无变动) -->
    <!-- 认证对话框 -->
    <AuthDialog 
      v-model="authDialogVisible" 
      @login-success="handleLoginSuccess" 
    />
  </el-container>
</template>

<style scoped>
/* 样式 (无变动) */
.main-container {
  height: 100vh;
  height: 100dvh;
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
.ws-indicator {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #e74c3c;
  margin-left: 4px;
  flex-shrink: 0;
  transition: background 0.3s;
}
.ws-indicator.connected {
  background: #67c23a;
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

.user-display {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.username {
  color: var(--el-text-color-primary);
  font-weight: 500;
}
.main-content {
  flex-grow: 1;
  background-color: #f0f2f5;
  padding: 0;
  height: calc(100vh - 60px);
  height: calc(100dvh - 60px);
}

@media (max-width: 768px) {
  .header {
    padding: 0 12px;
  }
  .header-content {
    gap: 8px;
  }
  .logo img {
    height: 28px;
    margin-right: 6px;
  }
  .logo h1 {
    font-size: 16px;
  }
  .el-menu {
    flex-grow: 0;
  }
  .el-menu :deep(.el-menu-item) {
    padding: 0 10px;
    font-size: 13px;
  }
  .user-section {
    width: auto;
    flex-shrink: 0;
  }
  .username {
    display: none;
  }
}

@media (max-width: 480px) {
  .logo h1 {
    display: none;
  }
  .el-menu :deep(.el-menu-item) {
    padding: 0 8px;
    font-size: 12px;
  }
  .el-menu :deep(.el-menu-item .el-icon) {
    margin-right: 2px;
  }
}
</style>
