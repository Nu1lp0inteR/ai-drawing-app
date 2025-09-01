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
            <el-button type="primary" native-type="submit" :loading="isLoading" :icon="Promotion" style="width: 100%;">
              {{ isLoading ? '生成中...' : '开始生成' }}
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
            <el-button type="primary" @click="showLoginDialog">立即登录</el-button>
        </div>
      </div>
    </el-main>
  </el-container>
</template>

<script setup>
import { ref, reactive, inject, watch, onMounted, onUnmounted } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { MagicStick, Picture as IconPicture, Refresh, Promotion, Lock, Share, Download } from '@element-plus/icons-vue'

// --- 依赖注入 ---
const isLoggedIn = inject('isLoggedIn')
const showLoginDialog = inject('showLoginDialog')
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
const isSharing = ref(false) // 分享状态

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
  
  if (newDrawing && newDrawing.stored_filename) {
    console.log('[Studio] Detected new completed drawing via watcher:', newDrawing);
    // 构建完整的图片URL
    const newImageUrl = `http://localhost:8080/api/v1/images/${newDrawing.stored_filename}`;
    console.log('[Studio] 🖼️ Setting image URL to:', newImageUrl);
    imageUrl.value = newImageUrl;
    currentDrawingId.value = newDrawing.id; // 保存当前图片的ID
    console.log('[Studio] 🖼️ imageUrl.value is now:', imageUrl.value);
    console.log('[Studio] 📝 Drawing ID stored:', currentDrawingId.value);
    isLoading.value = false; // 停止加载状态
    ElMessage.success('图片生成成功！');
  } else {
    console.log('[Studio] ❌ Condition failed - newDrawing:', !!newDrawing, 'stored_filename:', newDrawing?.stored_filename);
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

// 生命周期钩子
onMounted(() => {
  loadParametersFromStorage(); // 组件挂载时检查一次
  
  // 监听来自App.vue的参数加载事件
  window.addEventListener('loadAutoFillParams', handleLoadAutoFillParams);
});

// 清理事件监听器
onUnmounted(() => {
  window.removeEventListener('loadAutoFillParams', handleLoadAutoFillParams);
});

// 分享到画廊功能 - 企业级用户体验设计
const shareToGallery = async () => {
  if (!currentDrawingId.value) {
    ElMessage.warning('请先生成一张图片再分享');
    return;
  }

  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录再分享作品');
    showLoginDialog();
    return;
  }

  try {
    isSharing.value = true;
    const response = await axios.post(
      `http://localhost:8080/api/v1/ai-drawing/${currentDrawingId.value}/share`
    );
    
    if (response.status === 200) {
      ElMessage.success('作品已成功分享到画廊！🎉');
    }
  } catch (error) {
    console.error('分享到画廊失败:', error);
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
}

const handleSubmit = async () => {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录再进行绘图！')
    showLoginDialog()
    return
  }

  isLoading.value = true
  imageUrl.value = null // 开始生成时，清空旧图片

  try {
    const backendUrl = 'http://localhost:8080/api/v1/ai-drawing/generate'
    const response = await axios.post(backendUrl, params)

    if (response.data.status === 'QUEUED') {
        ElMessage.info('任务已成功进入队列，请等待生成完成...')
    } else {
        throw new Error(response.data.message || '提交任务失败')
    }

  } catch (error) {
    console.error('提交生成任务时发生错误:', error)
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
  max-width: 80vh;
  max-height: 80vh;
  aspect-ratio: 2 / 3;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  display: flex;
  justify-content: center;
  align-items: center;
  flex-direction: column;
  overflow: hidden;
  position: relative;
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
}

.generated-image {
  width: 100%;
  flex: 1;
  object-fit: contain;
  border-radius: 8px;
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
</style>

