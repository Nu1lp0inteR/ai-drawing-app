<template>
  <el-container class="studio-container">
    <el-aside width="400px" class="aside-panel">
      <el-card shadow="always" style="height: 100%;">
        <template #header>
          <div class="card-header">
            <el-icon><MagicStick /></el-icon>
            <span>生成参数</span>
          </div>
        </template>
        <el-form :model="params" label-position="top" @submit.prevent="handleSubmit">
          <el-form-item label="主要提示词 (Prompt)">
            <el-input v-model="params.prompt" type="textarea" :rows="5" />
          </el-form-item>
          <el-form-item label="反向提示词 (Negative Prompt)">
            <el-input v-model="params.negative_prompt" type="textarea" :rows="3" />
          </el-form-item>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="采样步数">
                <el-input-number v-model="params.steps" :min="1" :max="100" style="width: 100%;" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="采样器">
                <el-select v-model="params.sampler_name" style="width: 100%;">
                  <el-option v-for="item in samplerOptions" :key="item.value" :label="item.label" :value="item.value" />
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
            style="width: 100%;"
            :disabled="isLoading"
          >
            {{ isLoading ? '生成中...' : '开始生成' }}
          </el-button>
          
          <!-- 取消按钮，仅在生成中显示 -->
          <el-button 
            v-if="isLoading"
            type="danger" 
            @click="cancelDrawing"
            style="width: 100%; margin-top: 10px;"
            plain
          >
            停止等待 (后台仍在处理)
          </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </el-aside>
    
    <el-main class="main-content">
      <div class="image-container" v-loading="isLoading" element-loading-text="正在全力生成中...">
        <div v-if="!imageUrl && !isLoading" class="placeholder">
          <el-icon :size="60"><IconPicture /></el-icon>
          <p>生成的图片将在这里显示</p>
        </div>
        <div v-if="imageUrl" class="image-result-container">
          <img :src="imageUrl" alt="Generated Art" class="generated-image" 
               @load="console.log('[Studio] 🖼️ Image loaded successfully:', imageUrl)"
               @error="console.error('[Studio] ❌ Image load failed:', imageUrl)"/>
          
          <!-- 图片操作区域 -->
          <div class="image-actions">
            <el-button 
              type="primary" 
              :loading="isSharing"
              @click="shareToGallery"
              :icon="Share"
            >
              {{ isSharing ? '分享中...' : '分享到画廊' }}
            </el-button>
            
            <el-button 
              type="default"
              @click="downloadImage"
              :icon="Download"
            >
              下载图片
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
      <el-card shadow="always" style="height: 100%;">
        <template #header>
          <div class="card-header">
            <el-icon><Clock /></el-icon>
            <span>最近生成 ({{ historyImages.length }}/12)</span>
            <el-button 
              type="text" 
              size="small" 
              @click="clearHistory"
              v-if="historyImages.length > 0"
              style="float: right; padding: 0; color: #909399;"
            >
              清空
            </el-button>
          </div>
        </template>
        <div class="history-content">
          <div v-if="historyImages.length === 0" class="history-empty">
            <el-icon :size="40" style="color: #c0c4cc;"><IconPicture /></el-icon>
            <p style="color: #909399; margin-top: 10px;">暂无历史记录</p>
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
                  style="margin-right: 8px;"
                />
                <el-icon><MagicStick /></el-icon>
                <span style="font-size: 12px; margin-left: 4px;">加载参数</span>
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
    width="80%"
    :show-close="true"
    center
  >
    <div class="preview-content">
      <div class="preview-image-container">
        <img :src="previewImageUrl" alt="预览图片" class="preview-image" />
      </div>
      <div class="preview-info" v-if="previewImageInfo">
        <el-descriptions :column="2" border>
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
import { ref, reactive, inject, watch, onMounted, onUnmounted } from 'vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MagicStick, Picture as IconPicture, Refresh, Promotion, Lock, Share, Download, Clock, View } from '@element-plus/icons-vue'

// --- 依赖注入 ---
const isLoggedIn = inject('isLoggedIn')
const userInfo = inject('userInfo')
const showAuthDialog = inject('showAuthDialog')
const lastCompletedDrawing = inject('lastCompletedDrawing') // 注入最新完成的绘图数据

// --- 状态管理 ---
const params = reactive({
  prompt: '1girl, solo, masterpiece, best quality,  looking at viewer,white background, standing, long hair, purple hair,blue eyes,maid apron,maid',
  negative_prompt: 'lowres, bad anatomy, bad hands, text, error, missing fingers, extra digit, fewer digits, cropped, worst quality, low quality, normal quality, jpeg artifacts, signature, watermark, username, blurry',
  steps: 24,
  cfg: 6.0,
  sampler_name: 'euler_ancestral',
  seed: String(Math.floor(Math.random() * 1000000000000000)), // Seed should be a string
})
const imageUrl = ref(null)
const isLoading = ref(false)
const currentDrawingId = ref(null) // 存储当前生成图片的ID

// --- 历史图片管理 ---
const historyImages = ref([]) // 最近生成的图片历史 (最多12张)
const HISTORY_STORAGE_KEY = 'ai_drawing_history'
const MAX_HISTORY_COUNT = 12
const isSharing = ref(false) // 分享状态

// --- 图片预览状态 ---
const previewDialogVisible = ref(false)
const previewImageUrl = ref('')
const previewImageInfo = ref(null)

// --- 超时机制 ---
let drawingTimeoutId = null
const DRAWING_TIMEOUT = 5 * 60 * 1000 // 5分钟超时

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

// --- 监视器 ---
// 修复版本：直接监视注入的 ref 对象，而不是其 .value 属性
watch(lastCompletedDrawing, (newDrawing) => {
  console.log('[Studio] Watch triggered with new value:', newDrawing);
  console.log('[Studio] 🔍 newDrawing exists:', !!newDrawing);
  if (newDrawing) {
    console.log('[Studio] 🔍 storedFilename exists:', !!newDrawing.storedFilename);
    console.log('[Studio] 🔍 storedFilename value:', newDrawing.storedFilename);
    console.log('[Studio] 🔍 All keys in newDrawing:', Object.keys(newDrawing));
  }
  
  if (newDrawing && (newDrawing.stored_filename || newDrawing.storedFilename)) {
    console.log('[Studio] Detected new completed drawing via watcher:', newDrawing);
    // 构建完整的图片URL - 兼容两种命名方式
    const filename = newDrawing.stored_filename || newDrawing.storedFilename;
    const newImageUrl = `http://localhost:8080/api/v1/images/${filename}`;
    console.log('[Studio] 🖼️ Setting image URL to:', newImageUrl);
    imageUrl.value = newImageUrl;
    currentDrawingId.value = newDrawing.id; // 保存当前图片的ID
    console.log('[Studio] 🖼️ imageUrl.value is now:', imageUrl.value);
    console.log('[Studio] 📝 Drawing ID stored:', currentDrawingId.value);
    
    // 清除超时定时器
    if (drawingTimeoutId) {
      clearTimeout(drawingTimeoutId);
      drawingTimeoutId = null;
    }
    
    // 添加到历史记录
    addToHistory(newDrawing);
    
    isLoading.value = false; // 停止加载状态
    ElMessage.success('图片生成成功！');
  } else {
    console.log('[Studio] ❌ Condition failed - newDrawing:', !!newDrawing, 'stored_filename:', newDrawing?.stored_filename, 'storedFilename:', newDrawing?.storedFilename);
  }
}, { deep: true, immediate: true }); // 添加 immediate: true 来立即执行一次检查

// --- 函数 ---
// 企业级参数复用功能 - 从localStorage获取并填充参数
const loadParametersFromStorage = () => {
  const storedParams = localStorage.getItem('autoFillParams');
  if (storedParams) {
    try {
      const parsedParams = JSON.parse(storedParams);
      console.log('[Studio] 📝 Loading parameters from storage:', parsedParams);
      
      // 映射参数名称（处理命名差异）
      if (parsedParams.prompt) params.prompt = parsedParams.prompt;
      if (parsedParams.negativePrompt) params.negative_prompt = parsedParams.negativePrompt;
      if (parsedParams.steps) params.steps = parsedParams.steps;
      if (parsedParams.cfg) params.cfg = parsedParams.cfg;
      if (parsedParams.samplerName) params.sampler_name = parsedParams.samplerName;
      if (parsedParams.seed) params.seed = parsedParams.seed;
      
      // 清除localStorage中的参数，避免重复加载
      localStorage.removeItem('autoFillParams');
      
      ElMessage.success('参数已自动填充，您可以直接生成或修改后再生成');
    } catch (error) {
      console.error('[Studio] Failed to parse stored parameters:', error);
      localStorage.removeItem('autoFillParams');
    }
  }
};

// 监听参数加载事件
const handleLoadAutoFillParams = () => {
  loadParametersFromStorage();
};

// 监听清空历史记录事件
const handleClearHistoryEvent = () => {
  historyImages.value = [];
  console.log('🗑️ [Studio] 收到清空历史记录事件，已清空本地历史');
};

// 监听绘图失败事件
const handleDrawingFailedEvent = (event) => {
  console.error('❌ [Studio] 收到绘图失败事件:', event.detail);
  
  // 清除超时定时器
  if (drawingTimeoutId) {
    clearTimeout(drawingTimeoutId);
    drawingTimeoutId = null;
  }
  
  // 重置加载状态
  isLoading.value = false;
  
  // 显示具体错误信息
  ElMessage.error(event.detail.error || '生图失败，请稍后重试');
  
  console.log('🔄 [Studio] 已重置界面状态，用户可以重新尝试生图');
};

// 处理生图超时
const handleDrawingTimeout = () => {
  console.warn('⏰ [Studio] 生图任务超时');
  
  // 重置状态
  isLoading.value = false;
  drawingTimeoutId = null;
  
  // 显示温和的超时提示
  ElMessage.warning('图片生成时间较长，请稍后重试');
  
  console.log('🔄 [Studio] 超时后已重置界面状态');
};

// 手动取消生图任务
const cancelDrawing = () => {
  console.log('🛑 [Studio] 用户手动取消生图任务');
  
  // 清除超时定时器
  if (drawingTimeoutId) {
    clearTimeout(drawingTimeoutId);
    drawingTimeoutId = null;
  }
  
  // 重置状态
  isLoading.value = false;
  
  // 显示取消提示
  ElMessage({
    message: '已停止等待，但AI服务器可能仍在处理该任务',
    type: 'warning',
    duration: 4000
  });
  
  console.log('🔄 [Studio] 用户取消后已重置界面状态');
};



// 生命周期钩子
onMounted(() => {
  loadParametersFromStorage(); // 组件挂载时检查一次
  loadHistoryFromStorage(); // 加载历史图片记录
  
  // 监听来自App.vue的参数加载事件
  window.addEventListener('loadAutoFillParams', handleLoadAutoFillParams);
  // 监听清空历史记录事件
  window.addEventListener('clearHistory', handleClearHistoryEvent);
  // 监听绘图失败事件
  window.addEventListener('drawingFailed', handleDrawingFailedEvent);
});

// 清理事件监听器
onUnmounted(() => {
  window.removeEventListener('loadAutoFillParams', handleLoadAutoFillParams);
  window.removeEventListener('clearHistory', handleClearHistoryEvent);
  window.removeEventListener('drawingFailed', handleDrawingFailedEvent);
});

// 分享到画廊功能 - 企业级用户体验设计
const shareToGallery = async () => {
  if (!currentDrawingId.value) {
    ElMessage.warning('请先生成一张图片再分享');
    return;
  }

  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录再分享作品');
    showAuthDialog();
    return;
  }

  try {
    isSharing.value = true;
    
    // 获取认证token
    const token = localStorage.getItem('accessToken')
    const headers = token ? { Authorization: `Bearer ${token}` } : {}
    
    const response = await axios.post(
      `http://localhost:8080/api/v1/ai-drawing/${currentDrawingId.value}/share`,
      {},
      { headers }
    );
    
    if (response.status === 200) {
      ElMessage.success('作品已成功分享到画廊！🎉');
    }
  } catch (error) {
    console.error('分享到画廊失败:', error);
    
    // 检查是否是JWT过期或认证失败
    if (error.response?.status === 403 || error.response?.status === 401) {
      console.warn('⚠️ [Studio] 分享时认证失败，可能token已过期');
      ElMessage.warning('登录已过期，请重新登录');
      // 触发父组件的登出逻辑
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('userInfo');
      window.location.reload(); // 刷新页面以重置状态
      return;
    }
    
    if (error.response?.status === 404) {
      ElMessage.error('作品不存在，无法分享');
    } else {
      ElMessage.error('分享失败，请稍后重试');
    }
  } finally {
    isSharing.value = false;
  }
};

// 下载图片功能
const downloadImage = () => {
  if (!imageUrl.value) {
    ElMessage.warning('没有可下载的图片');
    return;
  }

  // 创建一个临时的a标签来触发下载
  const link = document.createElement('a');
  link.href = imageUrl.value;
  link.download = `ai-artwork-${Date.now()}.png`;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  
  ElMessage.success('图片下载已开始');
};

const handleRandomSeed = () => {
  params.seed = String(Math.floor(Math.random() * 1000000000000000))
  console.log('🎲 [Studio] Generated new random seed:', params.seed)
}

// --- 历史图片管理函数 ---

// 从localStorage加载历史图片
const loadHistoryFromStorage = () => {
  try {
    const stored = localStorage.getItem(HISTORY_STORAGE_KEY)
    if (stored) {
      const parsed = JSON.parse(stored)
      // 确保数据格式正确且不超过最大数量
      historyImages.value = Array.isArray(parsed) ? parsed.slice(0, MAX_HISTORY_COUNT) : []
      console.log(`📚 [Studio] 从本地存储加载了 ${historyImages.value.length} 张历史图片`)
    }
  } catch (error) {
    console.error('❌ [Studio] 加载历史图片失败:', error)
    historyImages.value = []
  }
}

// 保存历史图片到localStorage
const saveHistoryToStorage = () => {
  try {
    localStorage.setItem(HISTORY_STORAGE_KEY, JSON.stringify(historyImages.value))
  } catch (error) {
    console.error('❌ [Studio] 保存历史图片失败:', error)
  }
}

// 添加新图片到历史记录
const addToHistory = (drawingData) => {
  const historyItem = {
    id: drawingData.id,
    imageUrl: `http://localhost:8080/api/v1/images/${drawingData.storedFilename || drawingData.stored_filename}`,
    prompt: drawingData.prompt,
    negativePrompt: drawingData.negativePrompt || drawingData.negative_prompt,
    steps: drawingData.steps,
    cfg: drawingData.cfg,
    samplerName: drawingData.samplerName || drawingData.sampler_name,
    seed: drawingData.seed,
    createdAt: new Date().toISOString()
  }
  
  // 添加到数组开头（最新的在前面）
  historyImages.value.unshift(historyItem)
  
  // 保持最大数量限制
  if (historyImages.value.length > MAX_HISTORY_COUNT) {
    historyImages.value = historyImages.value.slice(0, MAX_HISTORY_COUNT)
  }
  
  saveHistoryToStorage()
  console.log(`📸 [Studio] 添加新图片到历史记录，当前总数: ${historyImages.value.length}`)
}

// 清空历史记录
const clearHistory = () => {
  historyImages.value = []
  localStorage.removeItem(HISTORY_STORAGE_KEY)
  ElMessage.success('历史记录已清空')
  console.log('🗑️ [Studio] 历史记录已清空')
}

// 从历史记录加载参数到当前表单
const loadHistoryItem = (item) => {
  params.prompt = item.prompt
  params.negative_prompt = item.negativePrompt
  params.steps = item.steps
  params.cfg = item.cfg
  params.sampler_name = item.samplerName
  params.seed = item.seed
  
  ElMessage.success('已加载历史参数')
  console.log('🔄 [Studio] 从历史记录加载参数:', item.prompt.substring(0, 50) + '...')
}

// 预览历史图片
const previewHistoryImage = (item, event) => {
  event.stopPropagation() // 阻止触发loadHistoryItem
  previewImageUrl.value = item.imageUrl
  previewImageInfo.value = item
  previewDialogVisible.value = true
  console.log('👁️ [Studio] 预览历史图片:', item.id)
}

// 格式化时间显示
const formatTime = (isoString) => {
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

  // 每次生图自动更新seed，确保图片唯一性
  params.seed = String(Math.floor(Math.random() * 1000000000000000));
  console.log('🎲 [Studio] 自动生成新seed确保唯一性:', params.seed);

  isLoading.value = true
  imageUrl.value = null // 开始生成时，清空旧图片

  // 设置超时定时器
  drawingTimeoutId = setTimeout(handleDrawingTimeout, DRAWING_TIMEOUT)

  try {
    const backendUrl = 'http://localhost:8080/api/v1/ai-drawing/generate'
    
    // 获取认证token
    const token = localStorage.getItem('accessToken')
    const headers = token ? { Authorization: `Bearer ${token}` } : {}
    
    const response = await axios.post(backendUrl, params, { headers })

    if (response.data.status === 'QUEUED') {
        ElMessage.info('任务已成功进入队列，请等待生成完成...')
        console.log('⏰ [Studio] 已设置5分钟超时保护')
    } else {
        throw new Error(response.data.message || '提交任务失败')
    }

  } catch (error) {
    console.error('提交生成任务时发生错误:', error)
    
    // 清除超时定时器
    if (drawingTimeoutId) {
      clearTimeout(drawingTimeoutId);
      drawingTimeoutId = null;
    }
    
    // 检查是否是JWT过期或认证失败
    if (error.response?.status === 403 || error.response?.status === 401) {
      console.warn('⚠️ [Studio] 认证失败，可能token已过期');
      ElMessage.warning('登录已过期，请重新登录');
      // 触发父组件的登出逻辑
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('userInfo');
      window.location.reload(); // 刷新页面以重置状态
      return;
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
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
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
</style>

