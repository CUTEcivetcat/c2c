<template>
  <div class="page-container search-page">
    <page-back-bar title="搜索" />

    <!-- 搜索框 -->
    <div class="search-box">
      <el-input v-model="filters.keyword" placeholder="搜索商品…" clearable size="large" @keyup.enter="doSearch" @input="onInput" @clear="clearSuggest">
        <template #prefix><el-icon><Search /></el-icon></template>
        <template #append><el-button @click="doSearch">搜索</el-button></template>
      </el-input>
      <!-- 联想下拉 -->
      <div v-if="suggestions.length" class="suggest-list">
        <div v-for="s in suggestions" :key="s" class="suggest-item" @click="pickSuggestion(s)">
          <el-icon><Search /></el-icon><span>{{ s }}</span>
        </div>
      </div>
    </div>

    <!-- 搜索历史 + 热门词（无关键词时显示） -->
    <div v-if="!filters.keyword" class="tag-area">
      <div class="tag-row" v-if="history.length">
        <span class="tag-label">🕐 搜索历史</span>
        <el-tag v-for="h in history" :key="h" class="tag" @click="quickSearch(h)">{{ h }}</el-tag>
        <el-button size="small" text type="danger" @click="clearHistory">清空</el-button>
      </div>
      <div class="tag-row">
        <span class="tag-label">🔥 热门搜索</span>
        <el-tag v-for="h in hotWords" :key="h" class="tag hot" @click="quickSearch(h)">{{ h }}</el-tag>
      </div>
    </div>

    <el-row :gutter="16" style="margin:16px 0">
      <el-col :span="4"><el-select v-model="filters.categoryId" placeholder="分类" clearable @change="search"><el-option v-for="c in cats" :key="c.id" :label="c.name" :value="c.id" /></el-select></el-col>
      <el-col :span="4"><el-select v-model="filters.condition" placeholder="成色" clearable @change="search"><el-option :value="1" label="全新"/><el-option :value="2" label="几乎全新"/><el-option :value="3" label="轻微使用"/></el-select></el-col>
      <el-col :span="4"><el-select v-model="filters.sort" @change="search"><el-option value="created_at" label="最新"/><el-option value="hot" label="人气"/><el-option value="price_asc" label="价格↑"/><el-option value="price_desc" label="价格↓"/></el-select></el-col>
    </el-row>

    <div v-loading="loading" class="product-grid">
      <skeleton-card v-if="loading" :count="8" />
      <el-card v-for="p in products" :key="p.id" class="product-card" shadow="hover" @click="$router.push(`/product/${p.id}`)">
        <product-cover :src="p.images?.[0]?.url" class="product-img" />
        <div class="product-info">
          <h3>{{ p.title }}</h3>
          <div class="price">¥{{ p.price }}</div>
          <div class="meta">
            <span>{{ p.conditionText || '' }}</span>
            <span>{{ p.location || '' }}</span>
          </div>
        </div>
      </el-card>
    </div>
    <el-empty v-if="!loading && !products.length" description="没有找到相关商品" />
    <el-pagination v-if="total>0" layout="prev,pager,next" :total="total" :page-size="20" v-model:current-page="page" @change="search" />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { getCategories, searchProducts } from '@/api/product'

const route = useRoute()
const cats = ref([])
const products = ref([])
const page = ref(1)
const total = ref(0)
const loading = ref(false)
const filters = ref({ keyword: route.query.keyword || '', categoryId: null, condition: null, sort: 'created_at' })

// 搜索历史（localStorage）
const HISTORY_KEY = 'c2c_search_history'
const history = ref([])
const hotWords = ['教材', '手机', '电脑', '自行车', '耳机', '图书']

const suggestions = ref([])
let suggestTimer = null

const loadHistory = () => {
  try { history.value = JSON.parse(localStorage.getItem(HISTORY_KEY) || '[]') } catch (e) { history.value = [] }
}
const saveHistory = (kw) => {
  if (!kw) return
  let list = history.value.filter(k => k !== kw)
  list.unshift(kw)
  if (list.length > 10) list = list.slice(0, 10)
  history.value = list
  localStorage.setItem(HISTORY_KEY, JSON.stringify(list))
}
const clearHistory = () => { history.value = []; localStorage.removeItem(HISTORY_KEY) }

const search = async () => {
  loading.value = true
  try {
    const res = await searchProducts({ ...filters.value, page: page.value, size: 20 })
    products.value = res.records; total.value = res.total
  } catch (e) { /* */ } finally { loading.value = false }
}

const doSearch = () => {
  saveHistory(filters.value.keyword.trim())
  suggestions.value = []
  page.value = 1
  search()
}
const quickSearch = (kw) => { filters.value.keyword = kw; doSearch() }

// 联想：防抖 300ms 调搜索接口取前 5 个标题
const onInput = () => {
  clearTimeout(suggestTimer)
  const kw = filters.value.keyword.trim()
  if (!kw) { suggestions.value = []; return }
  suggestTimer = setTimeout(async () => {
    try {
      const res = await searchProducts({ keyword: kw, page: 1, size: 5 })
      suggestions.value = (res.records || []).map(p => p.title).slice(0, 5)
    } catch (e) { suggestions.value = [] }
  }, 300)
}
const pickSuggestion = (s) => { filters.value.keyword = s; doSearch() }
const clearSuggest = () => { suggestions.value = [] }

onMounted(async () => {
  loadHistory()
  cats.value = await getCategories() || []
  if (filters.value.keyword) saveHistory(filters.value.keyword)
  search()
})
onUnmounted(() => clearTimeout(suggestTimer))
</script>

<style scoped>
.search-page { max-width: 900px; }
.search-box { position: relative; margin-top: 8px; }
.suggest-list {
  position: absolute; top: 100%; left: 0; right: 0; z-index: 100;
  background: #fff; border-radius: 0 0 12px 12px; box-shadow: 0 8px 24px rgba(0,0,0,0.1);
  overflow: hidden;
}
.suggest-item { display: flex; align-items: center; gap: 8px; padding: 10px 16px; cursor: pointer; font-size: 13px; color: #2d3436; }
.suggest-item:hover { background: #f8f9fa; color: #ff6b35; }
.tag-area { margin-top: 12px; display: flex; flex-direction: column; gap: 10px; }
.tag-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.tag-label { font-size: 13px; color: #b2bec3; }
.tag { cursor: pointer; }
.tag.hot { border-color: #ffd9c4; color: #ff6b35; background: #fff8f5; }
.product-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 20px; }
@media (max-width: 900px) { .product-grid { grid-template-columns: repeat(2, 1fr); } }
.product-card { border-radius: 14px; cursor: pointer; }
.product-img { width: 100%; height: 180px; }
.product-info h3 { font-size: 14px; margin: 8px 0 4px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.price { font-size: 18px; font-weight: 700; color: #ff6b35; }
.meta { display: flex; justify-content: space-between; font-size: 12px; color: #b2bec3; }
</style>