<template>
  <el-dialog v-model="visible" align-center append-to-body width="760px"
    class="login-dialog" :destroy-on-close="false" @closed="onClosed">
    <div class="login-container">
      <!-- 左侧装饰 -->
      <div class="login-art">
        <div class="art-content">
          <span class="art-icon">🔄</span>
          <h2>青禾集市</h2>
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
          <div class="input-group">
            <el-icon class="input-prefix"><Iphone /></el-icon>
            <input v-model="form.account" type="text"
              :placeholder="loginType===1?'手机号 或 邮箱':'输入手机号或邮箱获取验证码'"
              class="modern-input" />
            <span class="input-hint" v-if="detectedType">{{ detectedType === 'email' ? '📧 邮箱' : '📱 手机' }}</span>
          </div>

          <div class="input-group" v-if="loginType === 1">
            <el-icon class="input-prefix"><Lock /></el-icon>
            <input v-model="form.password" :type="showPwd ? 'text' : 'password'" placeholder="密码" class="modern-input" />
            <button type="button" class="input-suffix" @click="showPwd=!showPwd">
              <el-icon><View v-if="!showPwd"/><Hide v-else/></el-icon>
            </button>
          </div>

          <div class="input-group" v-if="loginType === 2">
            <el-icon class="input-prefix"><Message /></el-icon>
            <input v-model="form.smsCode" type="text" placeholder="验证码" class="modern-input" style="flex:1" />
            <button type="button" class="sms-btn" :disabled="countdown>0" @click="sendCode">
              {{ countdown > 0 ? countdown + 's' : '获取验证码' }}
            </button>
          </div>

          <div class="pwd-hints" v-if="loginType === 1">
            <span><el-icon><WarnTriangleFilled /></el-icon> 密码 8-32 位，需含字母和数字</span>
          </div>

          <button class="submit-btn" @click.prevent="handleSubmit" :disabled="loading">
            <span v-if="!loading">{{ loginType === 1 ? '登 录' : '验证并登录' }}</span>
            <el-icon v-else class="loading-icon"><Loading /></el-icon>
          </button>
        </el-form>

        <div class="form-footer">
          <a href="javascript:void(0)" @click="goRoute('/forgot-password')" v-if="loginType === 1">忘记密码？</a>
          <span v-else>&nbsp;</span>
          <a href="javascript:void(0)" @click="goRoute('/register')">去注册（可选更多信息）</a>
        </div>

        <div class="admin-entry" @click="goAdmin">
          <el-icon :size="14"><Lock /></el-icon> 管理员登录
          <el-icon :size="12"><Right /></el-icon>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/userStore'
import { sendSms } from '@/api/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const store = useUserStore()

const visible = computed({
  get: () => store.loginDialogVisible,
  set: (v) => { if (!v) store.closeLoginDialog() }
})

const loginType = ref(1)
const loading = ref(false)
const countdown = ref(0)
const showPwd = ref(false)
const form = ref({ account: '', password: '', smsCode: '' })
let countdownTimer = null

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
    countdownTimer = setInterval(() => { countdown.value--; if (countdown.value <= 0) { clearInterval(countdownTimer); countdownTimer = null } }, 1000)
  } catch (e) { /* 拦截器已提示 */ }
}

const handleSubmit = async () => {
  if (!form.value.account) { ElMessage.warning('请输入账号'); return }
  if (loginType.value === 1 && !form.value.password) { ElMessage.warning('请输入密码'); return }
  if (loginType.value === 2 && !form.value.smsCode) { ElMessage.warning('请输入验证码'); return }
  loading.value = true
  try {
    const data = {
      account: form.value.account,
      loginType: loginType.value,
      password: loginType.value === 1 ? form.value.password : undefined,
      smsCode: loginType.value === 2 ? form.value.smsCode : undefined
    }
    await store.login(data)
    ElMessage.success(loginType.value === 2 ? '登录成功（自动注册）' : '登录成功')
    store.closeLoginDialog()
    store.runAfterLogin()
  } catch (e) { /* 拦截器已提示 */ }
  loading.value = false
}

// 跳注册/忘记密码：先关弹窗再走路由
const goRoute = (path) => { store.closeLoginDialog(); router.push(path) }

const goAdmin = () => {
  const url = import.meta.env.DEV ? 'http://localhost:5174/' : '/admin/'
  window.open(url, '_blank', 'noopener')
}

const resetForm = () => {
  loginType.value = 1
  showPwd.value = false
  form.value = { account: '', password: '', smsCode: '' }
  if (countdownTimer) { clearInterval(countdownTimer); countdownTimer = null }
  countdown.value = 0
}

const onClosed = () => resetForm()

// 登录态变化：若外部已登出导致弹窗仍开，保持关闭
watch(() => store.isLoggedIn(), (loggedIn) => { if (loggedIn) store.closeLoginDialog() })
</script>

<style scoped>
.login-container {
  display: flex; max-width: 720px; width: 100%;
  background: rgba(255,255,255,0.98); border-radius: 20px; overflow: hidden;
}
.login-art {
  flex: 0 0 300px;
  background: linear-gradient(135deg, var(--c2c-primary) 0%, var(--c2c-primary-light) 50%, var(--c2c-primary-dark) 100%);
  display: flex; align-items: center; justify-content: center; padding: 40px;
  position: relative; overflow: hidden;
}
.login-art::before {
  content: ''; position: absolute; inset: 0;
  background: radial-gradient(circle at 30% 40%, rgba(255,255,255,0.15) 0%, transparent 60%);
}
.art-content { text-align: center; color: #fff; }
.art-icon { font-size: 52px; display: block; margin-bottom: 12px; }
.art-content h2 { font-size: 26px; font-weight: 800; margin-bottom: 4px; }
.art-content p { font-size: 14px; opacity: 0.85; margin-bottom: 28px; }
.art-features { display: flex; flex-direction: column; gap: 12px; text-align: left; }
.art-feat { display: flex; align-items: center; gap: 10px; font-size: 14px; opacity: 0.9; }
.art-feat span { font-size: 20px; }

.login-form-wrap { flex: 1; padding: 32px 32px 24px; display: flex; flex-direction: column; }
.login-form-wrap h3 { font-size: 20px; font-weight: 700; color: var(--c2c-text); }
.form-subtitle { font-size: 13px; color: var(--c2c-text-muted); margin: 6px 0 16px; }
.auto-reg-tip { color: var(--c2c-accent); font-weight: 600; }

.mode-switch { display: flex; background: var(--c2c-bg-soft); border-radius: 12px; padding: 4px; margin-bottom: 20px; }
.mode-switch button {
  flex: 1; padding: 9px; border: none; background: transparent;
  font-size: 14px; font-weight: 600; color: var(--c2c-text-secondary); cursor: pointer;
  border-radius: 10px; transition: all 0.2s;
}
.mode-switch button.active { background: #fff; color: var(--c2c-primary); box-shadow: 0 2px 8px rgba(0,0,0,0.08); }

.input-group {
  display: flex; align-items: center; background: var(--c2c-bg); border-radius: 14px;
  padding: 0 16px; height: 46px; margin-bottom: 14px;
  border: 2px solid transparent; transition: all 0.25s;
}
.input-group:focus-within { background: #fff; border-color: var(--c2c-primary); box-shadow: 0 0 0 4px rgba(255,107,53,0.08); }
.input-prefix { font-size: 18px; color: var(--c2c-text-muted); margin-right: 10px; flex-shrink: 0; }
.modern-input { flex: 1; border: none; outline: none; background: transparent; font-size: 15px; color: var(--c2c-text); }
.modern-input::placeholder { color: var(--c2c-text-muted); }
.input-hint { font-size: 12px; color: var(--c2c-primary); font-weight: 600; white-space: nowrap; }
.input-suffix { border: none; background: none; cursor: pointer; color: var(--c2c-text-muted); font-size: 16px; padding: 4px; }

.sms-btn {
  border: none; background: #fff5f0; color: var(--c2c-primary);
  padding: 8px 16px; border-radius: 10px; font-size: 13px; font-weight: 600;
  cursor: pointer; white-space: nowrap; transition: all 0.2s; flex-shrink: 0;
}
.sms-btn:hover:not(:disabled) { background: var(--c2c-primary); color: #fff; }
.sms-btn:disabled { opacity: 0.5; cursor: not-allowed; }

.pwd-hints { font-size: 12px; color: var(--c2c-text-muted); margin: -6px 0 12px 4px; display: flex; align-items: center; gap: 4px; }
.pwd-hints .el-icon { font-size: 14px; color: var(--c2c-warning); }

.submit-btn {
  width: 100%; height: 46px; border: none;
  background: linear-gradient(135deg, var(--c2c-primary), var(--c2c-primary-light));
  color: #fff; font-size: 16px; font-weight: 700; border-radius: 14px;
  cursor: pointer; margin-top: 6px; transition: all 0.3s;
  display: flex; align-items: center; justify-content: center;
}
.submit-btn:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(255,107,53,0.35); }
.submit-btn:disabled { opacity: 0.6; cursor: not-allowed; transform: none; }
.loading-icon { animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.form-footer { display: flex; justify-content: space-between; margin-top: 14px; font-size: 13px; }
.form-footer a { color: var(--c2c-primary); font-weight: 500; }

.admin-entry {
  margin-top: 16px; padding-top: 12px; border-top: 1px dashed #eee;
  display: flex; align-items: center; justify-content: center; gap: 4px;
  font-size: 13px; color: var(--c2c-text-muted); cursor: pointer; transition: all 0.2s;
}
.admin-entry:hover { color: var(--c2c-primary); }

@media (max-width: 768px) {
  .login-container { flex-direction: column; max-width: 420px; }
  .login-art { display: none; }
  .login-form-wrap { padding: 24px 20px; }
}
</style>
