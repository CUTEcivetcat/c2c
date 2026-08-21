<template>
  <div class="page-container" style="max-width:700px">
    <page-back-bar title="最近浏览" />
    <div class="history-head" v-if="list.length">
      <span style="font-size:13px;color:#b2bec3">共 {{ list.length }} 件</span>
      <el-button size="small" text @click="clearAll">清空</el-button>
    </div>
    <div v-loading="loading" class="history-list">
      <div v-for="p in list" :key="p.id" class="history-card" @click="$router.push(`/product/${p.id}`)">
        <product-cover :src="p.images?.[0]?.url" class="hc-cover" />
        <div class="hc-main">
          <div class="hc-title">{{ p.title }}</div>
          <div class="hc-sub">
            <span class="hc-price">¥{{ p.price }}</span>
            <span class="hc-time">{{ fmtTime(p.createdAt) }}</span>
          </div>
        </div>
      </div>
      <el-empty v-if="!loading && !list.length" description="暂无浏览记录" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getProductsByIds } from '@/api/product'
import { getBrowseHistory, clearBrowseHistory } from '@/utils/browseHistory'

const list = ref([])
const loading = ref(false)

const fmtTime = (s) => s ? String(s).replace('T', ' ').slice(0, 16) : ''

const load = async () => {
  const ids = getBrowseHistory()
  if (!ids.length) { list.value = []; return }
  loading.value = true
  try {
    list.value = await getProductsByIds(ids.join(',')) || []
  } catch (e) { /* */ } finally { loading.value = false }
}

const clearAll = () => {
  clearBrowseHistory()
  list.value = []
}

onMounted(load)
</script>

<style scoped>
.history-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.history-list { display: flex; flex-direction: column; gap: 10px; }
.history-card {
  display: flex; gap: 12px; background: #fff; border-radius: 14px;
  padding: 12px; border: 1px solid #f0f2f5; cursor: pointer;
  transition: all 0.25s;
}
.history-card:hover { box-shadow: 0 8px 24px rgba(0,0,0,0.08); transform: translateY(-2px); }
.hc-cover { width: 90px; height: 90px; border-radius: 10px; flex-shrink: 0; }
.hc-main { flex: 1; min-width: 0; display: flex; flex-direction: column; justify-content: space-between; }
.hc-title { font-size: 14px; font-weight: 600; color: #2d3436; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.hc-sub { display: flex; justify-content: space-between; align-items: center; }
.hc-price { font-size: 16px; font-weight: 700; color: #ff6b35; }
.hc-time { font-size: 12px; color: #b2bec3; }
</style>