<template>
  <div>
    <h2 style="font-size:22px;margin-bottom:20px;color:#2d3436">🤖 AI 接入配置</h2>

    <el-alert type="info" :closable="false" style="margin-bottom:16px"
              title="配置保存后即时生效（sys_config 运行时热更新），客服对话与多模态商品发布两条链路共用同一模型。Base URL 或 API Key 留空时，自动降级本地规则引擎，不发起真实调用。仅总管理员可见本页面。" />

    <el-row :gutter="20">
      <!-- 表单填写 -->
      <el-col :span="14">
        <el-card style="border-radius:14px" v-loading="loading">
          <template #header><span style="font-weight:700">模型接入</span></template>
          <el-form label-width="120px" style="max-width:620px">
            <el-form-item label="Base URL">
              <el-input v-model="form.baseUrl" placeholder="OpenAI 兼容接口地址，如 https://ark.cn-beijing.volces.com/api/v3" />
            </el-form-item>
            <el-form-item label="API Key">
              <el-input v-model="form.apiKey" type="password" show-password
                        :placeholder="hasKey ? '已配置，留空则不修改' : '留空 = 规则引擎模式'" />
            </el-form-item>
            <el-form-item label="模型名">
              <el-input v-model="form.model" placeholder="需支持多模态 vision，如 doubao-1-5-vision-pro-250828" />
            </el-form-item>
            <el-form-item label="超时(毫秒)">
              <el-input-number v-model="form.timeout" :min="1000" :max="120000" :step="1000" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="saveForm">保存配置</el-button>
              <el-button plain @click="load">刷新</el-button>
              <el-button type="danger" plain :loading="stopping" @click="disableAi">停用 AI（清空接入）</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- JSON 导入 / 导出 -->
      <el-col :span="10">
        <el-card style="border-radius:14px" v-loading="loading">
          <template #header><span style="font-weight:700">JSON 导入 / 导出</span></template>
          <p style="font-size:13px;color:#909399;margin:0 0 12px">
            可将配置复制为 JSON 备份；也可粘贴 JSON 后点「导入」回填到上方表单。
          </p>
          <el-input v-model="jsonText" type="textarea" :rows="10"
                    placeholder='{"baseUrl":"","apiKey":"","model":"doubao-1-5-vision-pro-250828","timeout":15000}' />
          <div style="margin-top:12px;display:flex;gap:10px">
            <el-button type="primary" plain @click="exportJson">导出当前配置</el-button>
            <el-button @click="importJson">导入 JSON</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api/request'

const KEY = {
  BASE_URL: 'ai.base-url',
  API_KEY: 'ai.api-key',
  MODEL: 'ai.model',
  TIMEOUT: 'ai.timeout'
}

const loading = ref(false)
const saving = ref(false)
const stopping = ref(false)
const hasKey = ref(false)
const jsonText = ref('')

const form = reactive({
  baseUrl: '',
  apiKey: '',
  model: '',
  timeout: 15000
})

const load = async () => {
  loading.value = true
  try {
    const list = await adminApi.getConfigs()
    const map = {}
    ;(list || []).forEach(c => { map[c.configKey] = c.configValue })
    form.baseUrl = map[KEY.BASE_URL] || ''
    hasKey.value = !!map[KEY.API_KEY]
    form.apiKey = '' // 密码框不回显明文，留空表示不修改
    form.model = map[KEY.MODEL] || 'doubao-1-5-vision-pro-250828'
    form.timeout = parseInt(map[KEY.TIMEOUT] || '15000', 10)
    exportJson()
  } finally {
    loading.value = false
  }
}

const saveForm = async () => {
  saving.value = true
  try {
    const payload = [
      { key: KEY.BASE_URL, value: form.baseUrl.trim() },
      { key: KEY.MODEL, value: form.model.trim() },
      { key: KEY.TIMEOUT, value: String(form.timeout) }
    ]
    // API Key 留空视为不修改，避免掩码场景下把已有 Key 清空
    if (form.apiKey.trim()) {
      payload.push({ key: KEY.API_KEY, value: form.apiKey.trim() })
    }
    await Promise.all(payload.map(p => adminApi.updateConfig(p)))
    ElMessage.success('AI 接入配置已保存并即时生效')
    await load()
  } catch {
    /* 拦截器已统一提示 */
  } finally {
    saving.value = false
  }
}

// 停用 AI：清空 Base URL 与 API Key，回到本地规则引擎模式
const disableAi = async () => {
  try {
    await ElMessageBox.confirm('停用后将清空 Base URL 与 API Key，客服对话与商品估价自动回到本地规则引擎模式。确定停用？',
      '停用 AI 接入', { type: 'warning', confirmButtonText: '确定停用', cancelButtonText: '取消' })
  } catch { return }
  stopping.value = true
  try {
    await adminApi.updateConfig({ key: KEY.BASE_URL, value: '' })
    await adminApi.updateConfig({ key: KEY.API_KEY, value: '' })
    ElMessage.success('已停用 AI 接入，回到规则引擎模式')
    await load()
  } catch {
    /* 拦截器已统一提示 */
  } finally {
    stopping.value = false
  }
}

const exportJson = () => {
  jsonText.value = JSON.stringify({
    baseUrl: form.baseUrl,
    apiKey: form.apiKey || (hasKey.value ? '******' : ''),
    model: form.model,
    timeout: form.timeout
  }, null, 2)
}

const importJson = () => {
  try {
    const obj = JSON.parse(jsonText.value)
    if (typeof obj !== 'object' || obj === null) {
      throw new Error('格式不是对象')
    }
    if (typeof obj.baseUrl === 'string') form.baseUrl = obj.baseUrl
    if (typeof obj.apiKey === 'string' && obj.apiKey && obj.apiKey !== '******') form.apiKey = obj.apiKey
    if (typeof obj.model === 'string' && obj.model) form.model = obj.model
    if (obj.timeout != null) form.timeout = parseInt(obj.timeout, 10) || 15000
    ElMessage.success('JSON 已回填到表单，点击「保存配置」生效')
  } catch (e) {
    ElMessage.error('JSON 解析失败：' + e.message)
  }
}

onMounted(load)
</script>
