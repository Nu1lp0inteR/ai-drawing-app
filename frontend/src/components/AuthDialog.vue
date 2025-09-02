<!-- src/components/AuthDialog.vue -->
<!-- 企业级认证对话框组件 - 支持注册和登录 -->
<script setup>
import { ref, defineEmits, defineProps } from 'vue';
import { ElMessage } from 'element-plus';
import axios from 'axios';

// --- Props & Emits ---
const props = defineProps({
  modelValue: Boolean, // 用于 v-model
});
const emit = defineEmits(['update:modelValue', 'login-success']);

// --- 状态管理 ---
const activeTab = ref('login'); // 'login' 或 'register'
const isLoading = ref(false);

// 登录表单
const loginForm = ref({
  usernameOrEmail: '',
  password: ''
});

// 注册表单
const registerForm = ref({
  username: '',
  email: '',
  password: '',
  confirmPassword: ''
});

// --- 表单验证规则 ---
const loginRules = {
  usernameOrEmail: [
    { required: true, message: '请输入用户名或邮箱', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
};

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: '用户名只能包含字母、数字和下划线', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, message: '密码长度至少8位', trigger: 'blur' },
    { pattern: /^(?=.*[a-zA-Z])(?=.*\d)[a-zA-Z\d@$!%*?&]+$/, message: '密码必须包含至少一个字母和一个数字', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { 
      validator: (rule, value, callback) => {
        if (value !== registerForm.value.password) {
          callback(new Error('两次输入密码不一致'));
        } else {
          callback();
        }
      }, 
      trigger: 'blur' 
    }
  ]
};

// --- API 基础配置 ---
const API_BASE = 'http://localhost:8080/api/v1/auth';

// --- 登录函数 ---
const handleLogin = async () => {
  if (!loginForm.value.usernameOrEmail || !loginForm.value.password) {
    ElMessage.error('请填写完整的登录信息！');
    return;
  }
  
  isLoading.value = true;
  
  try {
    console.log('🔐 [Auth] 发送登录请求:', loginForm.value.usernameOrEmail);
    
    const response = await axios.post(`${API_BASE}/login`, {
      usernameOrEmail: loginForm.value.usernameOrEmail,
      password: loginForm.value.password
    });
    
    console.log('✅ [Auth] 登录响应:', response.data);
    
    if (response.data.success) {
      // 存储认证信息到 localStorage
      localStorage.setItem('accessToken', response.data.data.accessToken);
      localStorage.setItem('refreshToken', response.data.data.refreshToken);
      localStorage.setItem('userInfo', JSON.stringify(response.data.data.user));
      
      ElMessage.success(`欢迎回来，${response.data.data.user.username}！`);
      emit('login-success', response.data.data.user); // 传递用户信息
      closeDialog();
    } else {
      ElMessage.error(response.data.message || '登录失败');
    }
  } catch (error) {
    console.error('❌ [Auth] 登录错误:', error);
    if (error.response?.data?.message) {
      ElMessage.error(error.response.data.message);
    } else {
      ElMessage.error('登录失败，请检查网络连接');
    }
  } finally {
    isLoading.value = false;
  }
};

// --- 注册函数 ---
const handleRegister = async () => {
  if (!registerForm.value.username || !registerForm.value.email || 
      !registerForm.value.password || !registerForm.value.confirmPassword) {
    ElMessage.error('请填写完整的注册信息！');
    return;
  }
  
  if (registerForm.value.password !== registerForm.value.confirmPassword) {
    ElMessage.error('两次输入的密码不一致！');
    return;
  }
  
  isLoading.value = true;
  
  try {
    console.log('📝 [Auth] 发送注册请求:', registerForm.value.username, registerForm.value.email);
    
    const response = await axios.post(`${API_BASE}/register`, {
      username: registerForm.value.username,
      email: registerForm.value.email,
      password: registerForm.value.password
    });
    
    console.log('✅ [Auth] 注册响应:', response.data);
    
    if (response.data.success) {
      // 存储认证信息到 localStorage
      localStorage.setItem('accessToken', response.data.data.accessToken);
      localStorage.setItem('refreshToken', response.data.data.refreshToken);
      localStorage.setItem('userInfo', JSON.stringify(response.data.data.user));
      
      ElMessage.success(`注册成功，欢迎 ${response.data.data.user.username}！`);
      emit('login-success', response.data.data.user); // 传递用户信息
      closeDialog();
    } else {
      ElMessage.error(response.data.message || '注册失败');
    }
  } catch (error) {
    console.error('❌ [Auth] 注册错误:', error);
    if (error.response?.data?.message) {
      ElMessage.error(error.response.data.message);
    } else {
      ElMessage.error('注册失败，请检查网络连接');
    }
  } finally {
    isLoading.value = false;
  }
};

// --- 关闭对话框 ---
const closeDialog = () => {
  // 清空表单
  loginForm.value = { usernameOrEmail: '', password: '' };
  registerForm.value = { username: '', email: '', password: '', confirmPassword: '' };
  activeTab.value = 'login';
  emit('update:modelValue', false);
};

// --- 切换标签页 ---
const switchTab = (tab) => {
  activeTab.value = tab;
};
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    :title="activeTab === 'login' ? '欢迎回来' : '加入我们'"
    width="400px"
    :before-close="closeDialog"
    center
  >
    <!-- 标签页切换 -->
    <div class="auth-tabs">
      <el-button 
        :type="activeTab === 'login' ? 'primary' : 'default'"
        @click="switchTab('login')"
        :disabled="isLoading"
      >
        登录
      </el-button>
      <el-button 
        :type="activeTab === 'register' ? 'primary' : 'default'"
        @click="switchTab('register')"
        :disabled="isLoading"
      >
        注册
      </el-button>
    </div>

    <!-- 登录表单 -->
    <div v-if="activeTab === 'login'" class="auth-form">
      <el-form 
        :model="loginForm" 
        :rules="loginRules"
        label-position="top" 
        @submit.prevent="handleLogin"
        ref="loginFormRef"
      >
        <el-form-item label="用户名或邮箱" prop="usernameOrEmail">
          <el-input 
            v-model="loginForm.usernameOrEmail" 
            placeholder="请输入用户名或邮箱"
            :disabled="isLoading"
          />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input 
            v-model="loginForm.password" 
            type="password" 
            placeholder="请输入密码" 
            show-password
            :disabled="isLoading"
          />
        </el-form-item>
      </el-form>
    </div>

    <!-- 注册表单 -->
    <div v-else class="auth-form">
      <el-form 
        :model="registerForm" 
        :rules="registerRules"
        label-position="top" 
        @submit.prevent="handleRegister"
        ref="registerFormRef"
      >
        <el-form-item label="用户名" prop="username">
          <el-input 
            v-model="registerForm.username" 
            placeholder="3-20个字符，支持字母数字下划线"
            :disabled="isLoading"
          />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input 
            v-model="registerForm.email" 
            placeholder="请输入邮箱地址"
            :disabled="isLoading"
          />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input 
            v-model="registerForm.password" 
            type="password" 
            placeholder="至少8位，包含字母和数字" 
            show-password
            :disabled="isLoading"
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input 
            v-model="registerForm.confirmPassword" 
            type="password" 
            placeholder="请再次输入密码" 
            show-password
            :disabled="isLoading"
          />
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="closeDialog" :disabled="isLoading">取消</el-button>
        <el-button 
          type="primary" 
          @click="activeTab === 'login' ? handleLogin() : handleRegister()" 
          :loading="isLoading"
        >
          {{ isLoading ? (activeTab === 'login' ? '登录中...' : '注册中...') : (activeTab === 'login' ? '登录' : '注册') }}
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<style scoped>
.auth-tabs {
  display: flex;
  justify-content: center;
  margin-bottom: 20px;
  gap: 10px;
}

.auth-form {
  margin-top: 20px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
