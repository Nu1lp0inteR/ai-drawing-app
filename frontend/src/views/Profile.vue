<template>
  <div class="profile-container">
    <!-- 个人信息头部 -->
    <el-card class="profile-header" shadow="always">
      <div class="user-info">
        <el-avatar :size="80" :icon="UserFilled" />
        <div class="user-details">
          <h2>{{ userInfo?.username || '用户' }}</h2>
          <p class="user-email">{{ userInfo?.email || '' }}</p>
          <p class="join-date" v-if="stats">加入时间: {{ formatDate(stats.joinDate) }}</p>
        </div>
        <div class="user-stats" v-if="stats">
          <div class="stat-item">
            <div class="stat-number">{{ stats.totalArtworks }}</div>
            <div class="stat-label">总作品</div>
          </div>
          <div class="stat-item clickable" @click="goToFollowingList">
            <div class="stat-number">{{ stats.followingCount || 0 }}</div>
            <div class="stat-label">关注</div>
          </div>
          <div class="stat-item clickable" @click="goToFollowersList">
            <div class="stat-number">{{ stats.followersCount || 0 }}</div>
            <div class="stat-label">粉丝</div>
          </div>
          <div class="stat-item">
            <div class="stat-number">{{ formatDate(stats.lastActiveDate) }}</div>
            <div class="stat-label">最后活跃</div>
          </div>
        </div>
      </div>
    </el-card>

    <el-card class="credits-card" shadow="hover">
      <div class="credits-section">
        <div class="credits-balance">
          <el-icon :size="24"><Coin /></el-icon>
          <span class="balance-number">{{ creditsBalance }}</span>
          <span class="balance-label">积分</span>
        </div>
        <el-button
          type="primary"
          :icon="Coin"
          :loading="signingIn"
          :disabled="todaySignedIn"
          @click="handleSignIn"
        >
          {{ todaySignedIn ? '今日已签到' : '每日签到 +10' }}
        </el-button>
      </div>
      <p class="credits-hint">每次生成消耗 1 积分，不足时请签到获取</p>
    </el-card>

    <!-- 作品管理区域 -->
    <el-card class="artworks-section" shadow="always">
      <template #header>
        <div class="section-header">
          <div class="header-left">
            <el-icon><Picture /></el-icon>
            <span>我的作品</span>
            <el-tag v-if="artworkList.totalCount" type="info" size="small">
              {{ artworkList.totalCount }} 张
            </el-tag>
          </div>
          <div class="header-right">
            <el-button
              type="primary"
              size="small"
              @click="refreshArtworks"
              :loading="loading"
              :icon="Refresh"
            >
              刷新
            </el-button>
          </div>
        </div>
      </template>

      <!-- 作品网格 -->
      <div v-loading="loading" class="artworks-content">
        <div v-if="artworkList.artworks.length === 0" class="empty-state">
          <el-icon :size="60" style="color: #c0c4cc"><Picture /></el-icon>
          <p style="color: #909399; margin-top: 15px">还没有创作作品</p>
          <el-button type="primary" @click="goToStudio">开始创作</el-button>
        </div>

        <div v-else class="artworks-grid">
          <div v-for="artwork in artworkList.artworks" :key="artwork.id" class="artwork-card">
            <!-- 图片 -->
            <div class="artwork-image-container">
              <img
                :src="getImageSrc(artwork)"
                :alt="artwork.prompt"
                class="artwork-image"
                @error="handleImageError"
              />

              <!-- 分享状态标识 -->
              <div v-if="artwork.sharedToGallery" class="shared-badge">
                <el-icon><Share /></el-icon>
                <span>已分享</span>
              </div>

              <div class="artwork-overlay">
                <el-button
                  type="primary"
                  size="small"
                  @click="viewArtworkDetail(artwork)"
                  :icon="View"
                  circle
                />
                <el-button
                  type="success"
                  size="small"
                  @click="toggleSharing(artwork)"
                  :icon="Share"
                  circle
                  :class="{ shared: artwork.sharedToGallery }"
                />
                <el-button
                  type="danger"
                  size="small"
                  @click="confirmDelete(artwork)"
                  :icon="Delete"
                  circle
                />
              </div>
            </div>

            <!-- 作品信息 -->
            <div class="artwork-info">
              <div class="artwork-prompt">
                {{
                  artwork.prompt.length > 50
                    ? artwork.prompt.substring(0, 50) + '...'
                    : artwork.prompt
                }}
              </div>
              <div class="artwork-meta">
                <el-tag size="small" type="info">{{ artwork.samplerName }}</el-tag>
                <el-tag size="small" type="success" v-if="artwork.sharedToGallery">已分享</el-tag>
                <span class="artwork-date">{{ formatTime(artwork.createdAt) }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 分页器 -->
        <div class="pagination-container" v-if="artworkList.totalCount > 0">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="artworkList.totalCount"
            :page-sizes="[10, 20, 50]"
            :layout="paginationLayout"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </div>
    </el-card>

    <!-- 作品详情弹窗 -->
    <ArtworkDetailModal v-model:visible="detailModalVisible" :artwork="selectedArtwork" />
  </div>
</template>

<script setup>
import { ref, reactive, inject, onMounted, onActivated, onUnmounted, computed } from 'vue'
import api from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UserFilled, Picture, Refresh, View, Share, Delete, Coin } from '@element-plus/icons-vue'
import ArtworkDetailModal from '../components/ArtworkDetailModal.vue'
import { getAllDrawings, deleteDrawing as deleteFromDB } from '../utils/indexedDB.js'

const userInfo = inject('userInfo')
const creditsBalance = inject('creditsBalance')
const todaySignedIn = inject('todaySignedIn')
const fetchCredits = inject('fetchCredits')
const navigateToFollowingList = inject('navigateToFollowingList')
const navigateToFollowersList = inject('navigateToFollowersList')

const loading = ref(false)
const stats = ref(null)
const artworkList = reactive({
  artworks: [],
  totalCount: 0,
})

const currentPage = ref(1)
const pageSize = ref(20)

const detailModalVisible = ref(false)
const selectedArtwork = ref(null)

const isSmallScreen = ref(false)
const checkScreenSize = () => {
  isSmallScreen.value = window.innerWidth <= 768
}
const paginationLayout = computed(() =>
  isSmallScreen.value ? 'prev, pager, next' : 'total, sizes, prev, pager, next, jumper',
)

const signingIn = ref(false)

async function handleSignIn() {
  signingIn.value = true
  try {
    const response = await api.post('/api/v1/credits/sign-in')
    creditsBalance.value = response.data.balance
    todaySignedIn.value = true
    ElMessage.success(response.data.message || '签到成功')
  } catch (error) {
    ElMessage.warning(error.response?.data?.message || '签到失败')
  } finally {
    signingIn.value = false
  }
}

async function fetchProfileHome() {
  try {
    // 从 IndexedDB 加载本地作品
    const localDrawings = await getAllDrawings()
    artworkList.artworks = localDrawings.map((d) => ({
      id: d.id,
      prompt: d.prompt || '',
      negativePrompt: d.negativePrompt,
      steps: d.steps,
      cfg: d.cfg,
      samplerName: d.samplerName,
      seed: d.seed,
      storedFilename: null,
      imageUrl: d.imageUrl,
      imageBase64: d.imageBase64,
      sharedToGallery: false,
      createdAt: d.createdAt,
      isLocal: true,
    }))
    artworkList.totalCount = localDrawings.length

    // 从后端获取统计数据（关注/粉丝数）
    try {
      const response = await api.get('/api/v1/profile/stats', {})
      stats.value = response.data
      stats.value.totalArtworks = localDrawings.length
    } catch (statsErr) {
      console.warn('⚠️ [Profile] 获取后端统计失败，使用本地数据:', statsErr)
    }

    console.log(`✅ [Profile] 从 IndexedDB 加载了 ${localDrawings.length} 张本地作品`)
    fetchCredits()
  } catch (error) {
    console.error('❌ [Profile] 获取数据失败:', error)
    handleAuthError(error)
  }
}

async function fetchUserArtworks() {
  loading.value = true
  try {
    const localDrawings = await getAllDrawings()
    artworkList.artworks = localDrawings.map((d) => ({
      id: d.id,
      prompt: d.prompt || '',
      negativePrompt: d.negativePrompt,
      steps: d.steps,
      cfg: d.cfg,
      samplerName: d.samplerName,
      seed: d.seed,
      storedFilename: null,
      imageUrl: d.imageUrl,
      imageBase64: d.imageBase64,
      sharedToGallery: false,
      createdAt: d.createdAt,
      isLocal: true,
    }))
    artworkList.totalCount = localDrawings.length
    console.log(`✅ [Profile] 作品列表加载成功: ${localDrawings.length} 张作品`)
  } catch (error) {
    console.error('❌ [Profile] 获取作品列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 处理认证错误
const handleAuthError = (error) => {
  if (error.response?.status === 403 || error.response?.status === 401) {
    ElMessage.warning('登录已过期，请重新登录')
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('userInfo')
    window.location.reload()
  } else {
    ElMessage.error('请求失败，请稍后重试')
  }
}

const refreshArtworks = () => {
  fetchUserArtworks()
}

const viewArtworkDetail = (artwork) => {
  selectedArtwork.value = artwork
  detailModalVisible.value = true
}

const confirmDelete = async (artwork) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除作品"${(artwork.prompt || '').substring(0, 30)}..."吗？`,
      '删除确认',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    await deleteArtwork(artwork)
  } catch (error) {
    if (error !== 'cancel') {
      console.error('❌ [Profile] 删除确认失败:', error)
    }
  }
}

const deleteArtwork = async (artwork) => {
  try {
    if (artwork.isLocal) {
      await deleteFromDB(artwork.id)
    } else {
      await api.delete(`/api/v1/profile/artworks/${artwork.id}`, {})
    }

    const index = artworkList.artworks.findIndex((item) => item.id === artwork.id)
    if (index !== -1) {
      artworkList.artworks.splice(index, 1)
      artworkList.totalCount--
      if (stats.value) {
        stats.value.totalArtworks--
      }
    }

    ElMessage.success('作品删除成功')
    console.log('✅ [Profile] 作品删除成功:', artwork.id)
  } catch (error) {
    console.error('❌ [Profile] 删除作品失败:', error)
    handleAuthError(error)
  }
}

const toggleSharing = async (artwork) => {
  try {
    if (!artwork.imageBase64 && !artwork.imageUrl) {
      ElMessage.warning('无法分享：图片数据不可用')
      return
    }

    let imageBlob
    if (artwork.imageBase64) {
      const byteString = atob(artwork.imageBase64)
      const ab = new ArrayBuffer(byteString.length)
      const ia = new Uint8Array(ab)
      for (let i = 0; i < byteString.length; i++) {
        ia[i] = byteString.charCodeAt(i)
      }
      imageBlob = new Blob([ab], { type: 'image/png' })
    } else {
      const response = await fetch(artwork.imageUrl)
      imageBlob = await response.blob()
    }

    const formData = new FormData()
    formData.append('image', imageBlob, 'artwork.png')
    formData.append(
      'params',
      JSON.stringify({
        prompt: artwork.prompt,
        negative_prompt: artwork.negativePrompt,
        steps: artwork.steps,
        cfg: artwork.cfg,
        sampler_name: artwork.samplerName,
        seed: artwork.seed,
      }),
    )

    await api.post('/api/v1/ai-drawing/share', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })

    artwork.sharedToGallery = true
    ElMessage.success('作品已分享到画廊')
    console.log('🔄 [Profile] 分享状态已更新:', artwork.id)
  } catch (error) {
    console.error('❌ [Profile] 切换分享状态失败:', error)
    handleAuthError(error)
  }
}

const handleSizeChange = (newSize) => {
  pageSize.value = newSize
  currentPage.value = 1
}

const handleCurrentChange = (newPage) => {
  currentPage.value = newPage
}

// --- 工具函数 ---

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN')
}

// 格式化时间（相对时间）
const formatTime = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  const now = new Date()
  const diffMs = now - date
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24))

  if (diffDays === 0) return '今天'
  if (diffDays === 1) return '昨天'
  if (diffDays < 7) return `${diffDays}天前`
  return date.toLocaleDateString('zh-CN')
}

// 处理图片加载错误
const blobUrlCache = new Map()

function getImageSrc(artwork) {
  if (blobUrlCache.has(artwork.id)) return blobUrlCache.get(artwork.id)

  if (artwork.imageBase64) {
    try {
      const byteChars = atob(artwork.imageBase64)
      const byteArrays = new Uint8Array(byteChars.length)
      for (let i = 0; i < byteChars.length; i++) {
        byteArrays[i] = byteChars.charCodeAt(i)
      }
      const blob = new Blob([byteArrays], { type: 'image/png' })
      const blobUrl = URL.createObjectURL(blob)
      blobUrlCache.set(artwork.id, blobUrl)
      return blobUrl
    } catch {
      // fallback to data URI
    }
  }

  return artwork.imageUrl || `/api/v1/images/${artwork.storedFilename}`
}

function revokeAllBlobUrls() {
  for (const url of blobUrlCache.values()) {
    URL.revokeObjectURL(url)
  }
  blobUrlCache.clear()
}

const handleImageError = (event) => {
  event.target.src =
    'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgZmlsbD0iI2Y1ZjVmNSIvPjx0ZXh0IHg9IjEwMCIgeT0iMTAwIiBmb250LWZhbWlseT0iQXJpYWwiIGZvbnQtc2l6ZT0iMTQiIGZpbGw9IiM5OTkiIHRleHQtYW5jaG9yPSJtaWRkbGUiIGR5PSIuM2VtIj7lm77niYfkuI3lrZjlnKg8L3RleHQ+PC9zdmc+'
}

// 跳转到创作中心
const goToStudio = () => {
  window.dispatchEvent(new CustomEvent('switchToStudio'))
}

// 跳转到关注列表
const goToFollowingList = () => {
  if (navigateToFollowingList) {
    navigateToFollowingList()
  }
}

// 跳转到粉丝列表
const goToFollowersList = () => {
  if (navigateToFollowersList) {
    navigateToFollowersList()
  }
}

// --- 事件监听 ---

const handleNewDrawingEvent = () => {
  console.log('🎨 [Profile] 监听到新图片生成事件，刷新本地数据')
  setTimeout(() => {
    fetchUserArtworks()
    fetchProfileHome()
  }, 500)
}

// 监听用户登出事件，清理数据
const handleLogoutEvent = () => {
  console.log('🚪 [Profile] 监听到用户登出，清理个人中心数据')
  revokeAllBlobUrls()
  stats.value = null
  artworkList.artworks = []
  artworkList.totalCount = 0
  artworkList.currentPage = 0
  artworkList.totalPages = 0
  currentPage.value = 1

  // 关闭弹窗
  detailModalVisible.value = false
  selectedArtwork.value = null
}

// 监听关注状态变化事件，更新统计数据
const handleFollowStatusChange = (event) => {
  const { type, source } = event.detail
  console.log(`📡 [Profile] 收到关注状态变化: ${type}, source: ${source}`)

  // 当前用户进行关注/取消关注操作时，更新自己的关注数
  if (stats.value && (source === 'UserProfile' || source === 'FollowingList')) {
    if (type === 'follow') {
      stats.value.followingCount = (stats.value.followingCount || 0) + 1
      console.log(`✅ [Profile] 关注数+1: ${stats.value.followingCount}`)
    } else if (type === 'unfollow') {
      stats.value.followingCount = Math.max(0, (stats.value.followingCount || 0) - 1)
      console.log(`✅ [Profile] 关注数-1: ${stats.value.followingCount}`)
    }
  }
}

// --- 生命周期 ---
onMounted(() => {
  console.log('🚀 [Profile] 组件开始挂载')

  // 检查用户是否已登录
  const userInfoData = localStorage.getItem('userInfo')

  if (!userInfoData) {
    console.warn('⚠️ [Profile] 用户未登录，跳过数据加载')
    ElMessage.warning('请先登录')
    return
  }

  try {
    fetchProfileHome()
    fetchUserArtworks()

    // 注册事件监听器
    window.addEventListener('drawingCompleted', handleNewDrawingEvent)
    window.addEventListener('userLogout', handleLogoutEvent)
    window.addEventListener('followStatusChange', handleFollowStatusChange)
    checkScreenSize()
    window.addEventListener('resize', checkScreenSize)

    console.log('✅ [Profile] 组件挂载完成')
  } catch (error) {
    console.error('❌ [Profile] 组件挂载失败:', error)
  }
})

onActivated(() => {
  console.log('🔄 [Profile] 组件重新激活，刷新数据')
  const userInfoData = localStorage.getItem('userInfo')
  if (userInfoData) {
    fetchProfileHome()
    fetchUserArtworks()
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', checkScreenSize)
  window.removeEventListener('drawingCompleted', handleNewDrawingEvent)
  window.removeEventListener('userLogout', handleLogoutEvent)
  window.removeEventListener('followStatusChange', handleFollowStatusChange)
  revokeAllBlobUrls()
})
</script>

<style scoped>
.profile-container {
  padding: 20px;
  background-color: #f0f2f5;
  min-height: 100vh;
}

/* 个人信息头部 */
.profile-header {
  margin-bottom: 20px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 20px;
}

.user-details h2 {
  margin: 0 0 5px 0;
  color: #303133;
  font-size: 24px;
}

.user-email {
  color: #909399;
  margin: 0 0 5px 0;
}

.join-date {
  color: #606266;
  margin: 0;
  font-size: 14px;
}

.user-stats {
  display: flex;
  gap: 30px;
  margin-left: auto;
}

.stat-item {
  text-align: center;
}

.stat-item.clickable {
  cursor: pointer;
  transition: all 0.3s ease;
  padding: 8px;
  border-radius: 8px;
}

.stat-item.clickable:hover {
  background-color: #f0f9ff;
  color: #409eff;
  transform: translateY(-2px);
}

.stat-number {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
  margin-bottom: 5px;
}

.stat-label {
  font-size: 12px;
  color: #909399;
}

.credits-card {
  margin-bottom: 20px;
}

.credits-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.credits-balance {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #409eff;
}

.balance-number {
  font-size: 28px;
  font-weight: 700;
}

.balance-label {
  font-size: 16px;
  color: #909399;
}

.credits-hint {
  margin: 8px 0 0;
  color: #909399;
  font-size: 12px;
}

/* 作品管理区域 */
.artworks-section {
  min-height: 600px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: bold;
}

.artworks-content {
  min-height: 400px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 300px;
  text-align: center;
}

/* 作品网格 */
.artworks-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
  margin-top: 20px;
}

.artwork-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

.artwork-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
}

.artwork-image-container {
  position: relative;
  aspect-ratio: 2 / 3;
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

/* 分享状态标识 */
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
  gap: 10px;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.artwork-card:hover .artwork-overlay {
  opacity: 1;
}

.artwork-overlay .el-button.shared {
  background-color: #67c23a;
  border-color: #67c23a;
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

/* 响应式设计 */
@media (max-width: 768px) {
  .profile-container {
    padding: 12px;
  }
  .user-info {
    flex-direction: column;
    text-align: center;
  }
  .user-stats {
    margin-left: 0;
    margin-top: 20px;
    flex-wrap: wrap;
    justify-content: center;
    gap: 20px;
  }
  .stat-number {
    font-size: 20px;
  }
  .credits-section {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }
  .artworks-grid {
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
    gap: 15px;
  }
  .artwork-overlay {
    opacity: 1;
    background: rgba(0, 0, 0, 0.25);
  }
  .artwork-card:hover .artwork-overlay,
  .artwork-card:hover .artwork-image {
    transform: none;
  }
}

@media (max-width: 480px) {
  .profile-container {
    padding: 8px;
  }
  .user-stats {
    gap: 12px;
  }
  .stat-number {
    font-size: 18px;
  }
  .user-details h2 {
    font-size: 20px;
  }
  .artworks-grid {
    grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
    gap: 10px;
  }
  .balance-number {
    font-size: 24px;
  }
}
</style>
