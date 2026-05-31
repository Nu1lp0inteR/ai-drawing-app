<template>
  <div class="followers-list-container">
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
        <el-icon><Avatar /></el-icon>
        我的粉丝 ({{ followersList.length }})
      </h2>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="5" animated />
    </div>

    <!-- 空状态 -->
    <div v-else-if="followersList.length === 0" class="empty-state">
      <el-empty description="还没有粉丝">
        <el-button type="primary" @click="goToShare">
          分享作品获得更多关注
        </el-button>
      </el-empty>
    </div>

    <!-- 粉丝列表 -->
    <div v-else class="followers-grid">
      <el-card 
        v-for="user in followersList" 
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
              关注 {{ user.followingCount || 0 }}
            </p>
            <p class="follow-time">
              {{ formatDate(user.followedAt) }} 关注了你
            </p>
          </div>

          <!-- 操作按钮 -->
          <div class="user-actions">
            <el-button
              :type="user.isFollowedBack ? 'danger' : 'primary'"
              size="small"
              :loading="followingUsers.has(user.userId)"
              @click="handleToggleFollow(user)"
              class="follow-btn"
            >
              {{ user.isFollowedBack ? '取消关注' : '回关' }}
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
import { ref, onMounted, onActivated, inject } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { User, Avatar } from '@element-plus/icons-vue';
import api from '@/api';

// 注入依赖
const navigateToUserProfile = inject('navigateToUserProfile');
const goBackFunction = inject('goBack');

// 状态管理
const followersList = ref([]);
const loading = ref(true);
const loadingMore = ref(false);
const followingUsers = ref(new Set());
const hasMore = ref(false);
const currentPage = ref(0);
const pageSize = 20;

// 后端API地址
const backendBaseUrl = 'http://localhost:8080';

// 返回上级页面
const goBack = () => {
  if (goBackFunction && goBackFunction()) {
    console.log('✅ [FollowersList] 使用路由系统返回');
  } else {
    console.log('🔄 [FollowersList] 路由系统返回失败，使用浏览器后退');
    window.history.back();
  }
};

// 跳转到分享页面（暂时跳转到创作中心）
const goToShare = () => {
  // 可以后续实现专门的分享功能，暂时跳转到创作中心
  window.location.hash = '#studio';
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

// 获取粉丝列表
const fetchFollowersList = async (page = 0) => {
  try {

    console.log(`🔍 [FollowersList] 获取粉丝列表: page=${page}`);
    
    const response = await api.get(
      `/api/v1/follows/current-user/followers`,
      {
        params: { page, size: pageSize }
      }
    );

    console.log('✅ [FollowersList] 粉丝列表获取成功:', response.data);
    
    // 后端返回的是 follows 字段，需要转换为前端期望的格式
    const followItems = response.data.follows || [];
    const hasMoreData = response.data.currentPage < response.data.totalPages;
    
    // 转换数据格式：将 FollowInfo 转换为用户信息，包含回关状态
    const users = followItems.map(item => ({
      userId: item.userInfo.id,  // 后端字段名是 id 不是 userId
      username: item.userInfo.username,
      email: item.userInfo.email || '',  // 可能没有email字段
      avatarUrl: item.userInfo.avatarUrl || '',  // 可能没有头像
      artworkCount: item.userInfo.totalArtworks || 0,  // 后端字段名是 totalArtworks
      followingCount: item.userInfo.followingCount || 0,
      followedAt: item.followedAt,
      isFollowedBack: item.isFollowing || false  // 是否已回关
    }));
    
    if (page === 0) {
      followersList.value = users;
    } else {
      followersList.value.push(...users);
    }
    
    hasMore.value = hasMoreData;
    currentPage.value = page;
    
  } catch (error) {
    console.error('❌ [FollowersList] 获取粉丝列表失败:', error);
    ElMessage.error(error.response?.data?.message || '获取粉丝列表失败');
  }
};

// 加载更多
const loadMore = async () => {
  if (loadingMore.value || !hasMore.value) return;
  
  loadingMore.value = true;
  try {
    await fetchFollowersList(currentPage.value + 1);
  } finally {
    loadingMore.value = false;
  }
};

// 切换关注状态
const handleToggleFollow = async (user) => {
  try {
    const isCurrentlyFollowing = user.isFollowedBack;
    const action = isCurrentlyFollowing ? '取消关注' : '关注';
    
    if (isCurrentlyFollowing) {
      await ElMessageBox.confirm(
        `确定要取消关注 ${user.username} 吗？`,
        '确认取消关注',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning',
        }
      );
    }
    
    followingUsers.value.add(user.userId);
    
    console.log(`🔄 [FollowersList] ${action}用户: ${user.userId}`);
    
    if (isCurrentlyFollowing) {
      // 取消关注
      await api.delete(
        `${backendBaseUrl}/api/v1/follows/${user.userId}`,
        {
          
        }
      );
    } else {
      // 关注
      await api.post(
        `${backendBaseUrl}/api/v1/follows/${user.userId}`,
        {},
        {
          
        }
      );
    }
    
    // 更新本地状态
    user.isFollowedBack = !isCurrentlyFollowing;
    
    ElMessage.success(`已${action} ${user.username}`);
    console.log(`✅ [FollowersList] ${action}成功`);
    
  } catch (error) {
    if (error !== 'cancel') {
      console.error('❌ [FollowersList] 关注操作失败:', error);
      ElMessage.error(error.response?.data?.message || '操作失败');
    }
  } finally {
    followingUsers.value.delete(user.userId);
  }
};

// 组件挂载时获取数据
onMounted(async () => {
  try {
    await fetchFollowersList(0);
  } finally {
    loading.value = false;
  }
});

// 页面激活时刷新数据（从其他页面返回时）
onActivated(async () => {
  console.log('🔄 [FollowersList] 页面激活，刷新粉丝列表数据');
  try {
    // 重置分页状态
    currentPage.value = 0;
    hasMore.value = false;
    
    // 重新获取第一页数据
    await fetchFollowersList(0);
  } catch (error) {
    console.error('❌ [FollowersList] 页面激活时刷新数据失败:', error);
  }
});
</script>

<style scoped>
.followers-list-container {
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

.followers-grid {
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

.follow-btn {
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
  .followers-list-container {
    padding: 16px;
  }
  .followers-grid {
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
  .followers-list-container {
    padding: 8px;
  }
  .followers-grid {
    gap: 12px;
  }
  .user-card-actions {
    flex-wrap: wrap;
    gap: 6px;
  }
}
</style>
