<template>
  <div class="page-container profile-edit fade-in-up" style="max-width:800px">
    <page-back-bar title="资料设置" />

    <!-- 基本资料 -->
    <div class="pe-card">
      <h3 class="pe-title">基本资料</h3>
      <el-form :model="profile" label-position="top" size="large">
        <el-form-item label="昵称">
          <el-input v-model="profile.nickname" placeholder="设置你的昵称" maxlength="20" />
          <div v-if="store.userInfo?.nicknameStatus === 1" class="pe-tip pending">
            昵称「{{ store.userInfo.nicknamePending }}」审核中，通过后生效
          </div>
          <div v-else class="pe-tip">修改昵称需审核：自动拦截敏感词，通过后由审核员人工审核</div>
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="profile.gender">
            <el-radio-button :value="1">男</el-radio-button>
            <el-radio-button :value="2">女</el-radio-button>
            <el-radio-button :value="0">保密</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="个人简介">
          <el-input v-model="profile.bio" type="textarea" :rows="3" maxlength="100"
                    placeholder="介绍一下自己，如：数码爱好者，诚心交易" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="saving" @click="saveProfile"
                     style="width:100%;height:48px;border-radius:14px;font-size:15px">保存资料</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 账号绑定：手机号 -->
    <div class="pe-card">
      <h3 class="pe-title">绑定手机号</h3>
      <div class="bind-current">
        <span class="bind-label">当前手机号：</span>
        <span class="bind-value">{{ store.userInfo?.phone || '未绑定' }}</span>
      </div>
      <el-form label-position="top" class="bind-form">
        <el-form-item label="新手机号">
          <el-input v-model="phoneForm.phone" placeholder="请输入新手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="短信验证码">
          <div class="code-row">
            <el-input v-model="phoneForm.code" placeholder="6 位验证码" maxlength="6" style="flex:1" />
            <el-button :disabled="phoneCountdown > 0" @click="sendPhoneCode">
              {{ phoneCountdown > 0 ? phoneCountdown + 's' : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-button type="primary" plain :loading="phoneBusy" @click="submitPhone" style="border-radius:12px">
          {{ store.userInfo?.phone ? '更换手机号' : '绑定手机号' }}
        </el-button>
      </el-form>
    </div>

    <!-- 账号绑定：邮箱 -->
    <div class="pe-card">
      <h3 class="pe-title">绑定邮箱</h3>
      <div class="bind-current">
        <span class="bind-label">当前邮箱：</span>
        <span class="bind-value">{{ store.userInfo?.email || '未绑定' }}</span>
      </div>
      <el-form label-position="top" class="bind-form">
        <el-form-item label="新邮箱">
          <el-input v-model="emailForm.email" placeholder="请输入邮箱地址" />
        </el-form-item>
        <el-form-item label="邮箱验证码">
          <div class="code-row">
            <el-input v-model="emailForm.code" placeholder="6 位验证码" maxlength="6" style="flex:1" />
            <el-button :disabled="emailCountdown > 0" @click="sendEmailCode">
              {{ emailCountdown > 0 ? emailCountdown + 's' : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-button type="primary" plain :loading="emailBusy" @click="submitEmail" style="border-radius:12px">
          {{ store.userInfo?.email ? '更换邮箱' : '绑定邮箱' }}
        </el-button>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/stores/userStore'
import { updateProfile, bindPhone, bindEmail, sendSms } from '@/api/user'
import { ElMessage } from 'element-plus'

const store = useUserStore()
const saving = ref(false)

const profile = ref({ nickname: '', gender: 0, bio: '' })

// 手机号绑定/换绑
const phoneForm = ref({ phone: '', code: '' })
const phoneCountdown = ref(0)
const phoneBusy = ref(false)
let phoneTimer = null

// 邮箱绑定/换绑
const emailForm = ref({ email: '', code: '' })
const emailCountdown = ref(0)
const emailBusy = ref(false)
let emailTimer = null

onMounted(async () => {
  await store.fetchProfile()
  const u = store.userInfo || {}
  profile.value = { nickname: u.nickname || '', gender: u.gender ?? 0, bio: u.bio || '' }
})

const startCountdown = (countRef, timerRef) => {
  countRef.value = 60
  timerRef.value = setInterval(() => {
    countRef.value--
    if (countRef.value <= 0) clearInterval(timerRef.value)
  }, 1000)
}

const sendPhoneCode = async () => {
  if (!/^1[3-9]\d{9}$/.test(phoneForm.value.phone)) return ElMessage.warning('请输入正确的手机号')
  try {
    const res = await sendSms(phoneForm.value.phone)
    ElMessage.success(res.type === 'email' ? '验证码已发送到邮箱' : `验证码: ${res.code}`)
    startCountdown(phoneCountdown, phoneTimer)
  } catch (e) { /* 拦截器已提示 */ }
}

const sendEmailCode = async () => {
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailForm.value.email)) return ElMessage.warning('请输入正确的邮箱')
  try {
    await sendSms(emailForm.value.email)
    ElMessage.success('验证码已发送到邮箱')
    startCountdown(emailCountdown, emailTimer)
  } catch (e) { /* 拦截器已提示 */ }
}

const saveProfile = async () => {
  saving.value = true
  const oldNick = store.userInfo?.nickname || ''
  const nickChanged = profile.value.nickname.trim() !== oldNick
  try {
    await updateProfile({ nickname: profile.value.nickname, gender: profile.value.gender, bio: profile.value.bio })
    await store.fetchProfile()
    ElMessage.success(nickChanged ? '昵称已提交审核，通过后生效；其他资料已保存' : '保存成功')
  } catch (e) { /* 拦截器已提示 */ } finally { saving.value = false }
}

const submitPhone = async () => {
  if (!/^1[3-9]\d{9}$/.test(phoneForm.value.phone)) return ElMessage.warning('请输入正确的手机号')
  if (!phoneForm.value.code) return ElMessage.warning('请输入验证码')
  phoneBusy.value = true
  try {
    await bindPhone({ phone: phoneForm.value.phone, smsCode: phoneForm.value.code })
    await store.fetchProfile()
    ElMessage.success('手机号已绑定')
    phoneForm.value = { phone: '', code: '' }
  } catch (e) { /* 拦截器已提示 */ } finally { phoneBusy.value = false }
}

const submitEmail = async () => {
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailForm.value.email)) return ElMessage.warning('请输入正确的邮箱')
  if (!emailForm.value.code) return ElMessage.warning('请输入验证码')
  emailBusy.value = true
  try {
    await bindEmail({ email: emailForm.value.email, code: emailForm.value.code })
    await store.fetchProfile()
    ElMessage.success('邮箱已绑定')
    emailForm.value = { email: '', code: '' }
  } catch (e) { /* 拦截器已提示 */ } finally { emailBusy.value = false }
}
</script>

<style scoped>
.profile-edit { display: flex; flex-direction: column; gap: 20px; }
.pe-card {
  background: #fff; border-radius: 20px; padding: 28px;
  border: 1px solid var(--c2c-line-soft);
}
.pe-title { margin: 0 0 18px; font-size: 16px; font-weight: 700; color: var(--c2c-text); }
.pe-tip { margin-top: 6px; font-size: 12px; color: var(--c2c-text-muted); }
.pe-tip.pending {
  display: flex; align-items: center; gap: 4px; color: var(--c2c-primary-dark);
  background: #fff5f0; border-radius: 8px; padding: 6px 10px;
}
.bind-current { margin-bottom: 16px; font-size: 14px; }
.bind-label { color: var(--c2c-text-muted); }
.bind-value { font-weight: 600; color: var(--c2c-text); }
.bind-form { max-width: 420px; }
.code-row { display: flex; gap: 10px; }
:deep(.el-radio-button__inner) { border-radius: 10px !important; }
</style>
