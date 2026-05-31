<template>
  <div class="following-list-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <el-button 
        type="text" 
        @click="goBack"
        class="back-button"
      >
        ← 返回
      </el-button>
      <h2 class="page-title">
        <el-icon><UserFilled /></el-icon>
        我关注的人 ({{ followingList.length }})
      </h2>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="5" animated />
    </div>

    <!-- 空状态 -->
    <div v-else-if="followingList.length === 0" class="empty-state">
      <el-empty description="还没有关注任何人">
        <el-button type="primary" @click="goToDiscovery">
          去发现更多用户
        </el-button>
      </el-empty>
    </div>

    <!-- 关注列表 -->
    <div v-else class="following-grid">
      <el-card 
        v-for="user in followingList" 
        :key="user.userId"
        class="user-card"
        :body-style="{ padding: '20px' }"
        shadow="hover"
      >
        <div class="user-info">
          <!-- 用户头像 -->
          <div class="user-avatar">
            <el-avatar 
              :size="60" 
              :src="user.avatarUrl"
              :alt="user.username"
              class="avatar"
            >
              <el-icon><User /></el-icon>
            </el-avatar>
          </div>

          <!-- 用户详情 -->
          <div class="user-details">
            <h3 class="username" @click="goToUserProfile(user.userId)">
              {{ user.username }}
            </h3>
            <p class="user-stats">
              作品 {{ user.artworkCount || 0 }} · 
              粉丝 {{ user.followersCount || 0 }}
            </p>
            <p class="follow-time">
              {{ formatDate(user.followedAt) }} 关注
            </p>
          </div>

          <!-- 操作按钮 -->
          <div class="user-actions">
            <el-button
              type="danger"
              size="small"
              :loading="unfollowingUsers.has(user.userId)"
              @click="handleUnfollow(user)"
              class="unfollow-btn"
            >
              取消关注
            </el-button>
            <el-button
              type="text"
              size="small"
              @click="goToUserProfile(user.userId)"
              class="profile-btn"
            >
              查看主页
            </el-button>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 加载更多 -->
    <div v-if="hasMore && !loading" class="load-more">
      <el-button 
        @click="loadMore" 
        :loading="loadingMore"
        type="primary"
        plain
      >
        加载更多
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onActivated, onUnmounted, inject } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { User, UserFilled } from '@element-plus/icons-vue';
import api from '@/api';

// 注入依赖
const navigateToUserProfile = inject('navigateToUserProfile');
const goBackFunction = inject('goBack');

// 状态管理
const followingList = ref([]);
const loading = ref(true);
const loadingMore = ref(false);
const unfollowingUsers = ref(new Set());
const hasMore = ref(false);
const currentPage = ref(0);
const pageSize = 20;

// 后端API地址
const backendBaseUrl = 'http://localhost:8080';

// 返回上级页面
const goBack = () => {
  if (goBackFunction && goBackFunction()) {
    console.log('✅ [FollowingList] 使用路由系统返回');
  } else {
    console.log('🔄 [FollowingList] 路由系统返回失败，使用浏览器后退');
    window.history.back();
  }
};

// 跳转到发现页面（暂时跳转到画廊）
const goToDiscovery = () => {
  // 可以后续实现发现页面，暂时跳转到画廊
  window.location.hash = '#gallery';
};

// 跳转到用户个人主页
const goToUserProfile = (userId) => {
  if (navigateToUserProfile) {
    navigateToUserProfile(userId);
  }
};

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '';
  const date = new Date(dateString);
  const now = new Date();
  const diffTime = Math.abs(now - date);
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
  
  if (diffDays === 1) return '今天';
  if (diffDays === 2) return '昨天';
  if (diffDays <= 7) return `${diffDays}天前`;
  if (diffDays <= 30) return `${Math.ceil(diffDays / 7)}周前`;
  if (diffDays <= 365) return `${Math.ceil(diffDays / 30)}个月前`;
  return `${Math.ceil(diffDays / 365)}年前`;
};

// 获取关注列表
const fetchFollowingList = async (page = 0) => {
  try {

    console.log(`🔍 [FollowingList] 获取关注列表: page=${page}`);
    
    const response = await api.get(
      `/api/v1/follows/current-user/following`,
      {
        params: { page, size: pageSize }
      }
    );

    console.log('✅ [FollowingList] 关注列表获取成功:', response.data);
    
    // 后端返回的是 follows 字段，需要转换为前端期望的格式
    const followItems = response.data.follows || [];
    const hasMoreData = response.data.currentPage < response.data.totalPages;
    
    // 转换数据格式：将 FollowInfo 转换为用户信息
    const users = followItems.map(item => ({
      userId: item.userInfo.id,  // 后端字段名是 id 不是 userId
      username: item.userInfo.username,
      email: item.userInfo.email || '',  // 可能没有email字段
      avatarUrl: item.userInfo.avatarUrl || '',  // 可能没有头像
      artworkCount: item.userInfo.totalArtworks || 0,  // 后端字段名是 totalArtworks
      followersCount: item.userInfo.followersCount || 0,
      followedAt: item.followedAt
    }));
    
    if (page === 0) {
      followingList.value = users;
    } else {
      followingList.value.push(...users);
    }
    
    hasMore.value = hasMoreData;
    currentPage.value = page;
    
  } catch (error) {
    console.error('❌ [FollowingList] 获取关注列表失败:', error);
    ElMessage.error(error.response?.data?.message || '获取关注列表失败');
  }
};

// 加载更多
const loadMore = async () => {
  if (loadingMore.value || !hasMore.value) return;
  
  loadingMore.value = true;
  try {
    await fetchFollowingList(currentPage.value + 1);
  } finally {
    loadingMore.value = false;
  }
};

// 取消关注
const handleUnfollow = async (user) => {
  try {
    await ElMessageBox.confirm(
      `确定要取消关注 ${user.username} 吗？`,
      '确认取消关注',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    );
    
    unfollowingUsers.value.add(user.userId);
    
    console.log(`👎 [FollowingList] 取消关注用户: ${user.userId}`);
    
    await api.delete(
      `${backendBaseUrl}/api/v1/follows/${user.userId}`,
      {
        
      }
    );
    
    // 从列表中移除
    followingList.value = followingList.value.filter(u => u.userId !== user.userId);
    
    // 触发关注状态变化事件
    window.dispatchEvent(new CustomEvent('followStatusChange', {
      detail: {
        type: 'unfollow',
        userId: user.userId,
        targetUsername: user.username,
        source: 'FollowingList' // 标识事件来源
      }
    }));
    
    ElMessage.success(`已取消关注 ${user.username}`);
    console.log('✅ [FollowingList] 取消关注成功');
    console.log(`📡 [FollowingList] 已发送取消关注事件: userId: ${user.userId}`);
    
  } catch (error) {
    if (error !== 'cancel') {
      console.error('❌ [FollowingList] 取消关注失败:', error);
      ElMessage.error(error.response?.data?.message || '取消关注失败');
    }
  } finally {
    unfollowingUsers.value.delete(user.userId);
  }
};

// 页面激活时刷新数据（从其他页面返回时）
onActivated(async () => {
  console.log('🔄 [FollowingList] 页面激活，刷新关注列表数据');
  try {
    // 重置分页状态
    currentPage.value = 0;
    hasMore.value = false;
    
    // 重新获取第一页数据
    await fetchFollowingList(0);
  } catch (error) {
    console.error('❌ [FollowingList] 页面激活时刷新数据失败:', error);
  }
});

// 监听关注状态变化事件
const handleFollowStatusChange = async (event) => {
  const { type, userId } = event.detail;
  console.log(`📡 [FollowingList] 收到关注状态变化事件: ${type}, userId: ${userId}`);
  
  if (type === 'unfollow') {
    // 直接从列表中移除该用户
    followingList.value = followingList.value.filter(user => user.userId !== userId);
    console.log(`✅ [FollowingList] 已从关注列表移除用户: ${userId}`);
  } else if (type === 'follow') {
    // 重新获取第一页数据以包含新关注的用户
    try {
      currentPage.value = 0;
      await fetchFollowingList(0);
    } catch (error) {
      console.error('❌ [FollowingList] 刷新关注列表失败:', error);
    }
  }
};

// 组件挂载时添加事件监听
onMounted(async () => {
  try {
    await fetchFollowingList(0);
    
    // 添加关注状态变化事件监听
    window.addEventListener('followStatusChange', handleFollowStatusChange);
    console.log('📡 [FollowingList] 已添加关注状态变化事件监听');
  } finally {
    loading.value = false;
  }
});

// 组件卸载时移除事件监听
onUnmounted(() => {
  window.removeEventListener('followStatusChange', handleFollowStatusChange);
  console.log('📡 [FollowingList] 已移除关注状态变化事件监听');
});
</script>

<style scoped>
.following-list-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  min-height: 100vh;
  background: #f5f7fa;
}

.page-header {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
  background: white;
  padding: 16px 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.back-button {
  margin-right: 12px;
  font-size: 16px;
  color: #606266;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 8px;
}

.loading-container {
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.empty-state {
  background: white;
  padding: 40px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  text-align: center;
}

.following-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.user-card {
  transition: all 0.3s ease;
  border: 1px solid #e4e7ed;
}

.user-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 16px rgba(0,0,0,0.15);
}

.user-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.user-avatar {
  margin-bottom: 16px;
}

.avatar {
  border: 3px solid #f0f2f5;
  transition: all 0.3s ease;
}

.user-card:hover .avatar {
  border-color: #409eff;
}

.user-details {
  margin-bottom: 16px;
  flex: 1;
}

.username {
  margin: 0 0 8px 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  cursor: pointer;
  transition: color 0.3s ease;
}

.username:hover {
  color: #409eff;
}

.user-stats {
  margin: 0 0 8px 0;
  font-size: 14px;
  color: #909399;
}

.follow-time {
  margin: 0;
  font-size: 12px;
  color: #c0c4cc;
}

.user-actions {
  display: flex;
  gap: 8px;
  justify-content: center;
  width: 100%;
}

.unfollow-btn {
  transition: all 0.3s ease;
}

.profile-btn {
  color: #409eff;
}

.load-more {
  text-align: center;
  padding: 20px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .following-list-container {
    padding: 16px;
  }
  .following-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }
  .page-header {
    padding: 12px 16px;
  }
  .page-title {
    font-size: 18px;
  }
}

@media (max-width: 480px) {
  .following-list-container {
    padding: 8px;
  }
  .following-grid {
    gap: 12px;
  }
  .user-card-actions {
    flex-wrap: wrap;
    gap: 6px;
  }
}
</style>
