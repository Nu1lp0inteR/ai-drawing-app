<script setup>
import { ref, onMounted, onActivated } from 'vue';
import axios from 'axios';
import { ElMessage } from 'element-plus';
import { User, Picture } from '@element-plus/icons-vue';
import ArtworkDetailModal from '@/components/ArtworkDetailModal.vue';

// --- 状态管理 ---
const galleryItems = ref([]); // Use a ref to store the list of drawings from the backend.
const isLoading = ref(true); // Control the loading state.
const backendBaseUrl = 'http://localhost:8080'; // Define the backend base URL for constructing image paths.

// 模态框相关状态
const detailModalVisible = ref(false);
const selectedArtwork = ref(null);

// --- 函数 ---

// 企业级API调用 - 获取画廊数据
const fetchGallery = async () => {
  try {
    isLoading.value = true;
    console.log('[Gallery] 🔄 开始获取画廊数据...');
    // 使用修复后的混合API - 支持数据库数据和回退机制
    console.log('[Gallery] 📡 请求URL:', `${backendBaseUrl}/api/v1/hybrid/gallery`);
    
    const response = await axios.get(`${backendBaseUrl}/api/v1/hybrid/gallery`);
    
    console.log('[Gallery] ✅ API响应状态:', response.status);
    console.log('[Gallery] 📦 响应数据:', response.data);
    console.log('[Gallery] 📊 获取到图片数量:', response.data?.length || 0);
    
    galleryItems.value = response.data || [];
    
    if (galleryItems.value.length === 0) {
      console.log('[Gallery] ℹ️ 画廊暂无图片');
    } else {
      console.log('[Gallery] 🖼️ 画廊图片列表:', galleryItems.value.map(item => ({
        id: item.id,
        shared: item.sharedToGallery,
        filename: item.storedFilename
      })));
    }
    
  } catch (error) {
    console.error('[Gallery] ❌ 获取画廊数据失败:', error);
    console.error('[Gallery] 🔍 错误详情:', {
      message: error.message,
      status: error.response?.status,
      statusText: error.response?.statusText,
      data: error.response?.data,
      url: error.config?.url
    });
    
    if (error.response?.status === 500) {
      ElMessage.error("服务器内部错误，请检查后端服务状态");
    } else if (error.response?.status === 404) {
      ElMessage.error("API接口不存在，请检查后端配置");
    } else if (error.code === 'NETWORK_ERROR') {
      ElMessage.error("网络连接失败，请检查后端是否启动");
    } else {
      ElMessage.error(`获取画廊数据失败: ${error.message}`);
    }
  } finally {
    isLoading.value = false;
    console.log('[Gallery] 🏁 数据加载完成');
  }
};

// Constructs the full URL for an image.
const getImageUrl = (filename) => {
  return `${backendBaseUrl}/api/v1/images/${filename}`;
};

// 显示作品详情 - 企业级用户体验设计
const showArtworkDetail = (artwork) => {
  selectedArtwork.value = artwork;
  detailModalVisible.value = true;
};

// 处理参数复用 - 跳转到创作中心并填充参数
const handleCopyParams = (params) => {
  // 由于没有使用Vue Router，直接使用事件或者localStorage传递参数
  localStorage.setItem('autoFillParams', JSON.stringify(params));
  
  // 触发切换到Studio视图的事件
  window.dispatchEvent(new CustomEvent('switchToStudio', { detail: { autoFill: true } }));
  
  ElMessage.success('即将跳转到创作中心并自动填充参数');
};

// --- Vue 生命周期钩子 ---

// 初次挂载时获取画廊数据
onMounted(() => {
  console.log('[Gallery] Component mounted, fetching gallery data...');
  fetchGallery();
});

// 每次激活组件时重新获取画廊数据（解决keep-alive缓存问题）
onActivated(() => {
  console.log('[Gallery] Component activated, refreshing gallery data...');
  fetchGallery();
});
</script>

<template>
  <div class="gallery-container" v-loading="isLoading" element-loading-text="正在加载作品...">
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
              <span class="author-name">{{ item.authorName || '匿名用户' }}</span>
            </div>
            
            <div class="item-actions">
              <el-button type="primary" size="small" @click.stop="showArtworkDetail(item)">
                查看详情
              </el-button>
            </div>
          </div>
        </el-card>
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

.item-actions {
  display: flex;
  justify-content: center;
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
</style>