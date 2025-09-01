<!--
  作品详情模态框组件
  企业级组件设计说明：
  1. 使用组合式API确保代码的可维护性和类型安全
  2. 响应式设计，适配移动端和桌面端
  3. 优雅的加载状态和错误处理
  4. 可复用的组件设计，支持外部传入数据或通过API获取
-->

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

// Props定义 - 支持灵活的使用方式
const props = defineProps({
  // 是否显示模态框
  visible: {
    type: Boolean,
    default: false
  },
  // 作品ID - 如果提供了ID，会自动获取详情
  artworkId: {
    type: String,
    default: ''
  },
  // 直接传入的作品数据 - 如果已有数据，可直接使用
  artwork: {
    type: Object,
    default: null
  }
})

// Emits定义 - 父组件通信
const emit = defineEmits(['update:visible', 'copy-params'])

// 响应式状态
const loading = ref(false)
const artworkData = ref(null)
const imageLoading = ref(true)
const backendBaseUrl = 'http://localhost:8080'

// 计算属性
const dialogVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value)
})

// 获取图片URL
const getImageUrl = (filename) => {
  if (!filename) return ''
  return `${backendBaseUrl}/api/v1/images/${filename}`
}

// 获取作品详情 - 企业级错误处理和用户体验
const fetchArtworkDetails = async (id) => {
  if (!id) return
  
  try {
    loading.value = true
    const response = await axios.get(`${backendBaseUrl}/api/v1/ai-drawing/${id}`)
    artworkData.value = response.data
  } catch (error) {
    console.error('Failed to fetch artwork details:', error)
    if (error.response?.status === 404) {
      ElMessage.error('作品不存在或已被删除')
    } else {
      ElMessage.error('获取作品详情失败，请稍后重试')
    }
    // 获取失败时自动关闭模态框
    dialogVisible.value = false
  } finally {
    loading.value = false
  }
}

// 复制参数到创作中心 - 企业级功能设计
const copyParameters = () => {
  const currentArtwork = artworkData.value || props.artwork
  if (!currentArtwork) return
  
  const params = {
    prompt: currentArtwork.prompt || '',
    negativePrompt: currentArtwork.negativePrompt || '',
    steps: currentArtwork.steps || 20,
    cfg: currentArtwork.cfg || 7.0,
    samplerName: currentArtwork.samplerName || 'Euler',
    seed: currentArtwork.seed || ''
  }
  
  emit('copy-params', params)
  ElMessage.success('参数已复制！即将跳转到创作中心')
  dialogVisible.value = false
}

// 格式化时间显示
const formatDate = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 监听器 - 当模态框打开且有artworkId时自动获取详情
watch([() => props.visible, () => props.artworkId], ([visible, id]) => {
  if (visible && id && !props.artwork) {
    fetchArtworkDetails(id)
  }
  if (visible && props.artwork) {
    artworkData.value = props.artwork
  }
})

// 图片加载完成处理
const handleImageLoad = () => {
  imageLoading.value = false
}

// 图片加载失败处理
const handleImageError = () => {
  imageLoading.value = false
  ElMessage.error('图片加载失败')
}
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    title="作品详情"
    width="900px"
    :close-on-click-modal="false"
    class="artwork-detail-modal"
    @close="imageLoading = true"
  >
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="8" animated />
    </div>
    
    <!-- 主要内容 -->
    <div v-else-if="artworkData || artwork" class="artwork-detail-content">
      <el-row :gutter="24">
        <!-- 左侧：图片展示 -->
        <el-col :xs="24" :md="12">
          <div class="image-container">
            <el-image
              :src="getImageUrl((artworkData || artwork).storedFilename)"
              fit="contain"
              class="artwork-image"
              :preview-src-list="[getImageUrl((artworkData || artwork).storedFilename)]"
              preview-teleported
              @load="handleImageLoad"
              @error="handleImageError"
            >
              <template #placeholder>
                <div class="image-loading">
                  <el-skeleton-item variant="image" style="width: 100%; height: 100%;" />
                </div>
              </template>
              <template #error>
                <div class="image-error">
                  <el-icon size="50"><Picture /></el-icon>
                  <p>图片加载失败</p>
                </div>
              </template>
            </el-image>
          </div>
        </el-col>
        
        <!-- 右侧：参数详情 -->
        <el-col :xs="24" :md="12">
          <div class="params-container">
            <h3 class="section-title">生成参数</h3>
            
            <!-- 提示词 -->
            <el-form-item label="正向提示词：" class="param-item">
              <el-input
                :value="(artworkData || artwork).prompt"
                type="textarea"
                :rows="3"
                readonly
                class="readonly-input"
              />
            </el-form-item>
            
            <!-- 负向提示词 -->
            <el-form-item 
              v-if="(artworkData || artwork).negativePrompt"
              label="负向提示词："
              class="param-item"
            >
              <el-input
                :value="(artworkData || artwork).negativePrompt"
                type="textarea"
                :rows="2"
                readonly
                class="readonly-input"
              />
            </el-form-item>
            
            <!-- 技术参数网格 -->
            <div class="tech-params-grid">
              <div class="param-group">
                <label>采样步数</label>
                <span class="param-value">{{ (artworkData || artwork).steps || '-' }}</span>
              </div>
              
              <div class="param-group">
                <label>CFG Scale</label>
                <span class="param-value">{{ (artworkData || artwork).cfg || '-' }}</span>
              </div>
              
              <div class="param-group">
                <label>采样器</label>
                <span class="param-value">{{ (artworkData || artwork).samplerName || '-' }}</span>
              </div>
              
              <div class="param-group">
                <label>随机种子</label>
                <span class="param-value">{{ (artworkData || artwork).seed || '-' }}</span>
              </div>
            </div>
            
            <!-- 元数据信息 -->
            <div class="metadata-section">
              <h4 class="subsection-title">作品信息</h4>
              <div class="metadata-item">
                <span class="metadata-label">创建时间：</span>
                <span class="metadata-value">{{ formatDate((artworkData || artwork).createdAt) }}</span>
              </div>
              <div class="metadata-item">
                <span class="metadata-label">文件类型：</span>
                <span class="metadata-value">{{ (artworkData || artwork).fileType || 'image/png' }}</span>
              </div>
              <div class="metadata-item">
                <span class="metadata-label">作品ID：</span>
                <span class="metadata-value">{{ (artworkData || artwork).id }}</span>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>
    
    <!-- 错误状态 -->
    <div v-else class="error-container">
      <el-empty description="作品数据加载失败" />
    </div>
    
    <!-- 底部操作按钮 -->
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="dialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="copyParameters">
          <el-icon><CopyDocument /></el-icon>
          复用参数到创作中心
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.artwork-detail-modal {
  --el-dialog-border-radius: 12px;
}

.loading-container {
  padding: 20px;
}

.artwork-detail-content {
  max-height: 70vh;
  overflow-y: auto;
}

.image-container {
  position: relative;
  border-radius: 8px;
  overflow: hidden;
  background: var(--el-fill-color-lighter);
  min-height: 300px;
}

.artwork-image {
  width: 100%;
  max-height: 500px;
  border-radius: 8px;
}

.image-loading,
.image-error {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 300px;
  color: var(--el-text-color-placeholder);
}

.params-container {
  padding-left: 20px;
}

.section-title {
  margin: 0 0 20px 0;
  color: var(--el-text-color-primary);
  font-size: 18px;
  font-weight: 600;
  border-bottom: 2px solid var(--el-color-primary);
  padding-bottom: 8px;
}

.param-item {
  margin-bottom: 16px;
}

.param-item :deep(.el-form-item__label) {
  font-weight: 600;
  color: var(--el-text-color-regular);
}

.readonly-input :deep(.el-textarea__inner) {
  background-color: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color-lighter);
  cursor: default;
}

.tech-params-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin: 20px 0;
  padding: 16px;
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
}

.param-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.param-group label {
  font-size: 13px;
  color: var(--el-text-color-regular);
  font-weight: 500;
}

.param-value {
  font-size: 14px;
  color: var(--el-text-color-primary);
  font-weight: 600;
  padding: 4px 8px;
  background: white;
  border-radius: 4px;
  border: 1px solid var(--el-border-color-light);
}

.metadata-section {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.subsection-title {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-regular);
}

.metadata-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
  border-bottom: 1px solid var(--el-border-color-extra-light);
}

.metadata-item:last-child {
  border-bottom: none;
}

.metadata-label {
  font-size: 13px;
  color: var(--el-text-color-regular);
}

.metadata-value {
  font-size: 13px;
  color: var(--el-text-color-primary);
  font-family: monospace;
}

.error-container {
  text-align: center;
  padding: 40px 20px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .params-container {
    padding-left: 0;
    margin-top: 20px;
  }
  
  .tech-params-grid {
    grid-template-columns: 1fr;
  }
  
  .artwork-detail-modal :deep(.el-dialog) {
    width: 95vw !important;
    margin: 5vh auto !important;
  }
  
  .artwork-detail-content {
    max-height: 80vh;
  }
}
</style>


