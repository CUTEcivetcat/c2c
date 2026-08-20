<template>
  <div style="display:flex;align-items:center;justify-content:center;height:100vh;background:linear-gradient(135deg,#1a1a2e,#16213e)">
    <div style="background:#fff;border-radius:16px;padding:40px;width:380px;box-shadow:0 20px 60px rgba(0,0,0,0.3)">
      <h2 style="text-align:center;margin-bottom:24px;font-size:22px">🔐 管理员登录</h2>
      <el-input v-model="username" placeholder="管理员账号" size="large" style="margin-bottom:12px" clearable />
      <el-input v-model="password" type="password" placeholder="密码" show-password size="large" style="margin-bottom:12px" @keyup.enter="login" />
      <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:20px">
        <el-checkbox v-model="remember">记住账号</el-checkbox>
        <span style="font-size:12px;color:#909399">勾选后下次自动填充账号密码</span>
      </div>
      <el-button type="primary" size="large" style="width:100%;height:48px;border-radius:12px" @click="login" :loading="loading">登 录</el-button>
      <div style="margin-top:14px;font-size:12px;color:#909399;text-align:center">演示账号：admin / Abc123456</div>
      <div style="margin-top:10px;text-align:center">
        <a href="javascript:void(0)" style="font-size:12px;color:#909399;text-decoration:none" @click="goUser">← 返回用户端</a>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api/request'
const router = useRouter()
const username = ref('admin')
const password = ref('Abc123456')
const remember = ref(true)
const loading = ref(false)

onMounted(() => {
  // 已有有效 token：直接进入后台，无需重复登录
  if (localStorage.getItem('admin_token')) {
    router.replace('/dashboard')
    return
  }
  // 填充记住的账号密码
  try {
    const saved = JSON.parse(localStorage.getItem('admin_remember') || 'null')
    if (saved && saved.username) {
      username.value = saved.username
      password.value = saved.password || ''
      remember.value = true
    }
  } catch (e) { /* 解析失败忽略 */ }
})

const login = async () => {
  if (!username.value.trim() || !password.value) {
    ElMessage.warning('请输入管理员账号和密码'); return
  }
  loading.value = true
  try {
    const res = await adminApi.adminLogin({ account: username.value.trim(), password: password.value })
    localStorage.setItem('admin_token', res.token)
    localStorage.setItem('admin_user', JSON.stringify(res.userInfo || {}))
    // 记住账号密码（下次自动填充）
    if (remember.value) {
      localStorage.setItem('admin_remember', JSON.stringify({ username: username.value.trim(), password: password.value }))
    } else {
      localStorage.removeItem('admin_remember')
    }
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (e) {
    // 错误提示已由拦截器统一处理
  } finally {
    loading.value = false
  }
}

// 返回用户端：开发环境跳本地用户端(5173)，生产跳根路径
const goUser = () => {
  const url = import.meta.env.DEV ? 'http://localhost:5173/' : '/'
  window.location.href = url
}
</script>
