<script setup>
import { ref, onMounted, onActivated, onUnmounted, inject } from 'vue';
import api from '@/api';
import { ElMessage, ElButton, ElIcon } from 'element-plus';
import { User, Picture } from '@element-plus/icons-vue';
import ArtworkDetailModal from '@/components/ArtworkDetailModal.vue';

const galleryItems = ref([]);
const isLoading = ref(true);
const galleryMode = ref('latest'); // 'latest' | 'hot'

// 模态框相关状态
const detailModalVisible = ref(false);
const selectedArtwork = ref(null);

// 注入导航函数和登录状态
const navigateToUserProfile = inject('navigateToUserProfile');
const navigateTo = inject('navigateTo');
const isLoggedIn = inject('isLoggedIn');
const userInfo = inject('userInfo');

// 点赞相关状态
const likingItems = ref(new Set()); // 正在进行点赞操作的作品ID集合

const galleryPage = ref(0)
const galleryTotalPages = ref(0)
const pageSize = 20

const keyword = ref('')
const selectedModel = ref('')
const availableModels = ref([])

const fetchAvailableModels = async () => {
  try {
    const response = await api.get('/api/v1/ai-drawing/models');
    availableModels.value = response.data.models || [];
  } catch (error) {
    console.error('获取模型列表失败:', error);
  }
};

const fetchGallery = async (page = 0) => {
  try {
    isLoading.value = true;
    const endpoint = galleryMode.value === 'hot' ? '/api/v1/gallery/hot' : '/api/v1/gallery';
    const params = { page, size: pageSize };
    if (keyword.value.trim()) params.keyword = keyword.value.trim();
    if (selectedModel.value) params.model = selectedModel.value;
    const response = await api.get(endpoint, { params });
    
    const data = response.data;
    if (page === 0) {
      galleryItems.value = data.content || [];
    } else {
      galleryItems.value = [...galleryItems.value, ...(data.content || [])];
    }
    galleryPage.value = data.page;
    galleryTotalPages.value = data.totalPages;
    
  } catch (error) {
    console.error('[Gallery] 获取画廊数据失败:', error);
    ElMessage.error('获取画廊数据失败');
  } finally {
    isLoading.value = false;
  }
};

const switchMode = (mode) => {
  if (mode === galleryMode.value) return;
  galleryMode.value = mode;
  galleryPage.value = 0;
  galleryTotalPages.value = 0;
  fetchGallery(0).then(() => setupObserver());
};

// Constructs the full URL for an image.
const getImageUrl = (filename) => {
  return `/api/v1/images/${filename}`;
};

// 显示作品详情 - 企业级用户体验设计
const showArtworkDetail = (artwork) => {
  selectedArtwork.value = artwork;
  detailModalVisible.value = true;
};

const handleCopyParams = (params) => {
  sessionStorage.setItem('prefillStudioParams', JSON.stringify(params));
  navigateTo('Studio');
  ElMessage.success('已跳转到创作中心，参数已自动填充');
};

// 跳转到用户主页
const goToUserProfile = (authorId, authorName) => {
  if (!authorId) {
    ElMessage.warning('该作品没有作者信息');
    return;
  }
  
  console.log('🔄 [Gallery] 跳转到用户主页:', authorName, authorId);
  navigateToUserProfile(authorId);
};

// 切换点赞状态
const toggleLike = async (item) => {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录后再点赞');
    return;
  }
  
  if (likingItems.value.has(item.id)) {
    return; // 防止重复点击
  }
  
  try {
    likingItems.value.add(item.id);
    console.log('👍 [Gallery] 切换点赞状态:', item.id, item.isLiked);
    
    let response;
    
    if (item.isLiked) {
      response = await api.delete(`/api/v1/likes/drawings/${item.id}`);
    } else {
      response = await api.post(`/api/v1/likes/drawings/${item.id}`);
    }
    
    // 更新本地状态
    item.isLiked = response.data.isLiked;
    item.likesCount = response.data.likesCount;
    
    console.log('✅ [Gallery] 点赞状态更新成功:', response.data);
    
  } catch (error) {
    console.error('❌ [Gallery] 点赞操作失败:', error);
    
    if (error.response?.status === 401 || error.response?.status === 403) {
      ElMessage.error('登录已过期，请重新登录');
    } else if (error.response?.status === 400) {
      ElMessage.error(error.response.data.message || '操作失败');
    } else {
      ElMessage.error('操作失败，请稍后重试');
    }
  } finally {
    likingItems.value.delete(item.id);
  }
};

const handleDeleteDrawing = async (item, event) => {
  event.stopPropagation();
  if (!confirm('确定要删除此作品吗？')) return;
  try {
    await api.delete(`/api/v1/ai-drawing/${item.id}`);
    galleryItems.value = galleryItems.value.filter(i => i.id !== item.id);
    ElMessage.success('作品已删除');
  } catch (error) {
    console.error('删除失败:', error);
    ElMessage.error('删除失败');
  }
};

// --- Vue 生命周期钩子 ---

const loadMoreRef = ref(null);
let isLoadingMore = false;

function loadMore() {
  if (!isLoadingMore && galleryPage.value < galleryTotalPages.value - 1) {
    isLoadingMore = true;
    fetchGallery(galleryPage.value + 1).finally(() => { isLoadingMore = false; });
  }
}

let saveObserver = null;

function setupObserver() {
  if (saveObserver) saveObserver.disconnect();
  saveObserver = new IntersectionObserver((entries) => {
    if (entries[0].isIntersecting && !isLoadingMore) {
      loadMore();
    }
  }, { threshold: 0.1 });
  if (loadMoreRef.value) saveObserver.observe(loadMoreRef.value);
}

function onDrawingDeleted() {
  fetchGallery(0).then(() => setupObserver());
}

onMounted(() => {
  fetchAvailableModels();
  fetchGallery(0).then(() => setupObserver());
  window.addEventListener('drawingDeleted', onDrawingDeleted);
});

onActivated(() => {
  fetchAvailableModels();
  fetchGallery(0).then(() => setupObserver());
});

onUnmounted(() => {
  if (saveObserver) saveObserver.disconnect();
  window.removeEventListener('drawingDeleted', onDrawingDeleted);
});

</script>

<template>
  <div class="gallery-container" v-loading="isLoading" element-loading-text="正在加载作品...">
    <div class="gallery-tabs">
      <button class="tab-btn" :class="{ active: galleryMode === 'latest' }" @click="switchMode('latest')">最新</button>
      <button class="tab-btn" :class="{ active: galleryMode === 'hot' }" @click="switchMode('hot')">热门</button>
    </div>
    <div class="gallery-search-bar">
      <el-input
        v-model="keyword"
        placeholder="搜索提示词..."
        clearable
        @clear="fetchGallery(0).then(() => setupObserver())"
        @keyup.enter="fetchGallery(0).then(() => setupObserver())"
        style="flex: 1; max-width: 300px;"
      />
      <el-select
        v-model="selectedModel"
        placeholder="全部模型"
        clearable
        @clear="fetchGallery(0).then(() => setupObserver())"
        style="width: 180px;"
      >
        <el-option
          v-for="m in availableModels"
          :key="m.key"
          :label="m.name"
          :value="m.key"
        />
      </el-select>
      <el-button type="primary" @click="fetchGallery(0).then(() => setupObserver())">搜索</el-button>
    </div>
    <el-scrollbar>
      <div v-if="!isLoading && galleryItems.length === 0" class="empty-state">
        <el-empty description="画廊还是空的，快去创作中心生成您的第一张作品吧！" />
      </div>
      <div v-else class="gallery-grid">
        <el-card 
          v-for="item in galleryItems" 
          :key="item.id" 
          shadow="hover" 
          class="gallery-item"
          @click="showArtworkDetail(item)"
        >
          <el-image :src="getImageUrl(item.storedFilename)" fit="cover" lazy>
            <template #placeholder>
              <div class="image-slot">加载中<span class="dot">...</span></div>
            </template>
            <template #error>
              <div class="image-slot" style="background: linear-gradient(45deg, #f0f0f0, #e0e0e0);">
                <el-icon size="60"><Picture /></el-icon>
                <p style="margin: 5px 0; font-size: 12px;">{{ item.prompt?.substring(0, 20) }}...</p>
              </div>
            </template>
          </el-image>
          <div class="item-info">
            <el-tooltip
              class="box-item"
              effect="dark"
              :content="item.prompt"
              placement="top"
            >
              <p class="prompt-text">{{ item.prompt }}</p>
            </el-tooltip>
            
            <!-- 作者信息 -->
            <div class="author-info">
              <el-icon><User /></el-icon>
              <span 
                class="author-name" 
                :class="{ 'clickable': item.authorId }"
                @click.stop="item.authorId ? goToUserProfile(item.authorId, item.authorName) : null"
              >
                {{ item.authorName || '匿名用户' }}
              </span>
            </div>
            
            <div class="item-actions">
              <!-- 点赞按钮 -->
              <el-button 
                :type="item.isLiked ? 'danger' : 'default'"
                size="small" 
                :loading="likingItems.has(item.id)"
                @click.stop="toggleLike(item)"
                class="like-button"
              >
                <span class="heart-icon" :class="{ 'liked': item.isLiked }">
                  ♥
                </span>
                <span class="like-count">{{ item.likesCount || 0 }}</span>
              </el-button>
              
              <el-button type="primary" size="small" @click.stop="showArtworkDetail(item)">
                查看详情
              </el-button>

              <el-button
                v-if="userInfo && userInfo.id === item.authorId"
                type="danger"
                size="small"
                @click.stop="handleDeleteDrawing(item, $event)"
              >
                删除
              </el-button>
            </div>
          </div>
        </el-card>
      </div>
      <div ref="loadMoreRef" class="load-more-sentinel" v-if="galleryPage < galleryTotalPages - 1">
        <span v-if="isLoading">加载中...</span>
      </div>
    </el-scrollbar>
    
    <!-- 作品详情模态框 -->
    <ArtworkDetailModal
      v-model:visible="detailModalVisible"
      :artwork="selectedArtwork"
      @copy-params="handleCopyParams"
    />
  </div>
</template>

<style scoped>
.gallery-container {
  height: 100%;
  padding: 20px;
  box-sizing: border-box;
}

.gallery-tabs {
  display: flex;
  gap: 0;
  margin-bottom: 20px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e0e0e0;
  width: fit-content;
}

.tab-btn {
  padding: 8px 24px;
  border: none;
  background: #fff;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  color: #666;
  transition: all 0.2s;
}

.tab-btn:hover {
  background: #f5f5f5;
}

.tab-btn.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.tab-btn:not(:last-child) {
  border-right: 1px solid #e0e0e0;
}

.gallery-search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  align-items: center;
}

.gallery-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}
.gallery-item {
  position: relative;
  overflow: hidden;
  cursor: pointer;
}
.gallery-item .el-image {
  width: 100%;
  height: 400px;
  display: block;
}
.item-info {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 20px 10px 10px 10px;
  background: linear-gradient(to top, rgba(0,0,0,0.85), transparent);
  color: white;
  font-size: 14px;
  opacity: 0;
  transition: opacity 0.3s ease;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.gallery-item:hover .item-info {
  opacity: 1;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.9);
  margin-top: 4px;
}

.author-info .el-icon {
  font-size: 14px;
  color: #67c23a;
}

.author-name {
  font-weight: 500;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
}

.author-name.clickable {
  cursor: pointer;
  text-decoration: underline;
  transition: color 0.3s ease;
}

.author-name.clickable:hover {
  color: #409eff;
}

.item-actions {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 4px;
}

.item-actions .el-button {
  background: rgba(255, 255, 255, 0.9);
  border: none;
  color: var(--el-color-primary);
  font-weight: 600;
  backdrop-filter: blur(4px);
  transition: all 0.3s ease;
}

.item-actions .el-button:hover {
  background: white;
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
}
.prompt-text {
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.image-slot {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-secondary);
  font-size: 14px;
}
.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  height: calc(100vh - 100px); /* Adjust based on your layout */
}

/* 点赞按钮样式 */
.like-button {
  display: flex;
  align-items: center;
  gap: 4px;
  transition: all 0.3s ease;
}

.heart-icon {
  font-size: 16px;
  color: #ccc;
  transition: all 0.3s ease;
}

.heart-icon.liked {
  color: #e74c3c;
  animation: pulse 0.6s ease-in-out;
}

.like-count {
  font-size: 12px;
  font-weight: 600;
}

/* 点赞动画 */
@keyframes pulse {
  0% { transform: scale(1); }
  50% { transform: scale(1.2); }
  100% { transform: scale(1); }
}

.gallery-item {
  animation: fadeIn 0.3s ease-in;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.gallery-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.12);
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.load-more-sentinel {
  text-align: center;
  padding: 16px;
  color: #999;
  font-size: 14px;
}
</style>