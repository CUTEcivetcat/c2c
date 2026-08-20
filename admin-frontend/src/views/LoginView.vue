<template>
  <div style="display:flex;align-items:center;justify-content:center;height:100vh;background:#1a1a2e">
    <div style="background:#fff;border-radius:16px;padding:40px;width:380px;box-shadow:0 20px 60px rgba(0,0,0,0.3)">
      <h2 style="text-align:center;margin-bottom:24px;font-size:22px">🔐 管理员登录</h2>
      <el-input v-model="username" placeholder="管理员账号" size="large" style="margin-bottom:12px" />
      <el-input v-model="password" type="password" placeholder="密码" show-password size="large" style="margin-bottom:20px" @keyup.enter="login" />
      <el-button type="primary" size="large" style="width:100%;height:48px;border-radius:12px" @click="login" :loading="loading">登 录</el-button>
      <div style="margin-top:14px;font-size:12px;color:#909399;text-align:center">演示账号：admin / Abc123456</div>
    </div>
  </div>
</template>
<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api/request'
const router = useRouter()
const username = ref('admin')
const password = ref('Abc123456')
const loading = ref(false)
const login = async () => {
  if (!username.value.trim() || !password.value) {
    ElMessage.warning('请输入管理员账号和密码'); return
  }
  loading.value = true
  try {
    const res = await adminApi.adminLogin({ account: username.value.trim(), password: password.value })
    localStorage.setItem('admin_token', res.token)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (e) {
    // 错误提示已由拦截器统一处理
  } finally {
    loading.value = false
  }
}
</script>
