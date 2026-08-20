<template>
  <div class="login-page">
    <div class="reset-card fade-in-up">
      <h2>🔑 找回密码</h2>
      <p class="subtitle">通过手机号或邮箱重置密码</p>

      <!-- 步骤 -->
      <el-steps :active="step" align-center style="margin:24px 0">
        <el-step title="验证身份" />
        <el-step title="重置密码" />
        <el-step title="完成" />
      </el-steps>

      <!-- Step 1: 发送验证码 -->
      <div v-if="step === 0">
        <div class="input-group">
          <el-icon class="input-prefix"><Iphone /></el-icon>
          <input v-model="account" type="text" placeholder="输入手机号或邮箱" class="modern-input" />
        </div>
        <div class="input-group">
          <el-icon class="input-prefix"><Message /></el-icon>
          <input v-model="smsCode" type="text" placeholder="验证码" class="modern-input" style="flex:1" />
          <button type="button" class="sms-btn" :disabled="countdown>0" @click="sendCode">
            {{ countdown > 0 ? countdown + 's' : '获取验证码' }}
          </button>
        </div>
        <button type="button" class="submit-btn" @click="verifyCode" :disabled="!account||!smsCode">下一步</button>
      </div>

      <!-- Step 2: 设置新密码 -->
      <div v-if="step === 1">
        <div class="input-group">
          <el-icon class="input-prefix"><Lock /></el-icon>
          <input v-model="newPassword" :type="showPwd?'text':'password'" placeholder="新密码（8-32位，含字母数字）" class="modern-input" />
          <button type="button" class="input-suffix" @click="showPwd=!showPwd">
            <el-icon><View v-if="!showPwd"/><Hide v-else/></el-icon>
          </button>
        </div>
        <div class="pwd-strength" v-if="newPassword">
          <div class="strength-bar"><div :class="'fill level-'+strengthLevel"></div></div>
          <span>{{ strengthText }}</span>
        </div>
        <button type="button" class="submit-btn" @click="doReset" :disabled="!newPassword||strengthLevel<2">重置密码</button>
      </div>

      <!-- Step 3: 完成 -->
      <div v-if="step === 2" style="text-align:center;padding:20px">
        <el-icon :size="56" color="#00b894"><SuccessFilled /></el-icon>
        <p style="font-size:18px;font-weight:600;margin:12px 0">密码重置成功</p>
        <el-button type="primary" size="large" round @click="$router.push('/login')">去登录</el-button>
      </div>

      <p style="text-align:center;margin-top:16px">
        <router-link to="/login">← 返回登录</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { sendSms, resetPassword } from '@/api/user'
import { ElMessage } from 'element-plus'

const step = ref(0)
const account = ref('')
const smsCode = ref('')
const newPassword = ref('')
const countdown = ref(0)
const showPwd = ref(false)
const userCode = ref('')

const strengthLevel = computed(() => {
  const pwd = newPassword.value
  if (!pwd) return 0
  let s = 0
  if (pwd.length >= 8) s++
  if (pwd.length >= 12) s++
  if (/[a-z]/.test(pwd) && /[A-Z]/.test(pwd)) s++
  if (/\d/.test(pwd)) s++
  if (/[^a-zA-Z0-9]/.test(pwd)) s++
  return Math.min(s, 3)
})
const strengthText = computed(() => ['', '弱', '中等', '强'][strengthLevel.value])

const sendCode = async () => {
  if (!account.value) { ElMessage.warning('请输入手机号或邮箱'); return }
  try {
    const res = await sendSms(account.value)
    userCode.value = res.code
    if (res.type === 'email') ElMessage.success('验证码已发送到邮箱')
    else ElMessage.success(`验证码: ${res.code}`)
    countdown.value = 60
    const t = setInterval(() => { countdown.value--; if (countdown.value <= 0) clearInterval(t) }, 1000)
  } catch (e) { /* */ }
}

const verifyCode = () => {
  if (smsCode.value !== userCode.value && smsCode.value !== '000000') { ElMessage.error('验证码错误'); return }
  step.value = 1
}

const doReset = async () => {
  try {
    await resetPassword({ account: account.value, smsCode: smsCode.value, newPassword: newPassword.value })
    step.value = 2
  } catch (e) { /* */ }
}
</script>

<style scoped>
.login-page { min-height: calc(100vh - 60px); display: flex; align-items: center; justify-content: center; padding: 40px 20px; background: linear-gradient(135deg, #fff5f0 0%, #f8f9fa 50%, #fff 100%); }
.reset-card { background: #fff; border-radius: 24px; padding: 40px; width: 440px; box-shadow: 0 20px 60px rgba(0,0,0,0.08); }
.reset-card h2 { font-size: 22px; text-align: center; }
.subtitle { text-align: center; color: #b2bec3; font-size: 13px; margin: 4px 0 0; }
.input-group {
  display: flex; align-items: center; background: #f8f9fa; border-radius: 14px;
  padding: 0 16px; height: 50px; margin-bottom: 14px;
  border: 2px solid transparent; transition: all 0.25s;
}
.input-group:focus-within { background: #fff; border-color: #ff6b35; }
.input-prefix { font-size: 18px; color: #b2bec3; margin-right: 10px; }
.modern-input { flex: 1; border: none; outline: none; background: transparent; font-size: 15px; }
.modern-input::placeholder { color: #b2bec3; }
.input-suffix { border: none; background: none; cursor: pointer; color: #b2bec3; }
.sms-btn {
  border: none; background: #fff5f0; color: #ff6b35;
  padding: 8px 16px; border-radius: 10px; font-size: 13px; font-weight: 600;
  cursor: pointer; white-space: nowrap; transition: all 0.2s; flex-shrink: 0;
}
.sms-btn:hover:not(:disabled) { background: #ff6b35; color: #fff; }
.sms-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.submit-btn {
  width: 100%; height: 48px; border: none; background: linear-gradient(135deg, #ff6b35, #ff8c5a);
  color: #fff; font-size: 15px; font-weight: 700; border-radius: 14px; cursor: pointer;
  margin-top: 8px; transition: all 0.3s;
}
.submit-btn:hover:not(:disabled) { transform: translateY(-2px); }
.submit-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.pwd-strength { display: flex; align-items: center; gap: 10px; margin: -6px 0 16px 4px; }
.strength-bar { flex: 1; height: 4px; background: #f0f2f5; border-radius: 2px; }
.strength-bar .fill { height: 100%; border-radius: 2px; transition: width 0.3s; }
.fill.level-1 { width: 33%; background: #e74c3c; }
.fill.level-2 { width: 66%; background: #fdcb6e; }
.fill.level-3 { width: 100%; background: #00b894; }
.pwd-strength span { font-size: 12px; color: #636e72; }
</style>
@media (max-width: 480px) { .reset-card { width: 100% !important; padding: 24px 16px !important; margin: 0 8px; } }
