<template>
  <div class="login-page">
    <div class="login-container fade-in-up">
      <!-- 左侧装饰 -->
      <div class="login-art">
        <div class="art-content">
          <span class="art-icon">🔄</span>
          <h2>闲小鱼</h2>
          <p>让闲置流转起来</p>
          <div class="art-features">
            <div class="art-feat"><span>📧</span> 邮箱注册</div>
            <div class="art-feat"><span>📱</span> 手机绑定</div>
            <div class="art-feat"><span>🔒</span> 安全交易</div>
            <div class="art-feat"><span>⚡</span> 秒级登录</div>
          </div>
        </div>
      </div>

      <!-- 右侧表单 -->
      <div class="login-form-wrap">
        <h3>{{ loginType === 1 ? '账号密码登录' : '验证码登录' }}</h3>
        <p class="form-subtitle">
          {{ loginType === 1 ? '输入手机号或邮箱 + 密码' : '输入手机号或邮箱，验证码直接登录' }}
          <span class="auto-reg-tip" v-if="loginType === 2">· 未注册则自动创建账号</span>
        </p>

        <!-- 模式切换 -->
        <div class="mode-switch">
          <button type="button" :class="{ active: loginType === 1 }" @click="loginType=1">密码登录</button>
          <button type="button" :class="{ active: loginType === 2 }" @click="loginType=2">验证码登录</button>
        </div>

        <el-form :model="form" label-width="0" size="large" class="login-form">
          <!-- 账号输入 -->
          <div class="input-group">
            <el-icon class="input-prefix"><Iphone /></el-icon>
            <input v-model="form.account" type="text"
              :placeholder="loginType===1?'手机号 或 邮箱':'输入手机号或邮箱获取验证码'"
              class="modern-input" />
            <span class="input-hint" v-if="detectedType">{{ detectedType === 'email' ? '📧 邮箱' : '📱 手机' }}</span>
          </div>

          <!-- 密码输入（密码登录模式） -->
          <div class="input-group" v-if="loginType === 1">
            <el-icon class="input-prefix"><Lock /></el-icon>
            <input v-model="form.password" :type="showPwd ? 'text' : 'password'" placeholder="密码" class="modern-input" />
            <button type="button" class="input-suffix" @click="showPwd=!showPwd">
              <el-icon><View v-if="!showPwd"/><Hide v-else/></el-icon>
            </button>
          </div>

          <!-- 验证码输入（验证码登录模式） -->
          <div class="input-group" v-if="loginType === 2">
            <el-icon class="input-prefix"><Message /></el-icon>
            <input v-model="form.smsCode" type="text" placeholder="验证码" class="modern-input" style="flex:1" />
            <button type="button" class="sms-btn" :disabled="countdown>0" @click="sendCode">
              {{ countdown > 0 ? countdown + 's' : '获取验证码' }}
            </button>
          </div>

          <!-- 密码提示 -->
          <div class="pwd-hints" v-if="loginType === 1">
            <span><el-icon><WarnTriangleFilled /></el-icon> 密码 8-32 位，需含字母和数字</span>
          </div>

          <!-- 提交 -->
          <button class="submit-btn" @click.prevent="handleSubmit" :disabled="loading">
            <span v-if="!loading">{{ loginType === 1 ? '登 录' : '验证并登录' }}</span>
            <el-icon v-else class="loading-icon"><Loading /></el-icon>
          </button>
        </el-form>

        <!-- 底部链接 -->
        <div class="form-footer">
          <router-link to="/forgot-password" v-if="loginType === 1">忘记密码？</router-link>
          <span v-else>&nbsp;</span>
          <a href="javascript:void(0)" @click="$router.push('/register')">去注册（可选更多信息）</a>
        </div>

        <!-- 管理员入口：快速到达管理后台 -->
        <div class="admin-entry" @click="goAdmin">
          <el-icon :size="14"><Lock /></el-icon> 管理员登录
          <el-icon :size="12"><Right /></el-icon>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/userStore'
import { sendSms, login } from '@/api/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const store = useUserStore()
const loginType = ref(1)
const loading = ref(false)
const countdown = ref(0)
const showPwd = ref(false)
const form = ref({ account: '', password: '', smsCode: '' })

// 智能检测输入类型
const detectedType = computed(() => {
  const v = form.value.account
  if (!v) return ''
  if (v.includes('@')) return 'email'
  if (/^1\d{10}$/.test(v)) return 'phone'
  if (v.length >= 5 && !v.includes('@')) return 'phone'
  return ''
})

const sendCode = async () => {
  if (!form.value.account) { ElMessage.warning('请输入手机号或邮箱'); return }
  try {
    const res = await sendSms(form.value.account)
    if (res.type === 'email') {
      ElMessage.success('验证码已发送到邮箱，请查收')
    } else {
      ElMessage.success(`验证码: ${res.code}`)
    }
    countdown.value = 60
    const t = setInterval(() => { countdown.value--; if (countdown.value <= 0) clearInterval(t) }, 1000)
  } catch (e) { /* */ }
}

const handleSubmit = async () => {
  if (!form.value.account) { ElMessage.warning('请输入账号'); return }
  loading.value = true
  try {
    const data = {
      account: form.value.account,
      loginType: loginType.value,
      password: loginType.value === 1 ? form.value.password : undefined,
      smsCode: loginType.value === 2 ? form.value.smsCode : undefined
    }

    // 验证码登录不验密码，密码登录不验验证码
    if (loginType.value === 1 && !form.value.password) { ElMessage.warning('请输入密码'); loading.value = false; return }
    if (loginType.value === 2 && !form.value.smsCode) { ElMessage.warning('请输入验证码'); loading.value = false; return }

    const res = await login(data)
    store.token = res.token
    store.userInfo = res.userInfo
    localStorage.setItem('token', res.token)
    localStorage.setItem('userInfo', JSON.stringify(res.userInfo))
    ElMessage.success(loginType.value === 2 && res.userInfo ? '登录成功（自动注册）' : '登录成功')
    router.push(route.query.redirect || '/')
  } catch (e) { /* handled */ }
  loading.value = false
}

// 管理员入口：开发环境跳本地管理端(5174)，生产跳 /admin/
const goAdmin = () => {
  const url = import.meta.env.DEV ? 'http://localhost:5174/' : '/admin/'
  window.open(url, '_blank', 'noopener')
}
</script>

<style scoped>
.login-page {
  min-height: calc(100vh - 60px); display: flex; align-items: center; justify-content: center;
  padding: 40px 20px; background: linear-gradient(135deg, #fff5f0 0%, #f8f9fa 50%, #fff 100%);
}
.login-container {
  display: flex; max-width: 880px; width: 100%;
  background: #fff; border-radius: 24px; box-shadow: 0 20px 60px rgba(0,0,0,0.08); overflow: hidden;
}
.login-art {
  flex: 0 0 360px; background: linear-gradient(135deg, #ff6b35 0%, #ff8c5a 50%, #e55a2b 100%);
  display: flex; align-items: center; justify-content: center; padding: 48px;
}
@media (max-width: 768px) {
  .login-container { flex-direction: column; max-width: 420px; margin: 0 12px; }
  .login-art { display: none; }
  .login-form-wrap { padding: 28px 20px; }
  .login-form-wrap h3 { font-size: 20px; }
  .input-group { height: 44px; }
  .submit-btn { height: 46px; font-size: 15px; }
}
@media (max-width: 480px) {
  .login-container { border-radius: 16px; margin: 0 8px; }
  .login-form-wrap { padding: 20px 16px; }
  .login-form-wrap h3 { font-size: 18px; }
  .form-subtitle { font-size: 12px; }
  .mode-switch button { font-size: 13px; padding: 8px; }
}
.art-content { text-align: center; color: #fff; }
.art-icon { font-size: 56px; display: block; margin-bottom: 12px; }
.art-content h2 { font-size: 28px; font-weight: 800; margin-bottom: 4px; }
.art-content p { font-size: 14px; opacity: 0.85; margin-bottom: 32px; }
.art-features { display: flex; flex-direction: column; gap: 12px; text-align: left; }
.art-feat { display: flex; align-items: center; gap: 10px; font-size: 14px; opacity: 0.9; }
.art-feat span { font-size: 20px; }

.login-form-wrap { flex: 1; padding: 40px; display: flex; flex-direction: column; }
.login-form-wrap h3 { font-size: 22px; font-weight: 700; color: #2d3436; }
.form-subtitle { font-size: 13px; color: #b2bec3; margin: 6px 0 16px; }
.auto-reg-tip { color: #00b894; font-weight: 600; }

/* 模式切换 */
.mode-switch { display: flex; background: #f0f2f5; border-radius: 12px; padding: 4px; margin-bottom: 24px; }
.mode-switch button {
  flex: 1; padding: 10px; border: none; background: transparent;
  font-size: 14px; font-weight: 600; color: #636e72; cursor: pointer;
  border-radius: 10px; transition: all 0.2s;
}
.mode-switch button.active { background: #fff; color: #ff6b35; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }

/* 输入组 */
.input-group {
  display: flex; align-items: center; background: #f8f9fa; border-radius: 14px;
  padding: 0 16px; height: 50px; margin-bottom: 14px;
  border: 2px solid transparent; transition: all 0.25s;
}
.input-group:focus-within { background: #fff; border-color: #ff6b35; box-shadow: 0 0 0 4px rgba(255,107,53,0.08); }
.input-prefix { font-size: 18px; color: #b2bec3; margin-right: 10px; flex-shrink: 0; }
.modern-input { flex: 1; border: none; outline: none; background: transparent; font-size: 15px; color: #2d3436; }
.modern-input::placeholder { color: #b2bec3; }
.input-hint { font-size: 12px; color: #ff6b35; font-weight: 600; white-space: nowrap; }
.input-suffix { border: none; background: none; cursor: pointer; color: #b2bec3; font-size: 16px; padding: 4px; }

.sms-btn {
  border: none; background: #fff5f0; color: #ff6b35;
  padding: 8px 16px; border-radius: 10px; font-size: 13px; font-weight: 600;
  cursor: pointer; white-space: nowrap; transition: all 0.2s; flex-shrink: 0;
}
.sms-btn:hover:not(:disabled) { background: #ff6b35; color: #fff; }
.sms-btn:disabled { opacity: 0.5; cursor: not-allowed; }

.pwd-hints { font-size: 12px; color: #b2bec3; margin: -6px 0 12px 4px; display: flex; align-items: center; gap: 4px; }
.pwd-hints .el-icon { font-size: 14px; color: #fdcb6e; }

.submit-btn {
  width: 100%; height: 50px; border: none;
  background: linear-gradient(135deg, #ff6b35, #ff8c5a);
  color: #fff; font-size: 16px; font-weight: 700; border-radius: 14px;
  cursor: pointer; margin-top: 6px; transition: all 0.3s;
  display: flex; align-items: center; justify-content: center;
}
.submit-btn:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(255,107,53,0.35); }
.submit-btn:disabled { opacity: 0.6; cursor: not-allowed; transform: none; }
.loading-icon { animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.form-footer { display: flex; justify-content: space-between; margin-top: 16px; font-size: 13px; }
.form-footer a { color: #ff6b35; font-weight: 500; }

/* 管理员入口 */
.admin-entry {
  margin-top: 20px; padding-top: 14px; border-top: 1px dashed #eee;
  display: flex; align-items: center; justify-content: center; gap: 4px;
  font-size: 13px; color: #b2bec3; cursor: pointer; transition: all 0.2s;
}
.admin-entry:hover { color: #ff6b35; }
</style>
