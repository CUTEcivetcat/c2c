<template>
  <div class="page-container" style="max-width:1000px">
    <page-back-bar title="意见反馈" />

    <!-- 提交建议 -->
    <div class="submit-card">
      <h3>📝 提个建议</h3>
      <p class="tip">遇到的问题、想加的功能、页面体验建议都可以告诉我们；提交后管理端会查看并回复是否采纳。</p>
      <el-form label-position="top">
        <el-form-item label="建议内容" required>
          <el-input v-model="content" type="textarea" :rows="4" maxlength="1000" show-word-limit
                    placeholder="请描述你的建议或遇到的问题…" />
        </el-form-item>
        <el-form-item label="联系方式（可空）">
          <el-input v-model="contact" maxlength="50" placeholder="手机号 / 邮箱 / 微信，方便我们联系你" />
        </el-form-item>
      </el-form>
      <div class="submit-ops">
        <el-button type="primary" :loading="submitting" @click="submit">提交建议</el-button>
      </div>
    </div>

    <!-- 我的建议 -->
    <div class="list">
      <div v-for="s in list" :key="s.id" class="card">
        <div class="head">
          <span class="content">{{ s.content }}</span>
          <el-tag size="small" :type="statusType(s.status)">{{ statusText(s.status) }}</el-tag>
        </div>
        <div v-if="s.contact" class="line contact">联系方式：{{ s.contact }}</div>
        <div v-if="s.reply" class="line remark">管理回复：{{ s.reply }}</div>
        <div v-if="s.status === 1" class="line pending">等待管理端处理中…</div>
        <div class="foot">
          <span class="time">{{ fmtTime(s.createdAt) }}</span>
        </div>
      </div>
      <el-empty v-if="!loading && !list.length" description="还没有提交过建议" />
    </div>
    <div v-if="total > 10" class="pager">
      <el-pagination layout="prev,pager,next" :total="total" :page-size="10" v-model:current-page="page" @change="load" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { createSuggestion, getMySuggestions } from '@/api/suggestion'
import { ElMessage } from 'element-plus'

const content = ref('')
const contact = ref('')
const submitting = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)

const statusMap = { 1: '待处理', 2: '已采纳', 3: '未采纳' }
const statusText = (s) => statusMap[s] || '未知'
const statusType = (s) => ({ 1: 'warning', 2: 'success', 3: 'danger' }[s] || '')
const fmtTime = (s) => (s ? String(s).replace('T', ' ').slice(0, 16) : '')

const load = async () => {
  loading.value = true
  try {
    const res = await getMySuggestions({ page: page.value, size: 10 })
    list.value = res.records || []
    total.value = res.total || 0
  } catch (e) { /* 拦截器已提示 */ } finally { loading.value = false }
}

const submit = async () => {
  if (!content.value.trim()) return ElMessage.warning('请填写建议内容')
  submitting.value = true
  try {
    await createSuggestion({ content: content.value.trim(), contact: contact.value.trim() || undefined })
    ElMessage.success('建议已提交，感谢反馈')
    content.value = ''
    contact.value = ''
    load()
  } catch (e) { /* 拦截器已提示 */ } finally { submitting.value = false }
}

onMounted(load)
</script>

<style scoped>
.submit-card {
  background: #fff; border-radius: 16px; padding: 18px 20px;
  border: 1px solid var(--c2c-line-soft); margin-bottom: 18px;
}
.submit-card h3 { margin: 0 0 6px; font-size: 16px; font-weight: 700; color: var(--c2c-text); }
.tip { font-size: 13px; color: var(--c2c-text-muted); margin: 0 0 14px; line-height: 1.6; }
.submit-ops { display: flex; justify-content: flex-end; }
.list { display: flex; flex-direction: column; gap: 12px; min-height: 120px; }
.card { background: #fff; border-radius: 16px; padding: 14px 16px; border: 1px solid var(--c2c-line-soft); }
.head { display: flex; gap: 10px; align-items: flex-start; justify-content: space-between; }
.content { font-weight: 600; color: var(--c2c-text); font-size: 14px; line-height: 1.7; flex: 1; }
.line { margin-top: 8px; font-size: 13px; color: var(--c2c-text-secondary); line-height: 1.7; }
.line.remark { color: var(--c2c-primary-dark); }
.line.contact { color: var(--c2c-text-muted); }
.line.pending { color: var(--c2c-text-muted); }
.foot { display: flex; justify-content: flex-end; margin-top: 10px; }
.time { font-size: 12px; color: var(--c2c-text-muted); }
.pager { text-align: center; margin-top: 16px; }
</style>
