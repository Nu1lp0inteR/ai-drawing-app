<script setup>
import { ref, onMounted, onActivated, onUnmounted, inject } from 'vue';
import api from '@/api';
import { ElMessage, ElMessageBox } from 'element-plus';
import ArtworkDetailModal from '@/components/ArtworkDetailModal.vue';

const isLoggedIn = inject('isLoggedIn');
const showAuthDialog = inject('showAuthDialog');
const navigateTo = inject('navigateTo');

const isLoading = ref(true);
const artworks = ref([]);
const page = ref(0);
const totalPages = ref(0);
const pageSize = 20;

const detailModalVisible = ref(false);
const selectedArtwork = ref(null);

function mapToArtwork(item) {
  return {
    id: item.id,
    prompt: item.prompt,
    negativePrompt: item.negative_prompt,
    steps: item.steps,
    cfg: item.cfg,
    samplerName: item.sampler_name,
    seed: item.seed,
    storedFilename: item.stored_filename,
    sharedToGallery: item.shared_to_gallery,
    createdAt: item.created_at,
  };
}

function openDetail(item) {
  selectedArtwork.value = mapToArtwork(item);
  detailModalVisible.value = true;
}

function buildImageUrl(item) {
  return item.stored_filename ? `/api/v1/images/${item.stored_filename}` : '';
}

async function fetchHistory(reset = false) {
  if (reset) { page.value = 0; artworks.value = []; }
  isLoading.value = true;

  try {
    if (isLoggedIn.value) {
      const response = await api.get('/api/v1/ai-drawing/my-history', {
        params: { page: page.value, size: pageSize }
      });
      const data = response.data;
      const items = (data.content || []).map(d => ({ ...d, source: 'server' }));
      artworks.value = reset ? items : [...artworks.value, ...items];
      totalPages.value = data.totalPages;
    }
  } catch (error) {
    console.error('获取历史记录失败:', error);
  } finally {
    isLoading.value = false;
  }
}

async function handleShare(item) {
  try {
    await api.post(`/api/v1/ai-drawing/${item.id}/share`);
    item.shared_to_gallery = true;
    ElMessage.success('作品已分享到画廊');
  } catch (error) {
    console.error('分享失败:', error);
    ElMessage.error('分享失败');
  }
}

async function handleDelete(item) {
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
    await api.delete(`/api/v1/ai-drawing/${item.id}`);
    artworks.value = artworks.value.filter(a => a.id !== item.id);
    ElMessage.success('作品已删除');
  } catch (error) {
    console.error('删除失败:', error);
    ElMessage.error('删除失败');
  }
}

function handleReuseParams(item) {
  sessionStorage.setItem('prefillStudioParams', JSON.stringify({
    prompt: item.prompt,
    negative_prompt: item.negative_prompt || item.negativePrompt || '',
    steps: item.steps,
    cfg: item.cfg,
    sampler_name: item.sampler_name || item.samplerName || '',
    seed: item.seed,
  }));
  navigateTo('Studio');
}

function handleDownload(item) {
  const url = buildImageUrl(item);
  if (!url) {
    ElMessage.warning('无法下载此图片');
    return;
  }
  const link = document.createElement('a');
  link.href = url;
  link.download = `ai-artwork-${item.id}.png`;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  ElMessage.success('图片下载已开始');
}

function loadMore() {
  if (page.value < totalPages.value - 1) {
    page.value++;
    fetchHistory(false);
  }
}

onMounted(() => {
  if (!isLoggedIn.value) { showAuthDialog(); return; }
  fetchHistory(true);
  window.addEventListener('drawingDeleted', onDrawingDeleted);
});

onActivated(() => {
  if (isLoggedIn.value) fetchHistory(true);
});

onUnmounted(() => {
  window.removeEventListener('drawingDeleted', onDrawingDeleted);
});

function onDrawingDeleted() {
  fetchHistory(true);
}
</script>

<template>
  <div class="history-container">
    <div class="history-header">
      <h2>我的作品</h2>
      <span class="history-count" v-if="artworks.length">共 {{ artworks.length }} 张</span>
    </div>

    <div v-if="isLoading && artworks.length === 0" class="loading-state">
      <el-skeleton :rows="3" animated />
      <el-skeleton :rows="3" animated style="margin-top: 16px" />
    </div>

    <div v-else-if="artworks.length === 0" class="empty-state">
      <p>你还没有生成任何作品</p>
      <el-button type="primary" @click="navigateTo('studio')">去创作</el-button>
    </div>

    <div v-else class="artwork-grid">
      <div v-for="item in artworks" :key="item.id + item.source" class="artwork-card" @click="openDetail(item)">
        <div class="card-image">
          <el-image :src="buildImageUrl(item)" fit="cover" lazy>
            <template #placeholder>
              <div class="image-skeleton"><el-skeleton-item variant="image" style="width: 100%; height: 100%;" /></div>
            </template>
            <template #error>
              <div class="image-error-fallback"><span>加载失败</span></div>
            </template>
          </el-image>
          <div class="card-overlay">
            <el-button size="small" circle @click.stop="handleReuseParams(item)" title="复用参数">
              <span>🔄</span>
            </el-button>
            <el-button v-if="!item.shared_to_gallery" size="small" type="success" circle @click.stop="handleShare(item)" title="分享到画廊">
              <span>📤</span>
            </el-button>
            <el-button size="small" circle @click.stop="handleDownload(item)" title="下载">
              <span>⬇</span>
            </el-button>
            <el-button size="small" type="danger" circle @click.stop="handleDelete(item)" title="删除">
              <span>🗑</span>
            </el-button>
          </div>
        </div>
        <div class="card-info">
          <p class="prompt-text">{{ item.prompt?.slice(0, 60) }}{{ item.prompt?.length > 60 ? '...' : '' }}</p>
          <span class="badge" :class="item.shared_to_gallery ? 'shared' : 'private'">
            {{ item.shared_to_gallery ? '已分享' : '未分享' }}
          </span>
        </div>
      </div>
    </div>

    <div v-if="page < totalPages - 1 && artworks.length > 0" class="load-more">
      <el-button :loading="isLoading" @click="loadMore">加载更多</el-button>
    </div>

    <ArtworkDetailModal
      v-model:visible="detailModalVisible"
      :artwork="selectedArtwork"
      @copy-params="handleReuseParams"
    />
  </div>
</template>

<style scoped>
.history-container { max-width: 1200px; margin: 0 auto; padding: 24px; }
.history-header { display: flex; align-items: baseline; gap: 16px; margin-bottom: 24px; }
.history-header h2 { margin: 0; font-size: 24px; }
.history-count { color: #999; font-size: 14px; }

.empty-state, .loading-state { text-align: center; padding: 80px 0; color: #999; max-width: 400px; margin: 0 auto; }
.empty-state p { margin-bottom: 16px; font-size: 16px; }

.artwork-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 16px; }
.artwork-card { border-radius: 8px; overflow: hidden; background: #fff; box-shadow: 0 2px 8px rgba(0,0,0,0.08); transition: transform 0.2s; cursor: pointer; }
.artwork-card:hover { transform: translateY(-2px); box-shadow: 0 4px 16px rgba(0,0,0,0.12); }

.card-image { position: relative; aspect-ratio: 1; overflow: hidden; background: #f0f0f0; }
.card-image .el-image { width: 100%; height: 100%; }
.card-image img { width: 100%; height: 100%; object-fit: cover; }
.image-skeleton { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; }
.image-error-fallback { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; color: #999; font-size: 13px; background: #f5f5f5; }
.card-overlay { position: absolute; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; gap: 8px; opacity: 0; transition: opacity 0.2s; }
.artwork-card:hover .card-overlay { opacity: 1; }

.card-info { padding: 12px; }
.prompt-text { margin: 0 0 8px; font-size: 13px; color: #333; line-height: 1.4; }
.badge { display: inline-block; padding: 2px 8px; border-radius: 4px; font-size: 11px; }
.badge.shared { background: #e6f7e6; color: #52c41a; }
.badge.private { background: #f0f0f0; color: #999; }

.load-more { text-align: center; padding: 24px 0; }

@media (max-width: 768px) {
  .history-container { padding: 16px 12px; }
  .history-header h2 { font-size: 20px; }
  .artwork-grid { grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); gap: 12px; }
  .card-overlay { opacity: 1; background: rgba(0,0,0,0.25); }
  .empty-state, .loading-state { padding: 60px 0; }
}
</style>
