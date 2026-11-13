<template>
  <div>
    <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:20px">
      <h2 style="font-size:22px;color:#2d3436">📊 AI 对话看板</h2>
      <el-button type="primary" plain :loading="exporting" @click="exportCsv">⬇ 导出日志</el-button>
    </div>

    <!-- 来源分布 -->
    <div style="display:flex;gap:16px;margin-bottom:20px">
      <el-card style="flex:1;border-radius:14px">
        <div class="stat-label">问答总数</div>
        <div class="stat-num">{{ stats.total ?? 0 }}</div>
      </el-card>
      <el-card style="flex:1;border-radius:14px">
        <div class="stat-label">大模型回复（model）</div>
        <div class="stat-num" style="color:#2980b9">{{ stats.model ?? 0 }}</div>
        <div class="stat-sub">{{ percent(stats.model) }}</div>
      </el-card>
      <el-card style="flex:1;border-radius:14px">
        <div class="stat-label">规则兜底（rule）</div>
        <div class="stat-num" style="color:#e17055">{{ stats.rule ?? 0 }}</div>
        <div class="stat-sub">{{ percent(stats.rule) }}</div>
      </el-card>
    </div>

    <el-card style="border-radius:14px">
      <div style="margin-bottom:16px;display:flex;gap:10px;flex-wrap:wrap">
        <el-select v-model="source" placeholder="来源" clearable style="width:150px" @change="load">
          <el-option label="大模型" value="model" />
          <el-option label="规则兜底" value="rule" />
        </el-select>
        <el-input v-model="keyword" placeholder="搜索回复内容" clearable style="max-width:260px" @keyup.enter="load" />
        <el-button type="primary" plain @click="load">搜索</el-button>
      </div>
      <el-table :data="list" stripe>
        <el-table-column label="用户" width="150"><template #default="{row}">
          <div>{{ row.userNickname }}</div>
          <div style="color:#b2bec3;font-size:12px">ID {{ row.userId }}</div>
        </template></el-table-column>
        <el-table-column label="提问" min-width="200" show-overflow-tooltip>
          <template #default="{row}">{{ row.question || '—' }}</template>
        </el-table-column>
        <el-table-column label="助手回复" min-width="260" show-overflow-tooltip>
          <template #default="{row}">{{ row.answer }}</template>
        </el-table-column>
        <el-table-column label="来源" width="100"><template #default="{row}">
          <el-tag :type="row.source === 'model' ? 'primary' : 'info'" size="small">
            {{ row.source === 'model' ? '大模型' : '规则' }}
          </el-tag>
        </template></el-table-column>
        <el-table-column prop="createdAt" label="时间" width="170" />
      </el-table>
      <div style="margin-top:16px;text-align:center">
        <el-pagination layout="prev,pager,next,total" :total="total" :page-size="size" v-model:current-page="page" @change="load" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { adminApi } from '@/api/request'
import { ElMessage } from 'element-plus'

const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const source = ref('')
const exporting = ref(false)
const stats = ref({ total: 0, model: 0, rule: 0 })

const percent = (n) => {
  const t = stats.value.total || 0
  if (!t) return '占比 0%'
  return '占比 ' + Math.round(((n || 0) / t) * 100) + '%'
}

const load = async () => {
  try {
    const res = await adminApi.getAiChatLogs({
      page: page.value, size: size.value,
      keyword: keyword.value || undefined,
      source: source.value || undefined
    })
    list.value = res.records || []
    total.value = Number(res.total || 0)
    const st = await adminApi.getAiChatStats()
    stats.value = st
  } catch (e) { /* 拦截器已提示 */ }
}

const exportCsv = async () => {
  exporting.value = true
  try {
    const rows = await adminApi.exportAiChatLogs({
      keyword: keyword.value || undefined,
      source: source.value || undefined
    })
    if (!rows || !rows.length) { ElMessage.warning('暂无可导出的对话日志'); return }
    const header = ['消息ID', '用户ID', '用户昵称', '提问', '回复', '来源', '时间']
    const lines = [header.join(',')]
    for (const r of rows) {
      lines.push([
        r.messageId, r.userId, csv(r.userNickname), csv(r.question),
        csv(r.answer), r.source, r.createdAt
      ].join(','))
    }
    const blob = new Blob(['\uFEFF' + lines.join('\r\n')], { type: 'text/csv;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'ai-chat-logs-' + new Date().toISOString().slice(0, 10) + '.csv'
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('已导出 ' + rows.length + ' 条日志')
  } catch (e) { /* */ } finally {
    exporting.value = false
  }
}

const csv = (v) => {
  if (v == null) return ''
  const s = String(v)
  return /[",\n\r]/.test(s) ? '"' + s.replace(/"/g, '""') + '"' : s
}

onMounted(load)
</script>
