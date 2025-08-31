<!-- src/components/LoginDialog.vue -->
<!-- 这是登录对话框组件 -->
<script setup>
import { ref, defineEmits, defineProps } from 'vue';
import { ElMessage } from 'element-plus';

// --- Props & Emits ---
const props = defineProps({
  modelValue: Boolean, // 用于 v-model
});
const emit = defineEmits(['update:modelValue', 'login-success']);

// --- 状态管理 ---
const username = ref('');
const password = ref('');
const isLoading = ref(false);

// --- 函数 ---
const handleLogin = () => {
  if (!username.value || !password.value) {
    ElMessage.error('请输入用户名和密码！');
    return;
  }
  
  isLoading.value = true;
  // 模拟一个网络请求
  setTimeout(() => {
    console.log(`模拟登录: 用户名=${username.value}, 密码=${password.value}`);
    isLoading.value = false;
    ElMessage.success('登录成功！');
    emit('login-success'); // 触发登录成功事件
  }, 1000);
};

const closeDialog = () => {
  emit('update:modelValue', false);
};
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    title="欢迎回来"
    width="30%"
    :before-close="closeDialog"
    center
  >
    <el-form label-position="top" @submit.prevent="handleLogin">
      <el-form-item label="用户名">
        <el-input v-model="username" placeholder="请输入用户名" />
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model="password" type="password" placeholder="请输入密码" show-password />
      </el-form-item>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="closeDialog">取消</el-button>
        <el-button type="primary" @click="handleLogin" :loading="isLoading">
          {{ isLoading ? '登录中...' : '登录' }}
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>