<template>
  <div class="page-container">
    <page-back-bar title="搜索" />
    <el-row :gutter="16" style="margin:16px 0">
      <el-col :span="4"><el-select v-model="filters.categoryId" placeholder="分类" clearable @change="search"><el-option v-for="c in cats" :key="c.id" :label="c.name" :value="c.id" /></el-select></el-col>
      <el-col :span="4"><el-select v-model="filters.condition" placeholder="成色" clearable @change="search"><el-option :value="1" label="全新"/><el-option :value="2" label="几乎全新"/><el-option :value="3" label="轻微使用"/></el-select></el-col>
      <el-col :span="4"><el-select v-model="filters.sort" @change="search"><el-option value="created_at" label="最新"/><el-option value="price_asc" label="价格↑"/><el-option value="price_desc" label="价格↓"/></el-select></el-col>
    </el-row>
    <div class="product-grid">
      <el-card v-for="p in products" :key="p.id" class="product-card" shadow="hover" @click="$router.push(`/product/${p.id}`)">
        <product-cover :src="p.images?.[0]?.url" class="product-img" />
        <div class="product-info">
          <h3>{{ p.title }}</h3>
          <div class="price">{{ p.price }}</div>
          <div class="meta">
            <span>{{ p.conditionText || '' }}</span>
            <span>{{ p.location || '' }}</span>
          </div>
        </div>
      </el-card>
    </div>
    <el-pagination v-if="total>0" layout="prev,pager,next" :total="total" :page-size="20" v-model:current-page="page" @change="search" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getCategories, searchProducts } from '@/api/product'

const route = useRoute()
const cats = ref([])
const products = ref([])
const page = ref(1)
const total = ref(0)
const filters = ref({ keyword: route.query.keyword||'', categoryId:null, condition:null, sort:'created_at' })

const search = async () => { const res = await searchProducts({...filters.value, page:page.value, size:20}); products.value=res.records; total.value=res.total }
onMounted(async () => { cats.value=await getCategories(); search() })
</script>

<style scoped></style>
