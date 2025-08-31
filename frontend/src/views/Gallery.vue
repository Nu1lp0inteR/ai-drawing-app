<script setup>
import { ref, onMounted } from 'vue';
import axios from 'axios';
import { ElMessage } from 'element-plus';

// --- 状态管理 ---
const galleryItems = ref([]); // Use a ref to store the list of drawings from the backend.
const isLoading = ref(true); // Control the loading state.
const backendBaseUrl = 'http://localhost:8080'; // Define the backend base URL for constructing image paths.

// --- 函数 ---

// Fetches the drawing history from the backend API.
const fetchGallery = async () => {
  try {
    isLoading.value = true;
    const response = await axios.get(`${backendBaseUrl}/api/v1/ai-drawing/history`);
    galleryItems.value = response.data;
    console.log("Fetched gallery items:", galleryItems.value);
  } catch (error) {
    console.error("Failed to fetch gallery:", error);
    ElMessage.error("获取画廊数据失败！");
  } finally {
    isLoading.value = false;
  }
};

// Constructs the full URL for an image.
const getImageUrl = (filename) => {
  return `${backendBaseUrl}/api/v1/images/${filename}`;
};

// --- Vue 生命周期钩子 ---

// When the component is mounted, fetch the initial gallery data.
onMounted(() => {
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
        <el-card v-for="item in galleryItems" :key="item.id" shadow="hover" class="gallery-item">
          <el-image :src="getImageUrl(item.storedFilename)" fit="cover" lazy>
            <template #placeholder>
              <div class="image-slot">加载中<span class="dot">...</span></div>
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
          </div>
        </el-card>
      </div>
    </el-scrollbar>
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
  background: linear-gradient(to top, rgba(0,0,0,0.8), transparent);
  color: white;
  font-size: 14px;
  opacity: 0;
  transition: opacity 0.3s ease;
}
.gallery-item:hover .item-info {
  opacity: 1;
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