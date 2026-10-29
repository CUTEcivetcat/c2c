<template>
  <div>
    <h2 style="font-size:22px;margin-bottom:20px;color:#2d3436">⚙️ 系统设置</h2>

    <el-alert type="warning" :closable="false" style="margin-bottom:16px"
              title="本页配置保存后即时生效（Redis 缓存热更新），每次修改都会记录操作日志。仅总管理员可见本页面。" />

    <!-- 验证码回显 -->
    <el-card style="border-radius:14px;margin-bottom:20px" v-loading="loading">
      <template #header>
        <span style="font-weight:700">验证码回显</span>
        <el-tag type="danger" size="small" style="margin-left:10px">高危</el-tag>
      </template>
      <p style="font-size:13px;color:#909399;margin:0 0 16px">
        开启后，「发送验证码」接口会在响应中明文返回验证码（前端自动填充输入框），仅供本地/演示环境联调使用。
        <b>生产环境必须保持关闭</b>——开启时任何人都能从接口响应读到验证码，进而接管任意账号。
      </p>
      <div style="display:flex;align-items:center;gap:16px">
        <el-switch v-model="echoCode" active-text="开启" inactive-text="关闭" />
        <el-button type="primary" plain :loading="savingEcho" @click="saveEcho">保存</el-button>
        <span style="font-size:12px;color:#b2bec3">当前：{{ echoCode ? '开启（验证码将回显）' : '关闭（安全）' }}</span>
      </div>
    </el-card>

    <!-- 管理员登录失败锁定 -->
    <el-card style="border-radius:14px" v-loading="loading">
      <template #header><span style="font-weight:700">管理员登录失败锁定</span></template>
      <p style="font-size:13px;color:#909399;margin:0 0 16px">
        管理员账号连续登录失败达到上限后锁定一段时间，防止在线爆破管理后台。关闭开关则不做次数限制（不推荐）。
      </p>
      <el-form label-width="120px" style="max-width:560px">
        <el-form-item label="启用锁定">
          <el-switch v-model="lockEnabled" />
        </el-form-item>
        <el-form-item label="失败次数上限">
          <el-input-number v-model="maxAttempts" :min="1" :max="10" />
          <span style="margin-left:10px;font-size:12px;color:#b2bec3">连续失败达到该次数即触发锁定</span>
        </el-form-item>
        <el-form-item label="锁定时长(分钟)">
          <el-input-number v-model="lockMinutes" :min="1" :max="1440" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="savingLock" @click="saveLock">保存</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api/request'

const loading = ref(false)
const savingEcho = ref(false)
const savingLock = ref(false)

const echoCode = ref(false)
const lockEnabled = ref(true)
const maxAttempts = ref(5)
const lockMinutes = ref(15)

const KEY = {
  ECHO: 'sys.sms-echo-code',
  LOCK_ENABLED: 'security.admin-login-lock.enabled',
  LOCK_MAX: 'security.admin-login-lock.max-attempts',
  LOCK_MINUTES: 'security.admin-login-lock.lock-minutes'
}

const load = async () => {
  loading.value = true
  try {
    const list = await adminApi.getConfigs()
    const map = {}
    ;(list || []).forEach(c => { map[c.configKey] = c.configValue })
    echoCode.value = map[KEY.ECHO] === 'true'
    lockEnabled.value = map[KEY.LOCK_ENABLED] !== 'false'
    maxAttempts.value = parseInt(map[KEY.LOCK_MAX] || '5', 10)
    lockMinutes.value = parseInt(map[KEY.LOCK_MINUTES] || '15', 10)
  } finally {
    loading.value = false
  }
}

const saveEcho = async () => {
  savingEcho.value = true
  try {
    await adminApi.updateConfig({ key: KEY.ECHO, value: String(echoCode.value) })
    ElMessage.success('验证码回显设置已保存并即时生效')
  } catch {
    /* 拦截器已统一提示错误 */
  } finally {
    savingEcho.value = false
  }
}

const saveLock = async () => {
  savingLock.value = true
  try {
    await Promise.all([
      adminApi.updateConfig({ key: KEY.LOCK_ENABLED, value: String(lockEnabled.value) }),
      adminApi.updateConfig({ key: KEY.LOCK_MAX, value: String(maxAttempts.value) }),
      adminApi.updateConfig({ key: KEY.LOCK_MINUTES, value: String(lockMinutes.value) })
    ])
    ElMessage.success('登录锁定设置已保存并即时生效')
  } catch {
    /* 拦截器已统一提示错误 */
  } finally {
    savingLock.value = false
  }
}

onMounted(load)
</script>
