<template>
  <div class="page-container user-profile fade-in-up" style="max-width:900px">
    <page-back-bar title="用户主页" />

    <!-- 用户信息卡片 -->
    <div class="user-card" v-if="user.id">
      <el-avatar :size="72" style="background:linear-gradient(135deg,#ff6b35,#ff8c5a);font-size:28px;font-weight:700">
        {{ user.nickname?.charAt(0) || '?' }}
      </el-avatar>
      <div class="user-meta">
        <h2>{{ user.nickname || '匿名用户' }}</h2>
        <div class="rep-row">
          <el-rate :model-value="Number(user.reputationScore || 5)" disabled size="small" />
          <span>{{ user.reputationScore || '5.0' }}</span>
        </div>
        <p v-if="user.bio && user.bio !== '这个用户还没有填写简介。'" class="user-bio">{{ user.bio }}</p>
        <div class="user-join" v-if="user.createdAt">加入于 {{ fmtDate(user.createdAt) }}</div>
      </div>
      <div class="user-actions">
        <template v-if="isSelf">
          <router-link to="/profile" class="btn-outline">编辑资料</router-link>
        </template>
        <template v-else>
          <button class="btn-primary" @click="contact" :disabled="!user.id">联系卖家</button>
        </template>
      </div>
    </div>

    <!-- 在售商品 -->
    <h3 class="section-title">在售商品（{{ total }}）</h3>
    <div v-if="products.length" class="product-grid">
      <div v-for="p in products" :key="p.id" class="product-card" @click="$router.push(`/product/${p.id}`)">
        <div class="thumb"><img :src="p.images?.[0]?.url" alt="" /></div>
        <div class="card-body">
          <div class="card-title">{{ p.title }}</div>
          <div class="card-footer">
            <span class="price">¥{{ p.price }}</span>
            <span class="cond">{{ p.conditionText }}</span>
          </div>
        </div>
      </div>
    </div>
    <el-empty v-else description="TA暂时还没有在售商品" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/userStore'
import { getUserPublicInfo } from '@/api/user'
import { getUserProducts } from '@/api/product'
import { getOrCreateConversation } from '@/api/im'

const route = useRoute()
const router = useRouter()
const store = useUserStore()

const user = ref({})
const products = ref([])
const total = ref(0)

const isSelf = computed(() => store.userInfo?.id === Number(route.params.id))

const fmtDate = (s) => (s || '').replace('T', ' ').slice(0, 10)

onMounted(async () => {
  try {
    const [u, list] = await Promise.all([
      getUserPublicInfo(route.params.id),
      getUserProducts(route.params.id, { page: 1, size: 20 })
    ])
    user.value = u || {}
    products.value = list?.records || []
    total.value = list?.total || 0
  } catch (e) { /* 拦截器已提示 */ }
})

const contact = async () => {
  if (!store.isLoggedIn()) return router.push('/login')
  try {
    const conv = await getOrCreateConversation({ targetUserId: Number(route.params.id), productId: null })
    router.push(`/chat/${conv.id}`)
  } catch (e) { /* 拦截器已提示 */ }
}
</script>

<style scoped>
.user-card {
  background: #fff; border: 1px solid #f0f2f5; border-radius: 20px;
  padding: 28px 24px; display: flex; align-items: flex-start; gap: 18px;
  margin-bottom: 24px; flex-wrap: wrap;
}
.user-meta { flex: 1; min-width: 200px; }
.user-meta h2 { font-size: 22px; font-weight: 700; color: #2d3436; margin: 0 0 6px; }
.rep-row { display: flex; align-items: center; gap: 8px; }
.rep-row span { font-size: 14px; font-weight: 600; color: #ff6b35; }
.user-bio { font-size: 14px; color: #636e72; margin: 10px 0 4px; line-height: 1.7; }
.user-join { font-size: 12px; color: #b2bec3; }
.user-actions { display: flex; gap: 10px; align-items: center; }
.btn-primary {
  padding: 10px 22px; border: none; border-radius: 12px; cursor: pointer;
  background: linear-gradient(135deg,#ff6b35,#ff8c5a); color: #fff;
  font-size: 14px; font-weight: 600; transition: all 0.25s;
}
.btn-primary:hover { transform: translateY(-1px); box-shadow: 0 6px 18px rgba(255,107,53,0.3); }
.btn-outline {
  padding: 10px 22px; border: 2px solid #ff6b35; border-radius: 12px; cursor: pointer;
  background: #fff; color: #ff6b35; font-size: 14px; font-weight: 600;
  text-decoration: none; display: inline-block; transition: all 0.25s;
}
.btn-outline:hover { background: #fff5f0; }

.section-title {
  font-size: 17px; font-weight: 700; color: #2d3436; margin: 8px 0 14px;
  padding-left: 12px; border-left: 4px solid #ff6b35;
}
.product-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(190px, 1fr));
  gap: 14px;
}
.product-card {
  background: #fff; border: 1px solid #f0f2f5; border-radius: 14px; overflow: hidden;
  cursor: pointer; transition: all 0.25s;
}
.product-card:hover { transform: translateY(-3px); box-shadow: 0 8px 24px rgba(0,0,0,0.08); }
.thumb { aspect-ratio: 4/3; background: #f8f9fa; }
.thumb img { width: 100%; height: 100%; object-fit: cover; }
.card-body { padding: 10px 12px 12px; }
.card-title {
  font-size: 13px; color: #2d3436; font-weight: 600;
  overflow: hidden; text-overflow: ellipsis; display: -webkit-box;
  -webkit-line-clamp: 1; -webkit-box-orient: vertical; margin-bottom: 6px;
}
.card-footer { display: flex; justify-content: space-between; align-items: center; }
.price { font-size: 16px; font-weight: 700; color: #e74c3c; }
.cond { font-size: 11px; color: #b2bec3; }

@media (max-width: 480px) {
  .user-card { flex-direction: column; align-items: center; text-align: center; }
  .user-actions { width: 100%; justify-content: center; }
  .product-grid { grid-template-columns: repeat(2, 1fr); gap: 10px; }
}
</style>
