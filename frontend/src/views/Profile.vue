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
          <div class="stat-item">
            <div class="stat-number">{{ formatDate(stats.lastActiveDate) }}</div>
            <div class="stat-label">最后活跃</div>
          </div>
        </div>
      </div>
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
          <el-icon :size="60" style="color: #c0c4cc;"><Picture /></el-icon>
          <p style="color: #909399; margin-top: 15px;">还没有创作作品</p>
          <el-button type="primary" @click="goToStudio">开始创作</el-button>
        </div>
        
        <div v-else class="artworks-grid">
          <div 
            v-for="artwork in artworkList.artworks" 
            :key="artwork.id"
            class="artwork-card"
          >
            <!-- 图片 -->
            <div class="artwork-image-container">
              <img 
                :src="`http://localhost:8080/api/v1/images/${artwork.storedFilename}`" 
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
                  :class="{ 'shared': artwork.sharedToGallery }"
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
                {{ artwork.prompt.length > 50 ? artwork.prompt.substring(0, 50) + '...' : artwork.prompt }}
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
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </div>
    </el-card>

    <!-- 作品详情弹窗 -->
    <ArtworkDetailModal 
      v-model:visible="detailModalVisible"
      :artwork="selectedArtwork"
    />
  </div>
</template>

<script setup>
import { ref, reactive, inject, onMounted, onUnmounted } from 'vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  UserFilled, 
  Picture, 
  Refresh, 
  View, 
  Share, 
  Delete 
} from '@element-plus/icons-vue'
import ArtworkDetailModal from '../components/ArtworkDetailModal.vue'

// --- 依赖注入 ---
const userInfo = inject('userInfo')

// --- 状态管理 ---
const loading = ref(false)
const stats = ref(null)
const artworkList = reactive({
  artworks: [],
  totalCount: 0,
  currentPage: 0,
  pageSize: 20,
  totalPages: 0
})

// 分页参数
const currentPage = ref(1)
const pageSize = ref(20)

// 作品详情弹窗
const detailModalVisible = ref(false)
const selectedArtwork = ref(null)

// --- API请求函数 ---

// 获取个人中心主页数据
const fetchProfileHome = async () => {
  try {
    const token = localStorage.getItem('accessToken')
    if (!token) {
      throw new Error('未登录')
    }

    const response = await axios.get('http://localhost:8080/api/v1/profile/home', {
      headers: { Authorization: `Bearer ${token}` }
    })

    stats.value = response.data.userStats
    // 如果有最近作品，也显示在列表中
    if (response.data.recentArtworks?.length > 0) {
      artworkList.artworks = response.data.recentArtworks
      artworkList.totalCount = response.data.userStats.totalArtworks
    }

    console.log('✅ [Profile] 个人中心数据加载成功')
  } catch (error) {
    console.error('❌ [Profile] 获取个人中心数据失败:', error)
    handleAuthError(error)
  }
}

// 获取用户作品列表（分页）
const fetchUserArtworks = async (page = 0, size = pageSize.value) => {
  loading.value = true
  try {
    const token = localStorage.getItem('accessToken')
    if (!token) {
      throw new Error('未登录')
    }

    const response = await axios.get('http://localhost:8080/api/v1/profile/artworks', {
      params: { page, size },
      headers: { Authorization: `Bearer ${token}` }
    })

    artworkList.artworks = response.data.artworks
    artworkList.totalCount = response.data.totalCount
    artworkList.currentPage = response.data.currentPage
    artworkList.pageSize = response.data.pageSize
    artworkList.totalPages = response.data.totalPages

    console.log(`✅ [Profile] 作品列表加载成功: ${response.data.artworks.length} 张作品`)
  } catch (error) {
    console.error('❌ [Profile] 获取作品列表失败:', error)
    handleAuthError(error)
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

// --- 作品操作函数 ---

// 刷新作品列表
const refreshArtworks = () => {
  fetchUserArtworks(currentPage.value - 1, pageSize.value)
}

// 查看作品详情
const viewArtworkDetail = (artwork) => {
  selectedArtwork.value = artwork
  detailModalVisible.value = true
}

// 确认删除作品
const confirmDelete = async (artwork) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除作品"${artwork.prompt.substring(0, 30)}..."吗？`,
      '删除确认',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    
    await deleteArtwork(artwork)
  } catch (error) {
    if (error !== 'cancel') {
      console.error('❌ [Profile] 删除确认失败:', error)
    }
  }
}

// 删除作品
const deleteArtwork = async (artwork) => {
  try {
    const token = localStorage.getItem('accessToken')
    if (!token) {
      throw new Error('未登录')
    }

    await axios.delete(`http://localhost:8080/api/v1/profile/artworks/${artwork.id}`, {
      headers: { Authorization: `Bearer ${token}` }
    })

    // 从本地列表中移除
    const index = artworkList.artworks.findIndex(item => item.id === artwork.id)
    if (index !== -1) {
      artworkList.artworks.splice(index, 1)
      artworkList.totalCount--
      
      // 更新统计信息
      if (stats.value) {
        stats.value.totalArtworks--
        if (artwork.sharedToGallery) {
          stats.value.sharedArtworks--
        }
      }
    }
    
    ElMessage.success('作品删除成功')
    console.log('✅ [Profile] 作品删除成功:', artwork.id)
    
  } catch (error) {
    console.error('❌ [Profile] 删除作品失败:', error)
    handleAuthError(error)
  }
}

// 切换分享状态
const toggleSharing = async (artwork) => {
  try {
    const newStatus = !artwork.sharedToGallery
    const token = localStorage.getItem('accessToken')
    
    await axios.put(
      `http://localhost:8080/api/v1/profile/artworks/${artwork.id}/sharing`,
      null,
      {
        params: { shareToGallery: newStatus },
        headers: { Authorization: `Bearer ${token}` }
      }
    )
    
    artwork.sharedToGallery = newStatus
    ElMessage.success(newStatus ? '作品已分享到画廊' : '已取消分享到画廊')
    console.log('🔄 [Profile] 分享状态已更新:', artwork.id, '→', newStatus)
    
  } catch (error) {
    console.error('❌ [Profile] 切换分享状态失败:', error)
    handleAuthError(error)
  }
}

// --- 分页处理 ---
const handleSizeChange = (newSize) => {
  pageSize.value = newSize
  currentPage.value = 1
  fetchUserArtworks(0, newSize)
}

const handleCurrentChange = (newPage) => {
  currentPage.value = newPage
  fetchUserArtworks(newPage - 1, pageSize.value)
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
const handleImageError = (event) => {
  event.target.src = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgZmlsbD0iI2Y1ZjVmNSIvPjx0ZXh0IHg9IjEwMCIgeT0iMTAwIiBmb250LWZhbWlseT0iQXJpYWwiIGZvbnQtc2l6ZT0iMTQiIGZpbGw9IiM5OTkiIHRleHQtYW5jaG9yPSJtaWRkbGUiIGR5PSIuM2VtIj7lm77niYfkuI3lrZjlnKg8L3RleHQ+PC9zdmc+'
}

// 跳转到创作中心
const goToStudio = () => {
  window.dispatchEvent(new CustomEvent('switchToStudio'))
}

// --- 事件监听 ---

// 监听新图片生成完成事件
const handleNewDrawingEvent = (event) => {
  console.log('🎨 [Profile] 监听到新图片生成事件，刷新个人中心数据')
  // 延迟一点刷新，确保后端数据已保存
  setTimeout(() => {
    fetchProfileHome()
    if (currentPage.value === 1) {
      // 如果在第一页，刷新作品列表
      fetchUserArtworks(0, pageSize.value)
    }
  }, 1000)
}

// 监听用户登出事件，清理数据
const handleLogoutEvent = () => {
  console.log('🚪 [Profile] 监听到用户登出，清理个人中心数据')
  // 清理所有数据
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

// --- 生命周期 ---
onMounted(() => {
  console.log('🚀 [Profile] 组件开始挂载')
  
  // 检查用户是否已登录
  const token = localStorage.getItem('accessToken')
  const userInfoData = localStorage.getItem('userInfo')
  
  if (!token || !userInfoData) {
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
    
    console.log('✅ [Profile] 组件挂载完成')
  } catch (error) {
    console.error('❌ [Profile] 组件挂载失败:', error)
  }
})

onUnmounted(() => {
  // 清理事件监听器
  window.removeEventListener('drawingCompleted', handleNewDrawingEvent)
  window.removeEventListener('userLogout', handleLogoutEvent)
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
  .user-info {
    flex-direction: column;
    text-align: center;
  }
  
  .user-stats {
    margin-left: 0;
    margin-top: 20px;
  }
  
  .artworks-grid {
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
    gap: 15px;
  }
}
</style>
