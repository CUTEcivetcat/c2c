<template>
  <header class="app-header">
    <div class="header-inner">
      <!-- 手机汉堡按钮 -->
      <button class="hamburger-btn" @click="mobileMenuOpen=!mobileMenuOpen">
        <el-icon :size="22"><component :is="mobileMenuOpen ? 'Close' : 'Menu'" /></el-icon>
      </button>

      <!-- Logo -->
      <router-link to="/" class="logo">
        <span class="logo-icon">🔄</span>
        <span class="logo-text">闲小鱼</span>
        <span class="logo-sub">二手</span>
      </router-link>

      <!-- 搜索栏（小屏隐藏） -->
      <div class="search-bar hidden-mobile">
        <div class="search-input-wrap">
          <el-icon class="search-icon"><Search /></el-icon>
          <input v-model="keyword" type="text" placeholder="搜索你想要的二手好物…" class="search-input" @keyup.enter="goSearch" />
          <button v-if="keyword" class="search-clear" @click="keyword=''" type="button"><el-icon><Close /></el-icon></button>
        </div>
      </div>

      <!-- 桌面端操作 -->
      <div class="nav-actions hidden-mobile">
        <template v-if="store.isLoggedIn()">
          <router-link to="/publish" class="btn-publish"><el-icon><Plus /></el-icon><span>发布</span></router-link>
          <router-link v-if="store.userInfo?.role === 2" to="/review" class="review-entry" @click="mobileMenuOpen=false">🛡️ 审核</router-link>
          <router-link to="/chat" class="nav-icon-btn">
            <el-badge :value="unread" :hidden="!unread" :max="99"><el-icon :size="22"><ChatDotRound /></el-icon></el-badge>
          </router-link>
          <router-link to="/favorite" class="nav-icon-btn"><el-icon :size="22"><Star /></el-icon></router-link>
          <el-dropdown trigger="click" popper-class="user-dropdown">
            <div class="user-avatar-btn">
              <el-avatar :size="34" style="background:linear-gradient(135deg,#ff6b35,#ff8c5a)">{{ store.userInfo?.nickname?.charAt(0) || 'U' }}</el-avatar>
            </div>
            <template #dropdown>
              <div class="dropdown-user-card">
                <el-avatar :size="48" style="background:linear-gradient(135deg,#ff6b35,#ff8c5a);font-size:20px">{{ store.userInfo?.nickname?.charAt(0) || 'U' }}</el-avatar>
                <div class="dropdown-user-info"><strong>{{ store.userInfo?.nickname }}</strong><span>信誉 {{ store.userInfo?.reputationScore || '5.0' }} ⭐</span></div>
              </div>
              <el-dropdown-item @click="$router.push('/profile')"><el-icon><User /></el-icon> 个人中心</el-dropdown-item>
              <el-dropdown-item @click="$router.push('/order/list')"><el-icon><Document /></el-icon> 我的订单</el-dropdown-item>
              <el-dropdown-item @click="$router.push('/intent')"><el-icon><Pointer /></el-icon> 我的意向</el-dropdown-item>
              <el-dropdown-item @click="$router.push('/my/report')"><el-icon><Warning /></el-icon> 我的举报</el-dropdown-item>
              <el-dropdown-item @click="$router.push('/my/appeal')"><el-icon><RefreshLeft /></el-icon> 我的整改申诉</el-dropdown-item>
              <el-dropdown-item v-if="store.userInfo?.role === 2" @click="$router.push('/review')"><el-icon><Checked /></el-icon> 审核工作台</el-dropdown-item>
              <el-dropdown-item v-if="store.userInfo?.role === 1" @click="goAdmin"><el-icon><Monitor /></el-icon> 管理后台</el-dropdown-item>
              <el-dropdown-item divided @click="$router.push('/announcement')"><el-icon><Bell /></el-icon> 平台公告</el-dropdown-item>
              <el-dropdown-item @click="$router.push('/address')"><el-icon><Location /></el-icon> 收货地址</el-dropdown-item>
              <el-dropdown-item divided @click="handleLogout"><el-icon><SwitchButton /></el-icon> 退出登录</el-dropdown-item>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <router-link to="/login"><el-button type="primary" size="large" round>登录 / 注册</el-button></router-link>
        </template>
      </div>

      <!-- 手机端登录按钮 -->
      <div class="mobile-auth" v-if="!store.isLoggedIn()">
        <router-link to="/login"><el-button type="primary" size="small" round>登录</el-button></router-link>
      </div>
      <div class="mobile-auth" v-else>
        <router-link to="/publish" class="mobile-publish-btn"><el-icon><Plus /></el-icon></router-link>
      </div>
    </div>

    <!-- 手机端下拉菜单 -->
    <transition name="slide">
      <div v-if="mobileMenuOpen" class="mobile-menu" @click="mobileMenuOpen=false">
        <div class="mobile-search">
          <input v-model="keyword" type="text" placeholder="搜索商品…" @keyup.enter="goSearch;mobileMenuOpen=false" />
        </div>
        <router-link to="/" class="mobile-nav-item" @click="mobileMenuOpen=false">🏠 首页</router-link>
        <router-link to="/search" class="mobile-nav-item" @click="mobileMenuOpen=false">🔍 搜索</router-link>
        <template v-if="store.isLoggedIn()">
          <router-link to="/publish" class="mobile-nav-item" @click="mobileMenuOpen=false">➕ 发布商品</router-link>
          <router-link to="/chat" class="mobile-nav-item" @click="mobileMenuOpen=false">💬 消息<el-badge v-if="unread" :value="unread" style="margin-left:6px" /></router-link>
          <router-link to="/favorite" class="mobile-nav-item" @click="mobileMenuOpen=false">⭐ 收藏</router-link>
          <router-link to="/profile" class="mobile-nav-item" @click="mobileMenuOpen=false">👤 个人中心</router-link>
          <router-link to="/order/list" class="mobile-nav-item" @click="mobileMenuOpen=false">📋 我的订单</router-link>
          <router-link to="/intent" class="mobile-nav-item" @click="mobileMenuOpen=false">🎯 我的意向</router-link>
          <router-link to="/my/report" class="mobile-nav-item" @click="mobileMenuOpen=false">⚑ 我的举报</router-link>
          <router-link to="/my/appeal" class="mobile-nav-item" @click="mobileMenuOpen=false">🔁 我的整改申诉</router-link>
          <router-link v-if="store.userInfo?.role === 2" to="/review" class="mobile-nav-item" @click="mobileMenuOpen=false">🛡️ 审核工作台</router-link>
          <router-link to="/address" class="mobile-nav-item" @click="mobileMenuOpen=false">📍 收货地址</router-link>
          <div class="mobile-nav-item" @click="handleLogout">🚪 退出登录</div>
        </template>
      </div>
    </transition>
  </header>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/userStore'
import { getUnreadCount } from '@/api/im'

const router = useRouter()
const route = useRoute()
const store = useUserStore()
const keyword = ref('')
const unread = ref(0)
const mobileMenuOpen = ref(false)

const goSearch = () => { if (keyword.value.trim()) { router.push({ name: 'Search', query: { keyword: keyword.value.trim() } }); mobileMenuOpen.value = false } }
const handleLogout = async () => { await store.logout(); router.push('/'); mobileMenuOpen.value = false }
// 管理后台入口：开发环境跳本地管理端(5174)，生产跳 /admin/
const goAdmin = () => {
  const url = import.meta.env.DEV ? 'http://localhost:5174/' : '/admin/'
  window.open(url, '_blank', 'noopener')
}

// 未读角标：不主动轮询/推送，仅在"登录后"和"页面切换时"刷新（按需同步）
const fetchUnread = async () => {
  if (!store.isLoggedIn()) return
  try { const r = await getUnreadCount(); unread.value = r.unreadTotal || 0 } catch (e) { /* */ }
}
onMounted(() => { fetchUnread() })
watch(() => route.fullPath, () => { fetchUnread() })
</script>

<style scoped>
.app-header { position: fixed; top: 0; left: 0; right: 0; z-index: 1000; height: 56px; background: rgba(255,255,255,0.94); backdrop-filter: blur(20px); border-bottom: 1px solid rgba(0,0,0,0.06); box-shadow: 0 1px 8px rgba(0,0,0,0.04); }
.header-inner { max-width: 1200px; margin: 0 auto; height: 100%; padding: 0 16px; display: flex; align-items: center; gap: 12px; }

/* Logo */
.logo { display: flex; align-items: center; gap: 4px; text-decoration: none; white-space: nowrap; flex-shrink: 0; }
.logo-icon { font-size: 22px; }
.logo-text { font-size: 18px; font-weight: 800; color: #2d3436; }
.logo-sub { font-size: 10px; color: #ff6b35; background: #fff5f0; padding: 1px 6px; border-radius: 8px; font-weight: 600; }

/* 搜索栏 */
.search-bar { flex: 1; max-width: 420px; }
.search-input-wrap {
  display: flex; align-items: center; gap: 8px;
  background: #f0f2f5; border-radius: 24px; padding: 0 16px; height: 38px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 2px solid transparent; box-shadow: 0 1px 4px rgba(0,0,0,0.02);
}
.search-input-wrap:focus-within {
  background: #fff; border-color: #ff6b35;
  box-shadow: 0 0 0 4px rgba(255,107,53,0.12), 0 4px 16px rgba(255,107,53,0.08);
  transform: scale(1.02);
}
.search-icon { font-size: 16px; color: #b2bec3; flex-shrink: 0; transition: color 0.3s; }
.search-input-wrap:focus-within .search-icon { color: #ff6b35; }
.search-input { flex: 1; border: none; outline: none; background: transparent; font-size: 13px; color: #2d3436; }
.search-input::placeholder { color: #b2bec3; }
.search-clear { border: none; background: #dfe6e9; cursor: pointer; width: 20px; height: 20px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 10px; transition: all 0.2s; }
.search-clear:hover { background: #b2bec3; color: #fff; }

/* 桌面操作 */
.nav-actions { display: flex; align-items: center; gap: 6px; flex-shrink: 0; }
.btn-publish { display: flex; align-items: center; gap: 3px; background: #ff6b35; color: #fff; padding: 5px 14px; border-radius: 18px; font-size: 13px; font-weight: 600; text-decoration: none; transition: all 0.25s; }
.btn-publish:hover { background: #e55a2b; transform: translateY(-1px); }
.review-entry {
  display: flex; align-items: center; padding: 4px 12px; border-radius: 16px;
  background: #f5eeff; color: #8e44ad; font-size: 13px; font-weight: 700;
  text-decoration: none; transition: all 0.25s; white-space: nowrap;
}
.review-entry:hover { background: #8e44ad; color: #fff; }
.nav-icon-btn { width: 34px; height: 34px; display: flex; align-items: center; justify-content: center; border-radius: 50%; color: #636e72; transition: all 0.25s; text-decoration: none; }
.nav-icon-btn:hover { background: #f0f2f5; color: #ff6b35; }
.user-avatar-btn { cursor: pointer; padding: 1px; border-radius: 50%; transition: all 0.25s; }
.user-avatar-btn:hover { box-shadow: 0 0 0 3px rgba(255,107,53,0.2); }

/* 汉堡按钮 */
.hamburger-btn { display: none; border: none; background: none; cursor: pointer; padding: 6px; color: #2d3436; }
.mobile-auth { display: none; margin-left: auto; }
.mobile-publish-btn { color: #ff6b35; font-size: 20px; }

/* 手机菜单 */
.mobile-menu { display: none; position: fixed; top: 56px; left: 0; right: 0; bottom: 0; background: rgba(255,255,255,0.98); backdrop-filter: blur(10px); z-index: 999; padding: 12px 16px; overflow-y: auto; }
.mobile-search { margin-bottom: 12px; }
.mobile-search input { width: 100%; padding: 10px 16px; border: 2px solid #e4e7ed; border-radius: 12px; font-size: 15px; outline: none; }
.mobile-search input:focus { border-color: #ff6b35; }
.mobile-nav-item { display: block; padding: 14px 8px; font-size: 16px; color: #2d3436; text-decoration: none; border-bottom: 1px solid #f5f5f5; cursor: pointer; transition: all 0.2s; }
.mobile-nav-item:hover { color: #ff6b35; background: #fff5f0; padding-left: 14px; }
.slide-enter-active, .slide-leave-active { transition: all 0.25s ease; }
.slide-enter-from, .slide-leave-to { opacity: 0; transform: translateY(-10px); }

/* 响应式 */
@media (max-width: 768px) {
  .hidden-mobile { display: none !important; }
  .hamburger-btn { display: block; }
  .mobile-auth { display: block; }
  .mobile-menu { display: block; }
  .logo-text { font-size: 16px; }
  .logo-icon { font-size: 20px; }
  .logo-sub { display: none; }
  .header-inner { padding: 0 10px; gap: 8px; }
}
@media (min-width: 769px) {
  .mobile-menu { display: none !important; }
}
</style>
