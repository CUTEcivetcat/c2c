<template>
  <div class="page-container" style="max-width:760px">
    <page-back-bar title="我的举报" />
    <div v-loading="loading" class="list">
      <div v-for="r in list" :key="r.id" class="card">
        <router-link :to="`/product/${r.productId}`" class="head">
          <el-image v-if="r.productCover" :src="r.productCover" class="cover" fit="cover" />
          <div class="title-box">
            <div class="title">{{ r.productTitle }}</div>
            <div class="tags">
              <el-tag size="small" type="warning">{{ r.reportTypeText }}</el-tag>
              <el-tag size="small" :type="statusType(r.status)">{{ statusText(r.status) }}</el-tag>
            </div>
          </div>
        </router-link>
        <div class="body">
          <div class="line">理由：{{ r.reason }}</div>
          <div v-if="r.handleRemark" class="line remark">处理备注：{{ r.handleRemark }}</div>
        </div>
        <div class="foot">
          <span class="time">{{ fmtTime(r.createdAt) }}</span>
          <div class="ops">
            <el-button v-if="r.status !== 1 && r.handledBy" size="small" type="primary" plain @click="contact(r.handledBy)">联系处理人</el-button>
          </div>
        </div>
      </div>
      <el-empty v-if="!loading && !list.length" description="还没有举报记录" />
    </div>
    <div v-if="total > 10" class="pager">
      <el-pagination layout="prev,pager,next" :total="total" :page-size="10" v-model:current-page="page" @change="load" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMyReports } from '@/api/review'
import { getOrCreateConversation } from '@/api/im'

const router = useRouter()
const list = ref([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)

const statusMap = { 1: '待处理', 2: '已违规下架', 3: '已驳回' }
const statusText = (s) => statusMap[s] || '未知'
const statusType = (s) => ({ 1: 'warning', 2: 'danger', 3: 'info' }[s] || '')
const fmtTime = (s) => (s ? String(s).replace('T', ' ').slice(0, 16) : '')

const load = async () => {
  loading.value = true
  try {
    const res = await getMyReports({ page: page.value, size: 10 })
    list.value = res.records || []
    total.value = res.total || 0
  } catch (e) { /* 拦截器已提示 */ } finally { loading.value = false }
}

const contact = async (userId) => {
  try {
    const conv = await getOrCreateConversation({ targetUserId: userId })
    router.push(`/chat/${conv.id}`)
  } catch (e) { /* 拦截器已提示 */ }
}

onMounted(load)
</script>

<style scoped>
.list { display: flex; flex-direction: column; gap: 12px; min-height: 120px; }
.card { background: #fff; border-radius: 16px; padding: 14px 16px; border: 1px solid #f0f2f5; }
.head { display: flex; gap: 12px; align-items: center; text-decoration: none; }
.cover { width: 64px; height: 64px; border-radius: 12px; flex-shrink: 0; }
.title-box { flex: 1; min-width: 0; }
.title { font-weight: 700; color: #2d3436; font-size: 15px; margin-bottom: 6px; }
.tags { display: flex; gap: 6px; }
.body { margin-top: 10px; background: #f8f9fa; border-radius: 10px; padding: 10px 12px; }
.line { font-size: 13px; color: #636e72; line-height: 1.7; }
.line.remark { color: #e74c3c; }
.foot { display: flex; justify-content: space-between; align-items: center; margin-top: 10px; }
.time { font-size: 12px; color: #b2bec3; }
.pager { text-align: center; margin-top: 16px; }
</style>
