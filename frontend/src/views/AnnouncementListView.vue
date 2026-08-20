<template>
  <div class="page-container">
    <page-back-bar title="平台公告" />
    <div class="announcement-list">
      <el-card v-for="a in list" :key="a.id" class="ann-card" shadow="hover">
        <div class="ann-head">
          <el-tag v-if="a.pinned === 1" type="danger" size="small">置顶</el-tag>
          <el-tag size="small" :type="typeTag(a.type)">{{ typeText(a.type) }}</el-tag>
          <strong class="ann-title">{{ a.title }}</strong>
          <span class="ann-time">{{ formatTime(a.createdAt) }}</span>
        </div>
        <div class="ann-content">{{ a.content }}</div>
      </el-card>
      <el-empty v-if="!list.length" description="暂无公告" />
      <div class="pager" v-if="total > size">
        <el-pagination layout="prev,pager,next" :total="total" :page-size="size" v-model:current-page="page" @change="load" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAnnouncementList } from '@/api/announcement'

const list = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)

const typeText = (t) => ({ 1: '公告', 2: '平台公约', 3: '通知' }[t] || '公告')
const typeTag = (t) => ({ 1: '', 2: 'warning', 3: 'info' }[t] || '')

const formatTime = (t) => {
  if (!t) return ''
  const d = new Date(t)
  const now = new Date()
  if (d.toDateString() === now.toDateString()) {
    return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

const load = async () => {
  try {
    const res = await getAnnouncementList({ page: page.value, size: size.value })
    list.value = res.records || []
    total.value = Number(res.total || 0)
  } catch (e) { /* */ }
}
onMounted(load)
</script>

<style scoped>
.page-container { max-width: 800px; margin: 0 auto; padding: 16px; }
.announcement-list { display: flex; flex-direction: column; gap: 12px; }
.ann-card { border-radius: 14px; }
.ann-head { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.ann-title { font-size: 15px; color: #2d3436; }
.ann-time { margin-left: auto; font-size: 12px; color: #b2bec3; }
.ann-content {
  margin-top: 10px; font-size: 13px; color: #636e72; line-height: 1.8;
  white-space: pre-wrap; word-break: break-word;
  background: #fafafa; border-radius: 10px; padding: 12px 14px;
}
.pager { display: flex; justify-content: center; margin-top: 16px; }
</style>
