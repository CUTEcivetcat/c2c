<template>
  <div class="page-container">
    <page-back-bar title="我的收藏" />
    <div class="product-grid" style="margin-top:8px">
      <el-card v-for="it in items" :key="it.id" class="product-card" shadow="hover" @click="$router.push(`/product/${it.productId}`)">
        <img :src="it.product?.images?.[0]?.url || '/default.png'" class="product-img" alt="" />
        <div class="product-info">
          <h3>{{ it.product?.title || ('商品 #' + it.productId) }}</h3>
          <div class="price">{{ it.product?.price ?? '--' }}</div>
          <div class="meta">
            <span>{{ it.product?.conditionText || '' }}</span>
            <el-button size="small" type="danger" plain @click.stop="remove(it.productId)">取消收藏</el-button>
          </div>
        </div>
      </el-card>
      <el-empty v-if="!loading && !items.length" description="还没有收藏，去逛逛吧" style="grid-column:1/-1" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getFavorites, removeFavorite } from '@/api/favorite'
import { getProductDetail } from '@/api/product'

const items = ref([])
const loading = ref(true)

onMounted(async () => {
  try {
    const res = await getFavorites({ page: 1, size: 50 })
    const recs = res.records || []
    const details = await Promise.all(recs.map(f => getProductDetail(f.productId).catch(() => null)))
    items.value = recs.map((f, i) => ({ ...f, product: details[i] }))
  } finally {
    loading.value = false
  }
})

const remove = async (pid) => {
  await removeFavorite(pid)
  items.value = items.value.filter(it => it.productId !== pid)
}
</script>
