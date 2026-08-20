<template>
  <div class="login-page">
    <div class="reset-card fade-in-up" style="width:480px">
      <h2>📝 注册账号</h2>
      <p class="subtitle">填写信息加入闲小鱼</p>

      <el-form :model="form" label-width="0" size="large" style="margin-top:20px">
        <!-- 注册方式：邮箱 或 手机号 -->
        <el-tabs v-model="regType" style="margin-bottom:8px">
          <el-tab-pane label="📧 邮箱注册" name="email" />
          <el-tab-pane label="📱 手机号注册" name="phone" />
        </el-tabs>

        <div class="input-group" v-if="regType==='phone'">
          <el-icon class="input-prefix"><Iphone /></el-icon>
          <input v-model="form.phone" type="text" placeholder="手机号" class="modern-input" maxlength="11" />
        </div>
        <div class="input-group" v-if="regType==='email'">
          <el-icon class="input-prefix"><Message /></el-icon>
          <input v-model="form.email" type="email" placeholder="邮箱地址" class="modern-input" />
        </div>

        <div class="input-group">
          <el-icon class="input-prefix"><Lock /></el-icon>
          <input v-model="form.password" :type="showPwd?'text':'password'" placeholder="密码（8-32位，含字母数字）" class="modern-input" />
          <button type="button" class="input-suffix" @click="showPwd=!showPwd"><el-icon><View v-if="!showPwd"/><Hide v-else/></el-icon></button>
        </div>
        <div class="pwd-hints"><el-icon><WarnTriangleFilled /></el-icon> 8-32位，需含字母和数字</div>

        <div class="input-group">
          <el-icon class="input-prefix"><Check /></el-icon>
          <input v-model="form.smsCode" type="text" placeholder="验证码" class="modern-input" style="flex:1" />
          <button type="button" class="sms-btn" :disabled="countdown>0" @click="sendCode">{{ countdown>0?countdown+'s':'获取验证码' }}</button>
        </div>

        <div class="input-group">
          <el-icon class="input-prefix"><User /></el-icon>
          <input v-model="form.nickname" type="text" placeholder="昵称（留空自动生成 user_xxxxx）" class="modern-input" />
        </div>

        <div class="input-group">
          <el-icon class="input-prefix"><Edit /></el-icon>
          <input v-model="form.bio" type="text" placeholder="个人简介（选填，如：数码爱好者，诚心交易）" class="modern-input" />
        </div>

        <button type="button" class="submit-btn" @click="doRegister" :disabled="loading">
          <span v-if="!loading">注 册</span>
          <el-icon v-else class="loading-icon"><Loading /></el-icon>
        </button>
      </el-form>

      <p style="text-align:center;margin-top:16px">
        已有账号？<router-link to="/login">去登录</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/userStore'
import { sendSms, register, login } from '@/api/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const store = useUserStore()
const regType = ref('email')
const loading = ref(false)
const countdown = ref(0)
const showPwd = ref(false)
const form = ref({ phone:'', email:'', password:'', smsCode:'', nickname:'', bio:'' })

const sendCode = async () => {
  const account = regType.value === 'email' ? form.value.email : form.value.phone
  if (!account) { ElMessage.warning(regType.value==='email'?'请输入邮箱':'请输入手机号'); return }
  try {
    const res = await sendSms(account)
    if (res.type === 'email') ElMessage.success('验证码已发送到邮箱')
    else ElMessage.success(`验证码: ${res.code}`)
    countdown.value = 60
    const t = setInterval(() => { countdown.value--; if (countdown.value <= 0) clearInterval(t) }, 1000)
  } catch (e) { /* */ }
}

const doRegister = async () => {
  const account = regType.value === 'email' ? form.value.email : form.value.phone
  if (!account) { ElMessage.warning('请输入注册信息'); return }
  if (!form.value.password) { ElMessage.warning('请设置密码'); return }
  if (!form.value.smsCode) { ElMessage.warning('请输入验证码'); return }

  loading.value = true
  try {
    await register({
      phone: regType.value === 'phone' ? form.value.phone : null,
      email: regType.value === 'email' ? form.value.email : null,
      password: form.value.password,
      smsCode: form.value.smsCode,
      nickname: form.value.nickname || null,
      bio: form.value.bio || null
    })
    // 注册成功自动登录
    await store.login({ account, password: form.value.password, loginType: 1 })
    ElMessage.success('注册成功')
    router.push('/')
  } catch (e) { /* */ }
  loading.value = false
}
</script>

<style scoped>
.login-page { min-height: calc(100vh - 60px); display: flex; align-items: center; justify-content: center; padding: 40px 20px; background: linear-gradient(135deg, #fff5f0 0%, #f8f9fa 50%, #fff 100%); }
.reset-card { background: #fff; border-radius: 24px; padding: 32px 36px; box-shadow: 0 20px 60px rgba(0,0,0,0.08); }
.reset-card h2 { font-size: 22px; text-align: center; }
.subtitle { text-align: center; color: #b2bec3; font-size: 13px; margin: 4px 0 0; }
.input-group {
  display: flex; align-items: center; background: #f8f9fa; border-radius: 14px;
  padding: 0 16px; height: 48px; margin-bottom: 12px;
  border: 2px solid transparent; transition: all 0.25s;
}
.input-group:focus-within { background: #fff; border-color: #ff6b35; }
.input-prefix { font-size: 18px; color: #b2bec3; margin-right: 10px; }
.modern-input { flex: 1; border: none; outline: none; background: transparent; font-size: 14px; }
.modern-input::placeholder { color: #b2bec3; font-size: 13px; }
.input-suffix { border: none; background: none; cursor: pointer; color: #b2bec3; }
.sms-btn { border: none; background: #fff5f0; color: #ff6b35; padding: 6px 14px; border-radius: 10px; font-size: 12px; font-weight: 600; cursor: pointer; white-space: nowrap; flex-shrink: 0; }
.sms-btn:hover:not(:disabled) { background: #ff6b35; color: #fff; }
.sms-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.pwd-hints { font-size: 12px; color: #b2bec3; margin: -6px 0 12px 4px; display: flex; align-items: center; gap: 4px; }
.submit-btn {
  width: 100%; height: 48px; border: none; background: linear-gradient(135deg, #ff6b35, #ff8c5a);
  color: #fff; font-size: 15px; font-weight: 700; border-radius: 14px; cursor: pointer;
  margin-top: 8px; transition: all 0.3s;
}
.submit-btn:hover:not(:disabled) { transform: translateY(-2px); }
.submit-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.loading-icon { animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
</style>
@media (max-width: 480px) { .reset-card { width: 100% !important; padding: 20px 14px !important; border-radius: 14px; } .input-group { height: 42px; } .submit-btn { height: 44px; } }
