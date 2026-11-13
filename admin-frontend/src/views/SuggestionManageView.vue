<template>
  <div>
    <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:20px">
      <h2 style="font-size:22px;color:#2d3436">💬 建议反馈</h2>
    </div>

    <el-card style="border-radius:14px">
      <div style="margin-bottom:16px;display:flex;gap:10px;flex-wrap:wrap">
        <el-select v-model="status" placeholder="全部状态" clearable style="width:150px" @change="reload">
          <el-option :value="1" label="待处理" />
          <el-option :value="2" label="已采纳" />
          <el-option :value="3" label="未采纳" />
        </el-select>
        <el-input v-model="keyword" placeholder="搜索建议内容" clearable style="max-width:260px" @keyup.enter="reload" />
        <el-button type="primary" plain @click="reload">搜索</el-button>
      </div>
      <el-table :data="list" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="提交人" width="120">
          <template #default="{row}">{{ row.nickname }}</template>
        </el-table-column>
        <el-table-column label="建议内容" min-width="260">
          <template #default="{row}">
            <span style="font-weight:600">{{ row.content }}</span>
            <div v-if="row.contact" style="font-size:12px;color:#b2bec3;margin-top:4px">联系方式：{{ row.contact }}</div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{row}">
            <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="管理回复" min-width="200">
          <template #default="{row}">
            <span v-if="row.reply" style="color:#2d3436">{{ row.reply }}</span>
            <span v-else style="color:#b2bec3">待回复</span>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="160">
          <template #default="{row}">{{ fmtTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{row}">
            <el-button size="small" :type="row.status === 1 ? 'primary' : ''" plain @click="openDialog(row)">
              {{ row.status === 1 ? '回复' : '查看' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:16px;text-align:center">
        <el-pagination layout="prev,pager,next,total" :total="total" :page-size="size" v-model:current-page="page" @change="load" />
      </div>
    </el-card>

    <!-- 回复建议 -->
    <el-dialog v-model="dialogVisible" title="回复建议" width="500px" :close-on-click-modal="false">
      <el-descriptions :column="1" border style="margin-bottom:16px">
        <el-descriptions-item label="提交人">{{ form.nickname }}</el-descriptions-item>
        <el-descriptions-item label="建议内容">{{ form.content }}</el-descriptions-item>
      </el-descriptions>
      <el-form label-position="top">
        <el-form-item label="处理结论">
          <el-radio-group v-model="form.adopt" :disabled="readOnly">
            <el-radio :value="true">采纳</el-radio>
            <el-radio :value="false">未采纳</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="回复意见">
          <el-input v-model="form.reply" type="textarea" :rows="4" maxlength="1000" show-word-limit
                    :disabled="readOnly" placeholder="回复用户是否采纳及说明（可空）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">关闭</el-button>
        <el-button v-if="!readOnly" type="primary" :loading="saving" @click="save">提交回复</el-button>
      </template>
    </el-dialog>
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
const status = ref(null)
const keyword = ref('')
const dialogVisible = ref(false)
const saving = ref(false)
const readOnly = ref(false)
const form = ref(emptyForm())

function emptyForm() {
  return { id: null, nickname: '', content: '', adopt: null, reply: '' }
}

const statusMap = { 1: '待处理', 2: '已采纳', 3: '未采纳' }
const statusText = (s) => statusMap[s] || '未知'
const statusType = (s) => ({ 1: 'warning', 2: 'success', 3: 'danger' }[s] || 'info')
const fmtTime = (s) => s ? String(s).replace('T', ' ').slice(0, 16) : ''

const load = async () => {
  try {
    const res = await adminApi.getSuggestions({ page: page.value, size: size.value, status: status.value ?? undefined, keyword: keyword.value || undefined })
    list.value = res.records || []
    total.value = Number(res.total || 0)
  } catch (e) { /* 拦截器已提示 */ }
}
const reload = () => { page.value = 1; load() }

const openDialog = (row) => {
  readOnly.value = row.status !== 1
  form.value = { id: row.id, nickname: row.nickname, content: row.content, adopt: row.status === 2, reply: row.reply || '' }
  dialogVisible.value = true
}

const save = async () => {
  if (form.value.adopt === null) { ElMessage.warning('请选择是否采纳'); return }
  saving.value = true
  try {
    await adminApi.replySuggestion(form.value.id, { adopt: form.value.adopt, reply: form.value.reply })
    ElMessage.success('已回复')
    dialogVisible.value = false
    load()
  } catch (e) { /* 拦截器已提示 */ } finally {
    saving.value = false
  }
}

onMounted(load)
</script>
