<template>
  <div class="page-container">
    <!-- 分类导航 -->
    <div class="category-bar">
      <button
        v-for="cat in categories"
        :key="cat.id"
        :class="['cat-chip', { active: selectedCat === cat.id }]"
        @click="selectedCat = selectedCat === cat.id ? null : cat.id; page=1; loadProducts()"
      >
        <img v-if="catIcon(cat.name)" :src="catIcon(cat.name)" class="cat-icon-img" alt="" />
        <span>{{ cat.name }}</span>
      </button>
    </div>

    <!-- 首页轮播图（运营位） -->
    <div class="home-banner" v-if="banners.length">
      <el-carousel height="200px" :interval="4000" arrow="hover">
        <el-carousel-item v-for="b in banners" :key="b.id">
          <div class="banner-slide" @click="goBanner(b)">
            <img :src="b.imageUrl" alt="" class="banner-img" />
          </div>
        </el-carousel-item>
      </el-carousel>
    </div>

    <!-- Banner 横幅 + 公告轮播 -->
    <div class="hero-banner fade-in-up" v-if="!selectedCat && products.length > 0">
      <div class="hero-content">
        <h1>发现好物，分享闲置</h1>
        <p>每个人都可以在这里买卖二手，让闲置流转起来</p>
        <router-link to="/publish" class="hero-btn" v-if="store.isLoggedIn()">
          <el-icon><Plus /></el-icon> 发布你的第一件商品
        </router-link>
      </div>
      <div class="hero-announce" v-if="scrollList.length">
        <div class="ha-head">
          <span class="ha-title-label">📢 平台公告</span>
          <router-link to="/announcement" class="ha-more">查看全部 ›</router-link>
        </div>
        <el-carousel height="100px" direction="vertical" :interval="3000" indicator-position="none" loop>
          <el-carousel-item v-for="a in scrollList" :key="a.id" @click="$router.push('/announcement')">
            <div class="ha-slide">
              <span class="ha-label">{{ annType(a) }}</span>
              <div class="ha-text-wrap">
                <div class="ha-title">{{ a.title }}</div>
                <div class="ha-desc" v-if="a.content">{{ a.content.slice(0, 48) }}{{ a.content.length > 48 ? '…' : '' }}</div>
              </div>
            </div>
          </el-carousel-item>
        </el-carousel>
      </div>
    </div>

    <!-- 优惠券活动栏（游客可见可领券；领取需登录，弹窗完成） -->
    <div class="coupon-banner" v-if="bannerCoupons.length">
      <div class="banner-head">
        <el-icon><Ticket /></el-icon>
        <span class="coupon-title">优惠活动</span>
      </div>
      <div class="banner-tags">
        <div v-for="c in bannerCoupons" :key="c.key" class="banner-tag" :class="{ owned: c.owned }">
          <div class="tag-main">
            <strong>{{ couponFaceText(c) }}</strong>
            <span class="tag-threshold">{{ couponThresholdText(c) }}</span>
            <span class="tag-cat">{{ couponCategoryText(c) }}</span>
          </div>
          <button v-if="!c.owned" class="tag-claim" :disabled="claiming === c.couponId" @click="claimOne(c)">
            {{ claiming === c.couponId ? '领取中' : '领取' }}
          </button>
          <em v-else class="tag-state">已持有</em>
        </div>
      </div>
      <router-link v-if="store.isLoggedIn()" to="/coupon/my" class="banner-more">我的券 ›</router-link>
    </div>

    <!-- 排序 -->
    <div class="toolbar" v-if="products.length > 0">
      <span class="result-count">共 <strong>{{ total }}</strong> 件商品</span>
      <div class="sort-tabs">
        <button :class="{ active: sort === 'created_at' }" @click="sort='created_at';loadProducts()">最新</button>
        <button :class="{ active: sort === 'hot' }" @click="sort='hot';loadProducts()">🔥 人气</button>
        <button :class="{ active: sort === 'view_count' }" @click="sort='view_count';loadProducts()">最热</button>
        <button :class="{ active: sort === 'price_asc' }" @click="sort='price_asc';loadProducts()">价格↑</button>
        <button :class="{ active: sort === 'price_desc' }" @click="sort='price_desc';loadProducts()">价格↓</button>
      </div>
    </div>

    <!-- 商品网格 -->
    <div class="product-grid" v-if="products.length > 0">
      <article
        v-for="(p, idx) in products"
        :key="p.id"
        class="product-card-new fade-in-up"
        :style="{ animationDelay: idx * 0.04 + 's' }"
        @click="$router.push(`/product/${p.id}`)"
      >
        <div class="card-img-wrap">
          <product-cover :src="p.images?.[0]?.thumbUrl || p.images?.[0]?.url" :title="p.title" class="card-img" />
          <span class="condition-badge">{{ conditionMap[p.condition] || '' }}</span>
          <button class="favorite-btn-mini" @click.stop="toggleFav(p)" v-if="store.isLoggedIn()">
            <el-icon :size="16" :style="{ color: p._fav ? 'var(--c2c-primary)' : 'var(--c2c-text-muted)' }">
              <StarFilled v-if="p._fav" /><Star v-else />
            </el-icon>
          </button>
        </div>
        <div class="card-body">
          <h3 class="card-title">{{ p.title }}</h3>
          <p class="card-desc" v-if="p.description">{{ p.description.slice(0, 40) }}{{ p.description.length > 40 ? '...' : '' }}</p>
          <div class="card-footer">
            <div class="card-price">
              <span class="price-val">{{ p.price }}</span>
              <span class="price-yuan">元</span>
              <span class="price-orig" v-if="p.originalPrice && p.originalPrice > p.price">¥{{ p.originalPrice }}</span>
            </div>
            <span class="card-views">{{ p.viewCount }} 人看过</span>
          </div>
          <div class="card-seller">
            <el-avatar :size="20" style="background:var(--c2c-primary);font-size:10px">{{ p.sellerName?.charAt(0) || '?' }}</el-avatar>
            <span>{{ p.sellerName || '匿名' }}</span>
            <span class="seller-loc" v-if="p.location">{{ p.location }}</span>
          </div>
        </div>
      </article>
    </div>

    <!-- 空状态 -->
    <div class="empty-state fade-in" v-if="!loading && products.length === 0">
      <el-icon :size="64" color="var(--c2c-border)"><FolderOpened /></el-icon>
      <h3>{{ selectedCat ? '该分类暂无商品' : '暂无商品' }}</h3>
      <p>快来发布第一件商品吧</p>
      <router-link to="/publish" v-if="store.isLoggedIn()">
        <el-button type="primary" size="large" round>发布商品</el-button>
      </router-link>
    </div>

    <!-- 加载骨架（uiverse 风格 shimmer 动画） -->
    <skeleton-card v-if="loading" :count="8" />

    <!-- 分页 -->
    <div class="pagination-wrap" v-if="total > 20">
      <el-pagination
        background layout="prev, pager, next" :total="total"
        :page-size="20" v-model:current-page="page" @change="loadProducts"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/userStore'
import { useCouponStore } from '@/stores/couponStore'
import { getCategories, searchProducts } from '@/api/product'
import { addFavorite, removeFavorite } from '@/api/favorite'
import { getAnnouncementLatest } from '@/api/announcement'
import { getBanners } from '@/api/banner'
import { claimCoupon } from '@/api/coupon'
import { COUPON_TYPE_BALANCE, couponFaceText, couponThresholdText, couponCategoryText, balanceArrivedNotify } from '@/utils/coupon'
import { ElMessage } from 'element-plus'

const store = useUserStore()
const couponStore = useCouponStore()
const router = useRouter()
// 本地分类图标（后端 iconUrl 未配置时兜底，与小程序同一套风格）
const CAT_ICONS = {
  书籍: '/images/category/book.jpg', 数码: '/images/category/digital.jpg',
  手机: '/images/category/phone.jpg', 日用: '/images/category/daily.jpg',
  教材: '/images/category/textbook.jpg', 英语: '/images/category/english.jpg',
  笔记本电脑: '/images/category/laptop.jpg', 苹果手机: '/images/category/apple-phone.jpg',
  宿舍用品: '/images/category/dorm.jpg'
}
const catIcon = (name) => CAT_ICONS[name] || ''
const categories = ref([])
const selectedCat = ref(null)
const products = ref([])
const page = ref(1)
const total = ref(0)
const sort = ref('created_at')
const loading = ref(false)
const announcements = ref([])
const banners = ref([])
// 首页券活动栏：已持有未用券 + 可领券（登录后拉取，无券不展示）
const ownedCoupons = ref([])
const claimableCoupons = ref([])
const claiming = ref(null)
const bannerCoupons = computed(() => [...ownedCoupons.value, ...claimableCoupons.value].slice(0, 4))
const conditionMap = { 1: '全新', 2: '几乎全新', 3: '轻微使用', 4: '明显使用' }

const annType = (a) => ({ 1: '公告', 2: '平台公约', 3: '通知' }[a?.type] || '公告')
// 参与滚动的公告（scroll=1 或未设置），默认滚动显示
const scrollList = computed(() => (announcements.value || []).filter(a => a.scroll !== 0))

const loadProducts = async () => {
  loading.value = true
  try {
    const res = await searchProducts({
      categoryId: selectedCat.value,
      page: page.value,
      size: 20,
      sort: sort.value
    })
    products.value = (res.records || []).map(p => ({ ...p, _fav: false }))
    total.value = res.total || 0
  } catch (e) { products.value = [] }
  loading.value = false
}

const toggleFav = async (p) => {
  if (!store.isLoggedIn()) return
  try {
    if (p._fav) { await removeFavorite(p.id); p._fav = false }
    else { await addFavorite(p.id); p._fav = true }
  } catch (e) { /* */ }
}

// 券活动栏：我的未用券优先，其次可领未领券；首页不按分类过滤；游客也可看可领券；失败内部消化
const loadCouponBanner = async () => {
  try {
    const mineP = store.isLoggedIn() ? couponStore.fetchMine().catch(() => []) : Promise.resolve([])
    const [mine, avail] = await Promise.all([
      mineP,
      couponStore.fetchAvailable().catch(() => [])
    ])
    ownedCoupons.value = (mine || []).map(c => ({ ...c, key: 'own-' + c.userCouponId, owned: true }))
    claimableCoupons.value = (avail || [])
      .filter(c => !c.claimed && c.remaining > 0)
      .map(c => ({ ...c, key: 'avail-' + c.couponId, owned: false }))
  } catch (e) { /* 拦截器已提示 */ }
}

// 活动栏直接领取：未登录先弹登录窗，登录成功后刷新券栏；成功后移入已持有列表
const claimOne = async (c) => {
  if (!store.isLoggedIn()) {
    store.openLoginDialog(() => loadCouponBanner())
    return
  }
  claiming.value = c.couponId
  try {
    await claimCoupon(c.couponId)
    if (Number(c.type) === COUPON_TYPE_BALANCE) balanceArrivedNotify(c)
    else ElMessage.success('领取成功，可在「我的优惠券」查看')
    couponStore.invalidate()
    claimableCoupons.value = claimableCoupons.value.filter(x => x.couponId !== c.couponId)
    ownedCoupons.value.unshift({ ...c, key: 'own-' + c.couponId, owned: true })
  } catch (e) { /* 拦截器已提示 */ } finally {
    claiming.value = null
  }
}

onMounted(() => {
  // 首页各模块并行加载、互不阻塞：任一接口慢/失败不影响其他区域渲染
  // （修复：登录后弹公告期间页面空白 = 原串行 await 分类接口被阻塞，整页等待）
  getCategories().then(r => { categories.value = r || []; couponStore.hydrateCategoryTree(r) }).catch(() => { categories.value = [] })
  loadProducts()
  // 加载最新公告（hero 右侧轮播）
  getAnnouncementLatest(6).then(r => {
    announcements.value = r || []
  }).catch(() => {})
  // 加载轮播图
  getBanners().then(r => { banners.value = r || [] }).catch(() => {})
  // 加载券活动栏（登录后）
  loadCouponBanner()
})

const goBanner = (b) => { if (b.linkUrl) window.open(b.linkUrl, '_blank') }
</script>

<style scoped>
/* 分类导航 */
.category-bar {
  display: flex; flex-wrap: wrap; gap: 8px;
  margin-bottom: 24px; padding-bottom: 20px;
  border-bottom: 1px solid var(--c2c-line-soft);
}
.cat-chip {
  padding: 8px 18px; border-radius: 22px;
  border: 1px solid var(--c2c-line-soft); background: var(--glass-bg);
  backdrop-filter: var(--glass-blur); -webkit-backdrop-filter: var(--glass-blur);
  font-size: 13px; font-weight: 500; color: var(--c2c-text-secondary);
  cursor: pointer; transition: all 0.25s;
  display: flex; align-items: center; gap: 6px;
  white-space: nowrap;
}
.cat-chip:hover { border-color: var(--c2c-primary); color: var(--c2c-primary); background: #fff8f5; }
.cat-chip.active { background: var(--c2c-primary); color: #fff; border-color: var(--c2c-primary); box-shadow: 0 4px 14px rgba(255,107,53,0.28); }
.cat-icon-img { width: 20px; height: 20px; border-radius: 6px; object-fit: cover; flex-shrink: 0; }

/* 首页轮播图 */
.home-banner { margin-bottom: 16px; border-radius: 14px; overflow: hidden; }
.banner-slide { position: relative; height: 100%; cursor: pointer; }
.banner-img { width: 100%; height: 100%; object-fit: cover; }

/* Banner + 公告轮播 */
.hero-banner {
  display: flex; align-items: center; gap: 32px;
  background: linear-gradient(135deg, rgba(255,245,240,0.9) 0%, rgba(255,232,224,0.7) 50%, rgba(255,255,255,0.72) 100%);
  backdrop-filter: var(--glass-blur); -webkit-backdrop-filter: var(--glass-blur);
  border-radius: 22px; padding: 40px 48px; margin-bottom: 28px;
  border: 1px solid var(--glass-border);
  box-shadow: var(--glass-shadow);
}
.hero-content { flex: 1; min-width: 0; }
.hero-content h1 { font-size: 28px; font-weight: 800; color: var(--c2c-text); margin-bottom: 8px; }
.hero-content p { font-size: 15px; color: var(--c2c-text-secondary); margin-bottom: 20px; }
.hero-btn {
  display: inline-flex; align-items: center; gap: 6px;
  background: var(--c2c-primary); color: #fff; padding: 12px 28px;
  border-radius: 28px; font-size: 15px; font-weight: 600;
  text-decoration: none; transition: all 0.25s;
}
.hero-btn:hover { background: var(--c2c-primary-dark); transform: translateY(-2px); box-shadow: 0 6px 20px rgba(255,107,53,0.35); }

/* hero 右侧公告轮播 */
.hero-announce {
  flex: 1.7; min-width: 0; background: var(--glass-bg);
  backdrop-filter: var(--glass-blur); -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border); border-radius: 18px; padding: 16px 22px;
  box-shadow: var(--shadow-sm);
}
.ha-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 6px; }
.ha-title-label { font-size: 13px; font-weight: 700; color: var(--c2c-text); }
.ha-more { font-size: 12px; color: var(--c2c-primary); text-decoration: none; font-weight: 600; }
.ha-slide { display: flex; align-items: center; gap: 10px; height: 100%; cursor: pointer; }
.ha-label {
  flex-shrink: 0; font-size: 11px; color: var(--c2c-primary-dark); font-weight: 700;
  background: #ffe8dc; padding: 2px 10px; border-radius: 8px;
}
.ha-text-wrap { min-width: 0; }
.ha-title { font-size: 14px; font-weight: 600; color: var(--c2c-text); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.ha-desc { font-size: 12px; color: var(--c2c-text-muted); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; margin-top: 2px; }

/* 优惠券活动栏 */
.coupon-banner {
  display: flex; align-items: center; gap: 12px;
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur); -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border); border-radius: 18px;
  padding: 12px 16px; margin-bottom: 20px;
  box-shadow: var(--shadow-sm);
}
.banner-head { display: flex; align-items: center; gap: 6px; flex-shrink: 0; }
.banner-head .el-icon { color: var(--c2c-primary); }
.coupon-title { font-size: 14px; font-weight: 700; color: var(--c2c-text); }
.banner-tags { display: flex; gap: 8px; flex-wrap: wrap; flex: 1; }
.banner-tag {
  display: inline-flex; align-items: center; gap: 8px;
  background: var(--c2c-surface); border: 1px solid var(--c2c-primary-light);
  border-radius: 10px; padding: 6px 10px; font-size: 12px;
}
.tag-main { display: inline-flex; align-items: baseline; gap: 6px; flex-wrap: wrap; }
.banner-tag strong { color: var(--c2c-danger); font-size: 15px; }
.tag-threshold { color: var(--c2c-text-secondary); }
.tag-cat { color: var(--c2c-text-muted); }
.tag-state { font-style: normal; color: var(--c2c-primary); font-weight: 600; }
.tag-claim {
  flex-shrink: 0; border: none; cursor: pointer;
  background: var(--c2c-primary); color: #fff;
  font-size: 12px; font-weight: 600; border-radius: 8px; padding: 3px 10px;
}
.tag-claim:disabled { opacity: 0.6; cursor: default; }
.banner-tag.owned { border-color: var(--c2c-line-soft); }
.banner-more { flex-shrink: 0; font-size: 13px; color: var(--c2c-primary); text-decoration: none; font-weight: 600; }

/* 工具栏 */
.toolbar {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 20px; flex-wrap: wrap; gap: 12px;
}
.result-count { font-size: 14px; color: var(--c2c-text-secondary); }
.result-count strong { color: var(--c2c-text); }
.sort-tabs { display: flex; gap: 4px; }
.sort-tabs button {
  padding: 6px 14px; border: none; background: transparent;
  font-size: 13px; color: var(--c2c-text-secondary); cursor: pointer; border-radius: 6px;
  transition: all 0.2s; font-weight: 500;
}
.sort-tabs button:hover { background: var(--c2c-bg-soft); color: var(--c2c-text); }
.sort-tabs button.active { background: var(--c2c-primary); color: #fff; box-shadow: 0 3px 10px rgba(255,107,53,0.25); }

/* 商品卡片新版 */
.product-card-new {
  background: var(--glass-bg-strong);
  backdrop-filter: var(--glass-blur); -webkit-backdrop-filter: var(--glass-blur);
  border-radius: 20px; overflow: hidden;
  border: 1px solid var(--glass-border); cursor: pointer;
  box-shadow: var(--shadow-sm);
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
}
.product-card-new:hover {
  transform: translateY(-8px);
  box-shadow: var(--glass-shadow-hover);
  border-color: transparent;
}
.card-img-wrap {
  position: relative; overflow: hidden;
  height: 200px; background: var(--c2c-bg);
}
.card-img-wrap::after {
  content: ''; position: absolute; inset: 0;
  opacity: 0; transition: opacity 0.35s ease;
  background: linear-gradient(180deg, transparent 40%, rgba(255,107,53,0.08) 100%);
}
.product-card-new:hover .card-img-wrap::after { opacity: 1; }
.card-img {
  width: 100%; height: 100%; object-fit: cover;
  transition: transform 0.5s ease;
}
.product-card-new:hover .card-img { transform: scale(1.08); }
.condition-badge {
  position: absolute; top: 10px; left: 10px;
  background: rgba(0,0,0,0.55); color: #fff;
  padding: 3px 10px; border-radius: 10px; font-size: 11px;
  backdrop-filter: blur(4px);
}
.favorite-btn-mini {
  position: absolute; top: 10px; right: 10px;
  width: 32px; height: 32px; border-radius: 50%;
  background: var(--glass-bg-strong); border: none;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; backdrop-filter: blur(4px);
  transition: all 0.2s;
}
.favorite-btn-mini:hover { transform: scale(1.1); }
.card-body { padding: 14px 16px; }
.card-title {
  font-size: 15px; font-weight: 600; color: var(--c2c-text);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  margin-bottom: 4px;
}
.card-desc { font-size: 12px; color: var(--c2c-text-muted); margin-bottom: 10px; }
.card-footer { display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 10px; }
.price-val { font-size: 22px; font-weight: 700; color: var(--c2c-danger); }
.price-yuan { font-size: 13px; color: var(--c2c-danger); margin-left: 2px; }
.price-orig { font-size: 12px; color: var(--c2c-text-muted); text-decoration: line-through; margin-left: 6px; }
.card-views { font-size: 11px; color: var(--c2c-text-muted); }
.card-seller { display: flex; align-items: center; gap: 6px; font-size: 12px; color: var(--c2c-text-secondary); }
.seller-loc { margin-left: auto; color: var(--c2c-text-muted); }

/* 空状态 */
.empty-state {
  text-align: center; padding: 80px 20px;
  display: flex; flex-direction: column; align-items: center; gap: 12px;
}
.empty-state h3 { font-size: 18px; color: var(--c2c-text-secondary); }
.empty-state p { font-size: 14px; color: var(--c2c-text-muted); margin-bottom: 8px; }

/* 骨架屏 */
.skeleton-card { padding: 0; background: var(--glass-bg-strong); border-radius: 20px; overflow: hidden; }
.skeleton-img { width: 100%; height: 200px; background: linear-gradient(90deg, var(--c2c-bg-soft) 25%, #e4e7ed 50%, var(--c2c-bg-soft) 75%); background-size: 200% 100%; animation: shimmer 1.5s infinite; }
.skeleton-line { height: 14px; margin: 10px 16px; background: var(--c2c-bg-soft); border-radius: 7px; }
.skeleton-line.w80 { width: 80%; } .skeleton-line.w60 { width: 60%; } .skeleton-line.w40 { width: 40%; }
@keyframes shimmer { 0% { background-position: -200% 0; } 100% { background-position: 200% 0; } }

.pagination-wrap { display: flex; justify-content: center; margin-top: 32px; }

/* 暗色覆盖 */
html.dark .hero-banner {
  background: linear-gradient(135deg, rgba(255,107,53,0.16) 0%, rgba(255,140,90,0.10) 50%, var(--glass-bg-strong) 100%);
}
html.dark .skeleton-img {
  background: linear-gradient(90deg, var(--c2c-bg-soft) 25%, #2a2e36 50%, var(--c2c-bg-soft) 75%);
}
</style>
@media (max-width: 480px) {
  .hero-banner { flex-direction: column; align-items: stretch; gap: 16px; padding: 24px 20px; margin-bottom: 16px; }
  .hero-content h1 { font-size: 22px; }
  .coupon-banner { flex-wrap: wrap; gap: 8px; padding: 10px 12px; }
  .banner-more { width: 100%; text-align: right; }
}
