<template>
  <el-container class="studio-container">
    <el-aside width="400px" class="aside-panel">
      <el-card shadow="always" style="height: 100%">
        <template #header>
          <div class="card-header">
            <el-icon><MagicStick /></el-icon>
            <span>生成参数</span>
            <el-button
              v-if="isMobileView"
              text
              class="collapse-toggle"
              :class="{ collapsed: paramsCollapsed }"
              @click="paramsCollapsed = !paramsCollapsed"
              :icon="ArrowDown"
            >
              {{ paramsCollapsed ? '展开' : '收起' }}
            </el-button>
          </div>
        </template>
        <el-form
          v-show="!paramsCollapsed || !isMobileView"
          :model="params"
          label-position="top"
          @submit.prevent="handleSubmit"
        >
          <el-form-item label="模型选择">
            <el-select v-model="params.model_name" style="width: 100%">
              <el-option v-for="m in availableModels" :key="m.key" :label="m.name" :value="m.key" />
            </el-select>
          </el-form-item>
          <el-form-item label="正向提示词 (Prompt)">
            <el-input v-model="params.prompt" type="textarea" :rows="5" />
          </el-form-item>
          <el-form-item label="反向提示词 (Negative Prompt)">
            <el-input v-model="params.negative_prompt" type="textarea" :rows="3" />
          </el-form-item>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="采样步数">
                <el-input-number v-model="params.steps" :min="1" :max="100" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="采样器">
                <el-select v-model="params.sampler_name" style="width: 100%">
                  <el-option
                    v-for="item in samplerOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item>
            <template #label>
              <span>CFG Scale (提示词相关性): {{ params.cfg }}</span>
            </template>
            <el-slider v-model="params.cfg" :min="1" :max="20" :step="0.5" />
          </el-form-item>
          <el-form-item label="随机种子 (Seed)">
            <el-input v-model="params.seed">
              <template #append>
                <el-button :icon="Refresh" @click="handleRandomSeed" />
              </template>
            </el-input>
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              native-type="submit"
              :loading="isLoading"
              :icon="Promotion"
              style="width: 100%"
              :disabled="isLoading"
            >
              {{ isLoading ? progressStage || '生成中...' : '开始生成' }}
            </el-button>

            <!-- 取消按钮，仅在生成中显示 -->
            <el-button
              v-if="isLoading"
              type="danger"
              @click="cancelDrawing"
              style="width: 100%; margin-top: 10px"
              plain
            >
              停止等待 (后台仍在处理)
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </el-aside>

    <el-main class="main-content">
      <div
        class="image-container"
        v-loading="isLoading"
        :element-loading-text="progressStage || '正在生成中...'"
      >
        <div v-if="!imageUrl && !isLoading" class="placeholder">
          <el-icon :size="60"><IconPicture /></el-icon>
          <p>生成的图片将在这里显示</p>
        </div>
        <div v-if="imageUrl" class="image-result-container">
          <el-image
            :src="imageUrl"
            fit="contain"
            class="generated-image"
            :preview-src-list="[imageUrl]"
            preview-teleported
            @load="console.log('[Studio] 🖼️ Image loaded successfully:', imageUrl)"
            @error="console.error('[Studio] ❌ Image load failed:', imageUrl)"
          >
            <template #placeholder>
              <div class="image-placeholder-slot">
                <el-icon class="is-loading"><IconPicture /></el-icon>
              </div>
            </template>
          </el-image>

          <!-- 图片操作区域 -->
          <div v-if="isLoggedIn" class="image-actions">
            <el-button type="primary" :loading="isSharing" @click="shareToGallery" :icon="Share">
              {{ isSharing ? '分享中...' : '分享到画廊' }}
            </el-button>

            <el-button type="default" @click="downloadImage" :icon="Download"> 下载图片 </el-button>

            <el-button type="success" :loading="isLoading" @click="handleSubmit">
              再来一张 🎲
            </el-button>
          </div>
        </div>
        <div v-if="!isLoggedIn" class="login-prompt">
          <el-icon :size="60"><Lock /></el-icon>
          <p>登录后即可开始您的创作之旅</p>
          <el-button type="primary" @click="showAuthDialog">立即登录</el-button>
        </div>
      </div>
    </el-main>

    <!-- 右侧：历史图片区域 -->
    <el-aside width="300px" class="history-panel">
      <el-card shadow="always" style="height: 100%">
        <template #header>
          <div class="card-header">
            <el-icon><Clock /></el-icon>
            <span>最近生成 ({{ historyImages.length }}/12)</span>
            <el-button
              v-if="isMobileView"
              text
              class="collapse-toggle"
              :class="{ collapsed: historyCollapsed }"
              @click="historyCollapsed = !historyCollapsed"
              :icon="ArrowDown"
            >
              {{ historyCollapsed ? '展开' : '收起' }}
            </el-button>
            <el-button
              type="text"
              size="small"
              @click="clearHistory"
              v-if="historyImages.length > 0"
              style="float: right; padding: 0; color: #909399"
            >
              清空
            </el-button>
          </div>
        </template>
        <div v-show="!historyCollapsed || !isMobileView" class="history-content">
          <div v-if="historyImages.length === 0" class="history-empty">
            <el-icon :size="40" style="color: #c0c4cc"><IconPicture /></el-icon>
            <p style="color: #909399; margin-top: 10px">暂无历史记录</p>
          </div>
          <div v-else class="history-grid">
            <div
              v-for="(item, index) in historyImages"
              :key="item.id"
              class="history-item"
              @click="loadHistoryItem(item)"
              :title="`点击加载参数 - ${item.prompt.substring(0, 50)}...`"
            >
              <img :src="item.imageUrl" :alt="`历史图片 ${index + 1}`" class="history-image" />
              <div class="history-overlay">
                <el-button
                  type="primary"
                  :icon="View"
                  size="small"
                  circle
                  @click="previewHistoryImage(item, $event)"
                  title="预览大图"
                  style="margin-right: 8px"
                />
                <el-icon><MagicStick /></el-icon>
                <span style="font-size: 12px; margin-left: 4px">加载参数</span>
              </div>
              <div class="history-time">{{ formatTime(item.createdAt) }}</div>
            </div>
          </div>
        </div>
      </el-card>
    </el-aside>
  </el-container>

  <!-- 图片预览对话框 -->
  <el-dialog
    v-model="previewDialogVisible"
    title="图片预览"
    :width="isMobileView ? '95%' : '80%'"
    :show-close="true"
    center
  >
    <div class="preview-content">
      <div class="preview-image-container">
        <img :src="previewImageUrl" alt="预览图片" class="preview-image" />
      </div>
      <div class="preview-info" v-if="previewImageInfo">
        <el-descriptions :column="isMobileView ? 1 : 2" border>
          <el-descriptions-item label="生成时间">
            {{ new Date(previewImageInfo.createdAt).toLocaleString('zh-CN') }}
          </el-descriptions-item>
          <el-descriptions-item label="种子值">
            {{ previewImageInfo.seed }}
          </el-descriptions-item>
          <el-descriptions-item label="采样方法">
            {{ previewImageInfo.samplerName }}
          </el-descriptions-item>
          <el-descriptions-item label="步数">
            {{ previewImageInfo.steps }}
          </el-descriptions-item>
          <el-descriptions-item label="CFG">
            {{ previewImageInfo.cfg }}
          </el-descriptions-item>
          <el-descriptions-item label="提示词" :span="2">
            {{ previewImageInfo.prompt }}
          </el-descriptions-item>
          <el-descriptions-item label="负面提示词" :span="2" v-if="previewImageInfo.negativePrompt">
            {{ previewImageInfo.negativePrompt }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </div>
    <template #footer>
      <el-button @click="previewDialogVisible = false">关闭</el-button>
      <el-button type="primary" @click="loadHistoryItem(previewImageInfo); previewDialogVisible = false">
        加载参数
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, inject, watch, onMounted, onActivated, onUnmounted } from 'vue'
import api from '@/api'
import { ElMessage } from 'element-plus'
import {
  MagicStick,
  Picture as IconPicture,
  Refresh,
  Promotion,
  Lock,
  Share,
  Download,
  Clock,
  View,
  ArrowDown,
} from '@element-plus/icons-vue'
import { addDrawing, getAllDrawings, deleteDrawing } from '../utils/indexedDB.js'

const isLoggedIn = inject('isLoggedIn')
const showAuthDialog = inject('showAuthDialog')
const lastCompletedDrawing = inject('lastCompletedDrawing')

const params = reactive({
  prompt:
    '1girl, solo, year 2024,newest,masterpiece,highres,best quality,absurdres,very aesthetic,',
  negative_prompt:
    '(ai-generated:1.05),:q,realistic,no lineart,bad anatomy,bad perspective, bad hands,bad feet,user interface,twitter username, watermark,weibo watermark,speech bubble,weibo username,signature,worst quality, lowres, bad foot,artist name, artist logo, patreon username, patreon logo,facial mark,star sticker,sticker on face,shaded face',
  steps: 24,
  cfg: 5.0,
  sampler_name: 'euler_ancestral',
  seed: String(Math.floor(Math.random() * 1000000000000000)),
  model_name: '',
})
const availableModels = ref([])
const imageUrl = ref(null)
const isLoading = ref(false)
const progressStage = ref('')
const currentImageBase64 = ref(null) // 当前展示图片的 base64 数据
const currentDrawingParams = ref(null) // 当前展示图片的生成参数

const historyImages = ref([])
const MAX_HISTORY_COUNT = 12
const isSharing = ref(false)

const previewDialogVisible = ref(false)
const previewImageUrl = ref('')
const previewImageInfo = ref(null)

let drawingTimeoutId = null
const DRAWING_TIMEOUT = 5 * 60 * 1000

const isMobileView = ref(false)
const paramsCollapsed = ref(false)
const historyCollapsed = ref(false)

function handleResize() {
  const mobile = window.innerWidth <= 768
  if (mobile !== isMobileView.value) {
    isMobileView.value = mobile
    if (mobile) {
      paramsCollapsed.value = !!imageUrl.value
      historyCollapsed.value = true
    } else {
      paramsCollapsed.value = false
      historyCollapsed.value = false
    }
  }
}

// --- 简化的状态管理 ---

const samplerOptions = [
  { value: 'euler_ancestral', label: 'euler_ancestral' },
  { value: 'dpmpp_2m_sde_gpu', label: 'dpmpp_2m_sde_gpu' },
  { value: 'dpmpp_2m_sde', label: 'dpmpp_2m_sde' },
  { value: 'dpmpp_3m_sde_gpu', label: 'dpmpp_3m_sde_gpu' },
  { value: 'dpmpp_3m_sde', label: 'dpmpp_3m_sde' },
  { value: 'euler', label: 'euler' },
  { value: 'lms', label: 'lms' },
]

watch(
  lastCompletedDrawing,
  (newDrawing) => {
    console.log(
      '[Studio] Watch triggered with new value:',
      newDrawing ? Object.keys(newDrawing).join(', ') : null,
    )

    if (
      newDrawing &&
      (newDrawing.imageUrl || newDrawing.stored_filename || newDrawing.storedFilename)
    ) {
      console.log('[Studio] Detected new completed drawing via watcher:', newDrawing)

      imageUrl.value =
        newDrawing.imageUrl ||
        (newDrawing.stored_filename || newDrawing.storedFilename
          ? `http://localhost:8080/api/v1/images/${newDrawing.stored_filename || newDrawing.storedFilename}`
          : null)

      currentImageBase64.value = newDrawing.image_base64 || null
      currentDrawingParams.value = {
        prompt: newDrawing.prompt,
        negative_prompt: newDrawing.negative_prompt || newDrawing.negativePrompt,
        steps: newDrawing.steps,
        cfg: newDrawing.cfg,
        sampler_name: newDrawing.sampler_name || newDrawing.samplerName,
        seed: newDrawing.seed,
      }

      if (drawingTimeoutId) {
        clearTimeout(drawingTimeoutId)
        drawingTimeoutId = null
      }

      addToHistory(newDrawing)
      isLoading.value = false
      progressStage.value = ''
      ElMessage.success('图片生成成功！')
    }
  },
  { deep: true, immediate: true },
)

watch(imageUrl, (newUrl) => {
  if (isMobileView.value && newUrl) {
    paramsCollapsed.value = true
  }
})

// --- 函数 ---
const loadParametersFromStorage = () => {
  let storedParams = sessionStorage.getItem('prefillStudioParams')
  if (!storedParams) storedParams = localStorage.getItem('autoFillParams')
  if (!storedParams) return

  try {
    const parsedParams = JSON.parse(storedParams)
    console.log('[Studio] 📝 Loading parameters from storage:', parsedParams)

    if (parsedParams.prompt) params.prompt = parsedParams.prompt
    if (parsedParams.negativePrompt) params.negative_prompt = parsedParams.negativePrompt
    if (parsedParams.negative_prompt) params.negative_prompt = parsedParams.negative_prompt
    if (parsedParams.steps) params.steps = parsedParams.steps
    if (parsedParams.cfg) params.cfg = parsedParams.cfg
    if (parsedParams.samplerName) params.sampler_name = parsedParams.samplerName
    if (parsedParams.sampler_name) params.sampler_name = parsedParams.sampler_name
    if (parsedParams.seed) params.seed = parsedParams.seed
    if (parsedParams.model_name) params.model_name = parsedParams.model_name
    if (parsedParams.modelName) params.model_name = parsedParams.modelName

    sessionStorage.removeItem('prefillStudioParams')
    localStorage.removeItem('autoFillParams')

    ElMessage.success('参数已自动填充')
  } catch (error) {
    console.error('[Studio] Failed to parse stored parameters:', error)
    sessionStorage.removeItem('prefillStudioParams')
    localStorage.removeItem('autoFillParams')
  }
}

// 监听参数加载事件
const handleLoadAutoFillParams = () => {
  loadParametersFromStorage()
}

// 监听清空历史记录事件
const handleClearHistoryEvent = () => {
  historyImages.value = []
  imageUrl.value = null
  currentImageBase64.value = null
  currentDrawingParams.value = null
  isLoading.value = false
  progressStage.value = ''
  console.log('🗑️ [Studio] 收到清空历史记录事件，已清空本地历史和当前显示')
}

// 监听绘图失败事件
const handleDrawingFailedEvent = (event) => {
  console.error('❌ [Studio] 收到绘图失败事件:', event.detail)

  // 清除超时定时器
  if (drawingTimeoutId) {
    clearTimeout(drawingTimeoutId)
    drawingTimeoutId = null
  }

  // 重置加载状态
  isLoading.value = false
  progressStage.value = ''

  // 显示具体错误信息
  ElMessage.error(event.detail.error || '生图失败，请稍后重试')

  console.log('🔄 [Studio] 已重置界面状态，用户可以重新尝试生图')
}

// 处理生图超时
const handleDrawingTimeout = () => {
  console.warn('⏰ [Studio] 生图任务超时')

  // 重置状态
  isLoading.value = false
  progressStage.value = ''
  drawingTimeoutId = null

  // 显示温和的超时提示
  ElMessage.warning('图片生成时间较长，请稍后重试')

  console.log('🔄 [Studio] 超时后已重置界面状态')
}

// 手动取消生图任务
const cancelDrawing = () => {
  console.log('🛑 [Studio] 用户手动取消生图任务')

  // 清除超时定时器
  if (drawingTimeoutId) {
    clearTimeout(drawingTimeoutId)
    drawingTimeoutId = null
  }

  // 重置状态
  isLoading.value = false
  progressStage.value = ''

  // 显示取消提示
  ElMessage({
    message: '已停止等待，但AI服务器可能仍在处理该任务',
    type: 'warning',
    duration: 4000,
  })

  console.log('🔄 [Studio] 用户取消后已重置界面状态')
}

async function fetchAvailableModels() {
  try {
    const response = await api.get('/api/v1/ai-drawing/models')
    availableModels.value = response.data.models || []
    if (availableModels.value.length > 0 && !params.model_name) {
      params.model_name = availableModels.value[0].key
    }
    console.log('📋 [Studio] 可用模型:', availableModels.value)
  } catch (error) {
    console.error('获取模型列表失败:', error)
  }
}

onMounted(() => {
  handleResize()
  window.addEventListener('resize', handleResize)
  loadParametersFromStorage()
  loadHistoryFromIndexedDB()
  fetchAvailableModels()

  window.addEventListener('loadAutoFillParams', handleLoadAutoFillParams)
  window.addEventListener('clearHistory', handleClearHistoryEvent)
  window.addEventListener('drawingFailed', handleDrawingFailedEvent)
})

onActivated(() => {
  loadParametersFromStorage()
  fetchAvailableModels()
})

onActivated(() => {
  loadParametersFromStorage()
})

// 清理事件监听器
onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  window.removeEventListener('loadAutoFillParams', handleLoadAutoFillParams)
  window.removeEventListener('clearHistory', handleClearHistoryEvent)
  window.removeEventListener('drawingFailed', handleDrawingFailedEvent)
})

async function shareToGallery() {
  if (!currentImageBase64.value && !imageUrl.value) {
    ElMessage.warning('请先生成一张图片再分享')
    return
  }

  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录再分享作品')
    showAuthDialog()
    return
  }

  try {
    isSharing.value = true

    // 将 base64 转为 Blob
    let imageBlob
    if (currentImageBase64.value) {
      const byteString = atob(currentImageBase64.value)
      const ab = new ArrayBuffer(byteString.length)
      const ia = new Uint8Array(ab)
      for (let i = 0; i < byteString.length; i++) {
        ia[i] = byteString.charCodeAt(i)
      }
      imageBlob = new Blob([ab], { type: 'image/png' })
    } else {
      const response = await fetch(imageUrl.value)
      imageBlob = await response.blob()
    }

    const formData = new FormData()
    formData.append('image', imageBlob, 'artwork.png')
    formData.append(
      'params',
      JSON.stringify(
        currentDrawingParams.value || {
          prompt: params.prompt,
          negative_prompt: params.negative_prompt,
          steps: params.steps,
          cfg: params.cfg,
          sampler_name: params.sampler_name,
          seed: params.seed,
        },
      ),
    )

    const response = await api.post('/api/v1/ai-drawing/share', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })

    if (response.status === 200) {
      ElMessage.success('作品已成功分享到画廊！')
    }
  } catch (error) {
    console.error('分享到画廊失败:', error)

    if (error.response?.status === 403 || error.response?.status === 401) {
      ElMessage.warning('登录已过期，请重新登录')
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('userInfo')
      window.location.reload()
      return
    }

    ElMessage.error(error.response?.data?.message || '分享失败，请稍后重试')
  } finally {
    isSharing.value = false
  }
}

const downloadImage = () => {
  if (!imageUrl.value) {
    ElMessage.warning('没有可下载的图片')
    return
  }

  const link = document.createElement('a')
  link.href = imageUrl.value
  link.download = `ai-artwork-${Date.now()}.png`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)

  ElMessage.success('图片下载已开始')
}

const handleRandomSeed = () => {
  params.seed = String(Math.floor(Math.random() * 1000000000000000))
  console.log('🎲 [Studio] Generated new random seed:', params.seed)
}

async function loadHistoryFromIndexedDB() {
  try {
    const drawings = await getAllDrawings()
    historyImages.value = drawings.slice(0, MAX_HISTORY_COUNT)
    console.log(`📚 [Studio] 从 IndexedDB 加载了 ${historyImages.value.length} 张历史图片`)
  } catch (error) {
    console.error('❌ [Studio] 加载 IndexedDB 历史失败:', error)
    historyImages.value = []
  }
}

async function addToHistory(drawingData) {
  const imageUrl =
    drawingData.imageUrl ||
    (drawingData.stored_filename || drawingData.storedFilename
      ? `http://localhost:8080/api/v1/images/${drawingData.stored_filename || drawingData.storedFilename}`
      : null)

  const historyItem = {
    imageUrl: imageUrl,
    imageBase64: drawingData.image_base64 || null,
    prompt: drawingData.prompt,
    negativePrompt: drawingData.negativePrompt || drawingData.negative_prompt,
    steps: drawingData.steps,
    cfg: drawingData.cfg,
    samplerName: drawingData.samplerName || drawingData.sampler_name,
    seed: drawingData.seed,
    createdAt: drawingData.timestamp || new Date().toISOString(),
  }

  try {
    const id = await addDrawing(historyItem)
    historyItem.id = id
    historyImages.value.unshift(historyItem)

    if (historyImages.value.length > MAX_HISTORY_COUNT) {
      const removed = historyImages.value.splice(MAX_HISTORY_COUNT)
      for (const item of removed) {
        if (item.id) await deleteDrawing(item.id)
      }
    }
    console.log(`📸 [Studio] 添加新图片到 IndexedDB，当前总数: ${historyImages.value.length}`)
  } catch (error) {
    console.error('❌ [Studio] 保存到 IndexedDB 失败:', error)
  }
}

async function clearHistory() {
  try {
    const allDrawings = await getAllDrawings()
    for (const d of allDrawings) {
      await deleteDrawing(d.id)
    }
    historyImages.value = []
    ElMessage.success('历史记录已清空')
    console.log('🗑️ [Studio] 历史记录已清空')
  } catch (error) {
    console.error('❌ [Studio] 清空历史失败:', error)
  }
}

const loadHistoryItem = (item) => {
  params.prompt = item.prompt
  params.negative_prompt = item.negativePrompt
  params.steps = item.steps
  params.cfg = item.cfg
  params.sampler_name = item.samplerName
  params.seed = item.seed

  imageUrl.value = item.imageUrl
  currentImageBase64.value = item.imageBase64 || null
  currentDrawingParams.value = {
    prompt: item.prompt,
    negative_prompt: item.negativePrompt,
    steps: item.steps,
    cfg: item.cfg,
    sampler_name: item.samplerName,
    seed: item.seed,
  }

  ElMessage.success('已加载历史参数')
  console.log('🔄 [Studio] 从历史记录加载参数:', item.prompt?.substring(0, 50) + '...')
}

const previewHistoryImage = (item, event) => {
  event.stopPropagation()
  previewImageUrl.value = item.imageUrl
  previewImageInfo.value = item
  previewDialogVisible.value = true
  console.log('👁️ [Studio] 预览历史图片:', item.id)
}

const formatTime = (isoString) => {
  if (!isoString) return ''
  const date = new Date(isoString)
  const now = new Date()
  const diffMs = now - date
  const diffMins = Math.floor(diffMs / 60000)

  if (diffMins < 1) return '刚刚'
  if (diffMins < 60) return `${diffMins}分钟前`
  if (diffMins < 1440) return `${Math.floor(diffMins / 60)}小时前`
  return date.toLocaleDateString()
}

const handleSubmit = async () => {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录再进行绘图！')
    showAuthDialog()
    return
  }

  params.seed = String(Math.floor(Math.random() * 1000000000000000))

  isLoading.value = true
  progressStage.value = '正在提交任务...'
  imageUrl.value = null

  drawingTimeoutId = setTimeout(handleDrawingTimeout, DRAWING_TIMEOUT)

  try {
    const response = await api.post('/api/v1/ai-drawing/generate', params)

    if (response.data.status === 'QUEUED') {
      progressStage.value = '任务已进入队列，ComfyUI 正在生成...'
      console.log('⏰ [Studio] 已设置5分钟超时保护')
    } else {
      throw new Error(response.data.message || '提交任务失败')
    }
  } catch (error) {
    console.error('提交生成任务时发生错误:', error)

    // 清除超时定时器
    if (drawingTimeoutId) {
      clearTimeout(drawingTimeoutId)
      drawingTimeoutId = null
    }

    // 检查是否是JWT过期或认证失败
    if (error.response?.status === 403 || error.response?.status === 401) {
      console.warn('⚠️ [Studio] 认证失败，可能token已过期')
      ElMessage.warning('登录已过期，请重新登录')
      // 触发父组件的登出逻辑
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('userInfo')
      window.location.reload() // 刷新页面以重置状态
      return
    }

    if (error.response?.data?.status === 'INSUFFICIENT_CREDITS') {
      ElMessage.warning('积分不足！请前往个人中心签到获取积分')
      isLoading.value = false
      return
    }

    ElMessage.error(`提交失败: ${error.message || '未知错误'}`)
    isLoading.value = false
  }
}
</script>

<style scoped>
.studio-container {
  height: 100%;
  background-color: #f0f2f5;
}
.aside-panel {
  padding: 20px;
  background-color: #ffffff;
  border-right: 1px solid #e0e0e0;
  height: 100%;
  overflow-y: auto;
}
.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: bold;
}
.main-content {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
}
.image-container {
  width: 100%;
  height: 100%;
  max-width: 90vw;
  max-height: 80vh;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  display: flex;
  justify-content: center;
  align-items: center;
  flex-direction: column;
  position: relative;
  padding: 20px;
}
.placeholder {
  color: #a8abb2;
  text-align: center;
}
.image-result-container {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  position: relative;
  align-items: center;
  justify-content: center;
}

.generated-image {
  max-width: calc(100% - 40px);
  max-height: calc(100% - 80px);
  object-fit: contain;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  cursor: pointer;
}

.generated-image :deep(.el-image__inner) {
  border-radius: 8px;
}

.image-placeholder-slot {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 300px;
  font-size: 40px;
  color: #c0c4cc;
}

.image-actions {
  position: absolute;
  bottom: 16px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 12px;
  padding: 12px 20px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 50px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.image-result-container:hover .image-actions {
  opacity: 1;
}

.image-actions .el-button {
  border-radius: 20px;
  font-weight: 600;
  padding: 8px 16px;
}

.image-actions .el-button--primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
}

.image-actions .el-button--default {
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(0, 0, 0, 0.1);
  color: #666;
}
.login-prompt {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 20px;
  color: #606266;
}

/* 历史图片区域样式 */
.history-panel {
  padding: 20px;
  background-color: #ffffff;
  border-left: 1px solid #e0e0e0;
  height: 100%;
}

.history-content {
  height: calc(100% - 60px);
  overflow-y: auto;
}

.history-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  text-align: center;
}

.history-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  padding: 8px 0;
}

.history-item {
  position: relative;
  aspect-ratio: 1;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.history-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
}

.history-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.history-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
  color: white;
  font-size: 20px;
}

.history-item:hover .history-overlay {
  opacity: 1;
}

.history-time {
  position: absolute;
  bottom: 4px;
  left: 4px;
  right: 4px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 10px;
  text-align: center;
}

/* 预览对话框样式 */
.preview-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.preview-image-container {
  text-align: center;
  background-color: #f5f5f5;
  padding: 20px;
  border-radius: 8px;
}

.preview-image {
  max-width: 100%;
  max-height: 60vh;
  object-fit: contain;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.preview-info {
  background-color: #fafafa;
  padding: 16px;
  border-radius: 8px;
}

.collapse-toggle {
  margin-left: auto;
  font-size: 12px;
  color: #909399;
  flex-shrink: 0;
}

.collapse-toggle.collapsed :deep(.el-icon) {
  transform: rotate(-90deg);
}

.collapse-toggle :deep(.el-icon) {
  transition: transform 0.2s;
}

@media (max-width: 768px) {
  .studio-container {
    flex-direction: column;
    height: auto;
    min-height: 100dvh;
  }
  .aside-panel {
    width: 100% !important;
    height: auto;
    border-right: none;
    border-bottom: 1px solid #e0e0e0;
    padding: 12px;
  }
  .main-content {
    order: -1;
    padding: 12px;
    min-height: 50dvh;
  }
  .image-container {
    max-width: 100vw;
    max-height: none;
    height: auto;
    min-height: 50dvh;
    padding: 12px;
  }
  .image-result-container:hover .image-actions {
    opacity: 1;
  }
  .image-actions {
    position: static;
    transform: none;
    margin-top: 12px;
    padding: 0;
    background: transparent;
    backdrop-filter: none;
    box-shadow: none;
    border-radius: 0;
    opacity: 1;
    flex-wrap: wrap;
    justify-content: center;
  }
  .image-actions .el-button {
    font-size: 13px;
    padding: 6px 12px;
  }
  .generated-image {
    max-width: 100%;
    max-height: 60dvh;
  }
  .history-panel {
    width: 100% !important;
    height: auto;
    border-left: none;
    border-top: 1px solid #e0e0e0;
    padding: 12px;
  }
  .history-content {
    height: auto;
    max-height: none;
  }
  .history-grid {
    grid-template-columns: repeat(3, 1fr);
    gap: 8px;
  }
  .history-overlay {
    opacity: 1;
    background: rgba(0, 0, 0, 0.2);
  }
  .history-item:hover .history-overlay {
    opacity: 1;
  }
  .card-header {
    flex-wrap: wrap;
  }
  .collapse-toggle .el-icon {
    transform: rotate(0);
  }
  .preview-content {
    flex-direction: column;
  }
  .preview-image {
    max-height: 40dvh;
  }
}

@media (max-width: 480px) {
  .history-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .aside-panel {
    padding: 8px;
  }
  .main-content {
    padding: 8px;
  }
  .image-container {
    padding: 8px;
  }
}
</style>
