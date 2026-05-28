<script setup>
import { ref, onMounted, onUnmounted, inject, watchEffect } from 'vue';
import api from '@/api';
import { ElMessage, ElCard, ElAvatar, ElTag, ElPagination, ElEmpty, ElIcon, ElButton } from 'element-plus';
import { User, Picture, Calendar, Share, Plus, Check } from '@element-plus/icons-vue';
import ArtworkDetailModal from '@/components/ArtworkDetailModal.vue';

// --- 接收参数 ---
const props = defineProps({
  userId: {
    type: String,
    required: true
  }
});

// --- 状态管理 ---
const userProfile = ref(null);
const artworkList = ref({
  artworks: [],
  totalCount: 0,
  currentPage: 1,
  totalPages: 0,
  pageSize: 12
});

const isLoading = ref(true);
const isLoadingArtworks = ref(false);
const currentPage = ref(1);
const pageSize = ref(12);

// 模态框相关状态
const detailModalVisible = ref(false);
const selectedArtwork = ref(null);

// 关注相关状态
const followInfo = ref({
  isFollowing: false,
  followersCount: 0
});
const isFollowLoading = ref(false);

// 注入依赖
const isLoggedIn = inject('isLoggedIn');

// --- 函数 ---

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '未知';
  return new Date(dateString).toLocaleDateString('zh-CN');
};

// 获取用户公开信息
const fetchUserProfile = async () => {
  try {
    isLoading.value = true;
    console.log('🔍 [UserProfile] 获取用户主页信息:', props.userId);
    
    const response = await api.get(`/api/v1/users/${props.userId}/profile`);
    userProfile.value = response.data;
    
    console.log('✅ [UserProfile] 用户主页信息获取成功:', response.data);
  } catch (error) {
    console.error('❌ [UserProfile] 获取用户主页失败:', error);
    
    if (error.response?.status === 404) {
      ElMessage.error('用户不存在');
    } else {
      ElMessage.error('获取用户信息失败');
    }
  } finally {
    isLoading.value = false;
  }
};

// 获取用户公开作品
const fetchUserArtworks = async (page = 0, size = pageSize.value) => {
  try {
    isLoadingArtworks.value = true;
    console.log('🎨 [UserProfile] 获取用户公开作品:', props.userId, 'page:', page);
    
    const response = await api.get(`/api/v1/users/${props.userId}/artworks`, {
      params: { page, size }
    });
    
    artworkList.value = response.data;
    console.log('✅ [UserProfile] 用户公开作品获取成功:', response.data);
  } catch (error) {
    console.error('❌ [UserProfile] 获取用户作品失败:', error);
    ElMessage.error('获取用户作品失败');
  } finally {
    isLoadingArtworks.value = false;
  }
};

// 处理分页
const handlePageChange = (page) => {
  currentPage.value = page;
  fetchUserArtworks(page - 1, pageSize.value);
};

// 查看作品详情
const viewArtworkDetail = (artwork) => {
  selectedArtwork.value = artwork;
  detailModalVisible.value = true;
};

// 处理复制参数
const handleCopyParams = (params) => {
  navigator.clipboard.writeText(JSON.stringify(params, null, 2)).then(() => {
    ElMessage.success('参数已复制到剪贴板');
  });
};

// 处理图片加载错误
const handleImageError = (event) => {
  console.warn('🖼️ [UserProfile] 图片加载失败:', event.target.src);
};

// 检查关注状态
const checkFollowStatus = async () => {
  if (!isLoggedIn.value) {
    followInfo.value = {
      isFollowing: false,
      followersCount: userProfile.value?.followersCount || 0
    };
    return;
  }
  
  try {
    console.log('👥 [UserProfile] 检查关注状态:', props.userId);
    
    const response = await api.get(`/api/v1/follows/${props.userId}/status`, {
      headers: {
        
      }
    });
    
    followInfo.value = {
      isFollowing: response.data.isFollowing,
      followersCount: response.data.followersCount
    };
    
    console.log('✅ [UserProfile] 关注状态查询成功:', response.data);
  } catch (error) {
    console.error('❌ [UserProfile] 检查关注状态失败:', error);
    followInfo.value = {
      isFollowing: false,
      followersCount: userProfile.value?.followersCount || 0
    };
  }
};

// 切换关注状态
const toggleFollow = async () => {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录');
    return;
  }
  
  if (isFollowLoading.value) {
    console.log('⚠️ [UserProfile] 操作正在进行中，忽略重复请求');
    return;
  }
  
  try {
    isFollowLoading.value = true;
    
    // 记录操作前的状态
    const currentFollowingState = followInfo.value.isFollowing;
    console.log(`👥 [UserProfile] 开始${currentFollowingState ? '取消关注' : '关注'}操作: ${props.userId}`);
    
    let response;
    if (followInfo.value.isFollowing) {
      // 取消关注
      console.log('👥 [UserProfile] 取消关注:', props.userId);
      response = await api.delete(`/api/v1/follows/${props.userId}`, {
        headers: {
          
        }
      });
      ElMessage.success('已取消关注');
    } else {
      // 关注用户
      console.log('👥 [UserProfile] 关注用户:', props.userId);
      response = await api.post(`/api/v1/follows/${props.userId}`, {}, {
        headers: {
          
        }
      });
      ElMessage.success('关注成功');
    }
    
    // 更新关注状态
    followInfo.value = {
      isFollowing: response.data.isFollowing,
      followersCount: response.data.followersCount
    };
    
    // 同时更新用户资料中的粉丝数
    if (userProfile.value) {
      userProfile.value.followersCount = response.data.followersCount;
    }
    
    // 触发全局状态变化事件
    const eventType = currentFollowingState ? 'unfollow' : 'follow';
    window.dispatchEvent(new CustomEvent('followStatusChange', {
      detail: {
        type: eventType,
        userId: props.userId,
        targetUsername: userProfile.value?.username,
        source: 'UserProfile' // 标识事件来源
      }
    }));
    console.log(`📡 [UserProfile] 已发送关注状态变化事件: ${eventType}, userId: ${props.userId}`);
    
    console.log('✅ [UserProfile] 关注状态更新成功:', response.data);
  } catch (error) {
    console.error('❌ [UserProfile] 关注操作失败:', error);
    
    // 详细的错误处理
    if (error.response?.status === 400) {
      const errorMsg = error.response?.data?.message || '';
      if (errorMsg.includes('未关注该用户') || errorMsg.includes('已关注该用户')) {
        // 状态不一致，重新同步
        console.log('🔄 [UserProfile] 检测到状态不一致，重新获取最新状态');
        await checkFollowStatus();
        ElMessage.warning('状态已同步，请重试操作');
      } else {
        ElMessage.error(errorMsg || '操作失败');
      }
    } else if (error.response?.status === 401) {
      ElMessage.error('登录已过期，请重新登录');
    } else if (error.response?.status === 404) {
      ElMessage.error('用户不存在');
    } else {
      ElMessage.error('网络错误，请稍后重试');
    }
  } finally {
    isFollowLoading.value = false;
  }
};

// --- 全局关注状态变化监听 ---
const handleGlobalFollowStatusChange = async (event) => {
  const { type, userId, source } = event.detail;
  console.log(`📡 [UserProfile] 收到全局关注状态变化: ${type}, userId: ${userId}, source: ${source}, 当前页面userId: ${props.userId}`);
  
  // 避免处理自己发送的事件，防止事件循环
  if (source === 'UserProfile' && userId === props.userId) {
    console.log('🔄 [UserProfile] 忽略自己发送的事件，避免循环');
    return;
  }
  
  // 只处理当前页面用户的状态变化
  if (userId === props.userId) {
    console.log(`🎯 [UserProfile] 状态变化匹配当前用户，更新UI状态`);
    
    if (type === 'follow') {
      followInfo.value.isFollowing = true;
      followInfo.value.followersCount++;
      if (userProfile.value) {
        userProfile.value.followersCount++;
      }
      console.log(`✅ [UserProfile] 已更新为关注状态，粉丝数: ${followInfo.value.followersCount}`);
    } else if (type === 'unfollow') {
      followInfo.value.isFollowing = false;
      followInfo.value.followersCount = Math.max(0, followInfo.value.followersCount - 1);
      if (userProfile.value) {
        userProfile.value.followersCount = Math.max(0, userProfile.value.followersCount - 1);
      }
      console.log(`✅ [UserProfile] 已更新为未关注状态，粉丝数: ${followInfo.value.followersCount}`);
    }
  }
};

// --- 生命周期 ---
onMounted(async () => {
  console.log('🚀 [UserProfile] 组件开始挂载, userId:', props.userId);
  
  if (!props.userId) {
    ElMessage.error('用户ID不能为空');
    return;
  }
  
  await fetchUserProfile();
  await fetchUserArtworks();
  await checkFollowStatus();
  
  // 添加全局关注状态变化监听
  window.addEventListener('followStatusChange', handleGlobalFollowStatusChange);
  console.log('📡 [UserProfile] 已添加全局关注状态变化监听');
  
  console.log('✅ [UserProfile] 组件挂载完成');
});

// 组件卸载时移除事件监听
onUnmounted(() => {
  window.removeEventListener('followStatusChange', handleGlobalFollowStatusChange);
  console.log('📡 [UserProfile] 已移除全局关注状态变化监听');
});

// 监听userId变化，重新加载数据
watchEffect(async () => {
  if (props.userId) {
    console.log('👀 [UserProfile] userId变化，重新加载数据:', props.userId);
    currentPage.value = 1;
    await fetchUserProfile();
    await fetchUserArtworks();
    await checkFollowStatus();
  }
});
</script>

<template>
  <div class="user-profile-container">
    <!-- 用户信息卡片 -->
    <el-card class="user-info-section" shadow="always" v-loading="isLoading" element-loading-text="正在加载用户信息...">
      <div v-if="userProfile" class="user-info">
        <div class="user-avatar">
          <el-avatar :size="80" :icon="User" />
        </div>
        <div class="user-details">
          <div class="user-header">
            <h2>{{ userProfile.username }}</h2>
            <el-button 
              v-if="isLoggedIn"
              :type="followInfo.isFollowing ? 'default' : 'primary'"
              :icon="followInfo.isFollowing ? Check : Plus"
              :loading="isFollowLoading"
              @click="toggleFollow"
              class="follow-btn"
            >
              {{ followInfo.isFollowing ? '已关注' : '关注' }}
            </el-button>
          </div>
          <p class="join-date" v-if="userProfile.joinDate">
            <el-icon><Calendar /></el-icon>
            加入时间: {{ formatDate(userProfile.joinDate) }}
          </p>
        </div>
        <div class="user-stats">
          <div class="stat-item">
            <div class="stat-number">{{ userProfile.totalArtworks || 0 }}</div>
            <div class="stat-label">总作品</div>
          </div>
          <div class="stat-item">
            <div class="stat-number">{{ userProfile.sharedArtworks || 0 }}</div>
            <div class="stat-label">公开作品</div>
          </div>
          <div class="stat-item">
            <div class="stat-number">{{ userProfile.followingCount || 0 }}</div>
            <div class="stat-label">关注</div>
          </div>
          <div class="stat-item">
            <div class="stat-number">{{ followInfo.followersCount || userProfile.followersCount || 0 }}</div>
            <div class="stat-label">粉丝</div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 用户公开作品区域 -->
    <el-card class="artworks-section" shadow="always" v-loading="isLoadingArtworks" element-loading-text="正在加载作品...">
      <template #header>
        <div class="section-header">
          <div class="header-left">
            <el-icon><Picture /></el-icon>
            <span>公开作品</span>
            <el-tag v-if="artworkList.totalCount" type="info" size="small">
              {{ artworkList.totalCount }}
            </el-tag>
          </div>
        </div>
      </template>

      <!-- 作品列表 -->
      <div v-if="artworkList.artworks.length === 0" class="empty-state">
        <el-empty description="该用户还没有公开作品" />
      </div>
      <div v-else class="artworks-grid">
        <div 
          v-for="artwork in artworkList.artworks" 
          :key="artwork.id"
          class="artwork-card"
          @click="viewArtworkDetail(artwork)"
        >
          <!-- 图片 -->
          <div class="artwork-image-container">
            <img 
              :src="`/api/v1/images/${artwork.storedFilename}`" 
              :alt="artwork.prompt"
              class="artwork-image"
              @error="handleImageError"
            />
            
            <!-- 分享标识 -->
            <div class="shared-badge">
              <el-icon><Share /></el-icon>
              <span>公开</span>
            </div>
            
            <div class="artwork-overlay">
              <el-button 
                type="primary" 
                size="small" 
                @click.stop="viewArtworkDetail(artwork)"
                :icon="Picture"
                circle
              />
            </div>
          </div>
          
          <!-- 作品信息 -->
          <div class="artwork-info">
            <p class="artwork-prompt">{{ artwork.prompt.substring(0, 60) }}...</p>
            <div class="artwork-meta">
              <el-tag size="small">{{ artwork.steps }} steps</el-tag>
              <span class="artwork-date">{{ formatDate(artwork.createdAt) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 分页器 -->
      <div v-if="artworkList.totalPages > 1" class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="artworkList.totalCount"
          layout="prev, pager, next, jumper"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 作品详情模态框 -->
    <ArtworkDetailModal
      v-model:visible="detailModalVisible"
      :artwork="selectedArtwork"
      @copy-params="handleCopyParams"
    />
  </div>
</template>

<style scoped>
.user-profile-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 用户信息区域 */
.user-info-section {
  margin-bottom: 20px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 20px;
}

.user-avatar {
  flex-shrink: 0;
}

.user-details {
  flex: 1;
}

.user-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.user-details h2 {
  margin: 0;
  color: #303133;
  font-size: 24px;
}

.follow-btn {
  margin-left: 16px;
}

.join-date {
  display: flex;
  align-items: center;
  gap: 5px;
  color: #909399;
  margin: 5px 0;
  font-size: 14px;
}

.user-stats {
  display: flex;
  gap: 30px;
  margin-left: 20px;
}

.stat-item {
  text-align: center;
}

.stat-number {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
  line-height: 1;
}

.stat-label {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}

/* 作品区域 */
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.artworks-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
  margin-top: 20px;
}

.artwork-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  cursor: pointer;
  background: white;
}

.artwork-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
}

.artwork-image-container {
  position: relative;
  width: 100%;
  height: 200px;
  overflow: hidden;
}

.artwork-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.artwork-card:hover .artwork-image {
  transform: scale(1.05);
}

/* 公开标识 */
.shared-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  background: linear-gradient(135deg, #67c23a, #85ce61);
  color: white;
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 4px;
  box-shadow: 0 2px 8px rgba(103, 194, 58, 0.3);
  z-index: 2;
}

.shared-badge .el-icon {
  font-size: 12px;
}

.artwork-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.artwork-card:hover .artwork-overlay {
  opacity: 1;
}

.artwork-info {
  padding: 15px;
}

.artwork-prompt {
  font-size: 14px;
  color: #303133;
  margin-bottom: 10px;
  line-height: 1.4;
}

.artwork-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.artwork-date {
  font-size: 12px;
  color: #909399;
  margin-left: auto;
}

/* 分页器 */
.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 60px 20px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .user-info {
    flex-direction: column;
    text-align: center;
  }
  
  .user-header {
    flex-direction: column;
    gap: 12px;
  }
  
  .follow-btn {
    margin-left: 0;
  }
  
  .user-stats {
    margin-left: 0;
    justify-content: center;
  }
  
  .artworks-grid {
    grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
    gap: 15px;
  }
}
</style>
