<!--
  作品详情模态框组件
  企业级组件设计说明：
  1. 使用组合式API确保代码的可维护性和类型安全
  2. 响应式设计，适配移动端和桌面端
  3. 优雅的加载状态和错误处理
  4. 可复用的组件设计，支持外部传入数据或通过API获取
-->

<script setup>
import { ref, computed, watch, inject } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/api'

const isLoggedIn = inject('isLoggedIn')
const userInfo = inject('userInfo')

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

// 计算属性
const dialogVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value)
})

// 获取图片URL
const getImageUrl = (filename) => {
  if (!filename) return ''
  return `/api/v1/images/${filename}`
}

// 获取作品详情 - 企业级错误处理和用户体验
const fetchArtworkDetails = async (id) => {
  if (!id) return
  
  try {
    loading.value = true
    const response = await api.get(`/api/v1/ai-drawing/${id}`)
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

function downloadImage() {
  if (!artworkData.value?.storedFilename) {
    ElMessage.warning('没有可下载的图片');
    return;
  }
  const link = document.createElement('a');
  link.href = getImageUrl(artworkData.value.storedFilename);
  link.download = artworkData.value.storedFilename;
  link.click();
}

function copyPromptText() {
  if (!artworkData.value?.prompt) {
    ElMessage.warning('没有可复制的提示词');
    return;
  }
  navigator.clipboard.writeText(artworkData.value.prompt).then(() => {
    ElMessage.success('提示词已复制到剪贴板');
  }).catch(() => {
    ElMessage.error('复制失败');
  });
}

const comments = ref([])
const commentTotalPages = ref(0)
const commentPage = ref(0)
const commentsLoading = ref(false)
const newCommentText = ref('')
const submittingComment = ref(false)
const commentPageSize = 10

async function fetchComments(reset = false) {
  const drawingId = (artworkData.value || props.artwork)?.id
  if (!drawingId) return

  if (reset) { commentPage.value = 0; comments.value = [] }
  commentsLoading.value = true

  try {
    const response = await api.get(`/api/v1/comments/drawings/${drawingId}`, {
      params: { page: commentPage.value, size: commentPageSize }
    })
    const data = response.data
    comments.value = reset ? (data.comments || []) : [...comments.value, ...(data.comments || [])]
    commentTotalPages.value = data.total_pages || 0
  } catch (error) {
    console.error('获取评论失败:', error)
  } finally {
    commentsLoading.value = false
  }
}

async function submitComment() {
  if (!newCommentText.value.trim()) return
  submittingComment.value = true
  try {
    await api.post('/api/v1/comments', {
      drawing_id: (artworkData.value || props.artwork).id,
      content: newCommentText.value.trim()
    })
    newCommentText.value = ''
    ElMessage.success('评论发表成功')
    fetchComments(true)
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '评论发表失败')
  } finally {
    submittingComment.value = false
  }
}

async function deleteCommentItem(commentId) {
  try {
    await api.delete(`/api/v1/comments/${commentId}`)
    comments.value = comments.value.filter(c => c.id !== commentId)
    ElMessage.success('评论已删除')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '删除失败')
  }
}

function loadMoreComments() {
  if (commentPage.value < commentTotalPages.value - 1) {
    commentPage.value++
    fetchComments(false)
  }
}

async function handleDelete() {
  const drawingId = (artworkData.value || props.artwork)?.id
  if (!drawingId) return
  try {
    await ElMessageBox.confirm('确定要删除此作品吗？删除后无法恢复。', '确认删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    });
  } catch {
    return;
  }
  try {
    await api.delete(`/api/v1/ai-drawing/${drawingId}`)
    ElMessage.success('作品已删除')
    dialogVisible.value = false
    window.dispatchEvent(new CustomEvent('drawingDeleted', { detail: { id: drawingId } }))
  } catch (error) {
    console.error('删除失败:', error)
    ElMessage.error('删除失败')
  }
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
  if (visible) {
    fetchComments(true)
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

      <div class="comments-section">
        <h3 class="section-title">评论 ({{ comments.length }})</h3>

        <div v-if="isLoggedIn" class="comment-input-area">
          <el-input
            v-model="newCommentText"
            type="textarea"
            :rows="2"
            placeholder="写下你的评论..."
            maxlength="500"
            show-word-limit
          />
          <el-button
            type="primary"
            size="small"
            :loading="submittingComment"
            :disabled="!newCommentText.trim()"
            @click="submitComment"
            style="margin-top: 8px"
          >
            发表评论
          </el-button>
        </div>
        <div v-else class="comment-login-hint">
          <span>登录后即可发表评论</span>
        </div>

        <div v-if="comments.length > 0" class="comment-list">
          <div v-for="comment in comments" :key="comment.id" class="comment-item">
            <div class="comment-avatar">{{ comment.username?.charAt(0)?.toUpperCase() }}</div>
            <div class="comment-body">
              <div class="comment-header">
                <span class="comment-username">{{ comment.username }}</span>
                <span class="comment-time">{{ formatDate(comment.created_at) }}</span>
              </div>
              <p class="comment-text">{{ comment.content }}</p>
            </div>
            <el-button
              v-if="isLoggedIn && comment.user_id === userInfo?.id"
              size="small"
              text
              type="danger"
              class="comment-delete-btn"
              @click="deleteCommentItem(comment.id)"
            >
              删除
            </el-button>
          </div>
        </div>

        <div v-if="commentPage < commentTotalPages - 1" class="comment-load-more">
          <el-button size="small" text :loading="commentsLoading" @click="loadMoreComments">
            加载更多评论
          </el-button>
        </div>

        <div v-if="comments.length === 0 && !commentsLoading" class="comment-empty">
          <span>暂无评论，来发表第一条吧</span>
        </div>
      </div>
    </div>
    
    <!-- 错误状态 -->
    <div v-else class="error-container">
      <el-empty description="作品数据加载失败" />
    </div>
    
    <!-- 底部操作按钮 -->
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="dialogVisible = false">关闭</el-button>
        <el-button @click="downloadImage">
          下载原图
        </el-button>
        <el-button @click="copyPromptText">
          复制提示词
        </el-button>
        <el-button type="primary" @click="copyParameters">
          复用参数到创作中心
        </el-button>
        <el-button
          v-if="userInfo && userInfo.id === ((artworkData || artwork)?.authorId)"
          type="danger"
          @click="handleDelete"
        >
          删除作品
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

.comments-section {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.comment-input-area {
  margin-bottom: 16px;
}

.comment-login-hint {
  text-align: center;
  padding: 12px;
  color: #999;
  font-size: 13px;
  background: var(--el-fill-color-lighter);
  border-radius: 6px;
  margin-bottom: 12px;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.comment-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid var(--el-border-color-extra-light);
}

.comment-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  flex-shrink: 0;
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.comment-username {
  font-size: 13px;
  font-weight: 600;
  color: #333;
}

.comment-time {
  font-size: 11px;
  color: #999;
}

.comment-text {
  margin: 0;
  font-size: 13px;
  color: #555;
  line-height: 1.5;
  word-break: break-word;
}

.comment-delete-btn {
  flex-shrink: 0;
  font-size: 12px;
}

.comment-load-more {
  text-align: center;
  padding: 8px 0;
}

.comment-empty {
  text-align: center;
  padding: 20px;
  color: #999;
  font-size: 13px;
}
</style>


