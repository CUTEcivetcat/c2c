<template>
  <div id="app-container" :class="{ 'mobile': isMobile }">
    <app-header v-if="showHeader" />
    <main class="main-content" :class="mainClass">
      <router-view v-slot="{ Component }">
        <transition :name="transitionName" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
    <app-footer v-if="showFooter" />
    <!-- 回到顶部 -->
    <div v-show="showBackTop" class="back-top" @click="scrollToTop">
      <el-icon :size="22"><Top /></el-icon>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from './components/layout/AppHeader.vue'
import AppFooter from './components/layout/AppFooter.vue'

const route = useRoute()
const isMobile = ref(window.innerWidth < 768)
const showBackTop = ref(false)

// 隐藏头尾的页面
const hideLayoutPages = ['Chat']
const showHeader = computed(() => !hideLayoutPages.includes(route.name))
const showFooter = computed(() => !hideLayoutPages.includes(route.name))
const mainClass = computed(() => ({
  'has-header': showHeader.value,
  'chat-page': route.name === 'Chat'
}))
const transitionName = computed(() => 'fade')

// 响应式
const onResize = () => { isMobile.value = window.innerWidth < 768 }
const onScroll = () => { showBackTop.value = window.scrollY > 300 }
const scrollToTop = () => { window.scrollTo({ top: 0, behavior: 'smooth' }) }

onMounted(() => {
  window.addEventListener('resize', onResize)
  window.addEventListener('scroll', onScroll)
})
onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  window.removeEventListener('scroll', onScroll)
})
</script>

<style>
#app-container {
  min-height: 100vh;
  min-height: 100dvh; /* 移动端动态viewport高度 */
  display: flex;
  flex-direction: column;
  background: #f8f9fa;
}
.main-content {
  flex: 1;
  width: 100%;
  animation: fadeIn 0.3s ease-out;
}
.main-content.has-header {
  margin-top: 60px;
}
.main-content.chat-page {
  margin-top: 0;
  height: 100vh;
  overflow: hidden;
}

/* 路由过渡动画 */
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.fade-enter-from { opacity: 0; transform: translateY(8px); }
.fade-leave-to { opacity: 0; transform: translateY(-8px); }

/* 回到顶部 */
.back-top {
  position: fixed; bottom: 32px; right: 32px; z-index: 999;
  width: 44px; height: 44px; border-radius: 50%;
  background: #fff; box-shadow: 0 4px 16px rgba(0,0,0,0.12);
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; transition: all 0.3s;
  color: #636e72;
}
.back-top:hover { background: #ff6b35; color: #fff; transform: translateY(-2px); }

@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }

@media (max-width: 768px) {
  .back-top { bottom: 20px; right: 12px; width: 38px; height: 38px; }
  .main-content.has-header { margin-top: 56px; }
}
@media (max-width: 480px) {
  .back-top { bottom: 16px; right: 10px; width: 34px; height: 34px; }
}
</style>
