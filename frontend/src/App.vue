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

    <!-- 强制公告弹窗（登录后弹出，最低停留秒数内不可关闭） -->
    <el-dialog v-model="forceDialog" :show-close="false" width="460px" class="force-dialog"
      :close-on-click-modal="false" :close-on-press-escape="false" align-center>
      <template #header>
        <div class="force-dialog-head">
          <span class="fd-icon">📢</span>
          <span>{{ current?.title || '平台公告' }}</span>
          <el-tag v-if="current?.isForce === 1" type="danger" size="small">强制阅读</el-tag>
        </div>
      </template>
      <div class="force-dialog-body">{{ current?.content }}</div>
      <template #footer>
        <div class="force-dialog-foot">
          <span v-if="remain > 0" class="remain-tip">请阅读 {{ remain }} 秒后可关闭…</span>
          <el-button v-else type="primary" @click="nextOrClose">
            {{ queue.length > 1 ? '下一条' : '我知道了' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, watch, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/userStore'
import { getAnnouncementForce } from '@/api/announcement'
import AppHeader from './components/layout/AppHeader.vue'
import AppFooter from './components/layout/AppFooter.vue'

const route = useRoute()
const router = useRouter()
const store = useUserStore()
const isMobile = ref(window.innerWidth < 768)
const showBackTop = ref(false)

// ---- 强制公告弹窗 ----
const forceDialog = ref(false)
const queue = ref([])        // 待弹出的强制公告队列
const current = ref(null)    // 当前展示的公告
const remain = ref(0)        // 剩余停留秒数
let countdownTimer = null

const showNext = () => {
  if (!queue.value.length) { forceDialog.value = false; current.value = null; return }
  current.value = queue.value.shift()
  remain.value = Number(current.value.minSeconds || 0)
  forceDialog.value = true
  if (countdownTimer) clearInterval(countdownTimer)
  if (remain.value > 0) {
    countdownTimer = setInterval(() => {
      remain.value--
      if (remain.value <= 0) { clearInterval(countdownTimer); countdownTimer = null }
    }, 1000)
  }
}
const nextOrClose = () => { showNext() }

// 登录后拉取强制公告并弹出
watch(() => store.isLoggedIn(), async (loggedIn) => {
  if (!loggedIn) return
  try {
    const list = await getAnnouncementForce()
    queue.value = (list || []).slice()
    if (queue.value.length) showNext()
  } catch (e) { /* 接口异常忽略，不影响使用 */ }
}, { immediate: true })

onUnmounted(() => { if (countdownTimer) clearInterval(countdownTimer) })

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

/* 强制公告弹窗 */
.force-dialog { border-radius: 16px; }
.force-dialog-head {
  display: flex; align-items: center; gap: 8px; font-size: 17px; font-weight: 700;
}
.fd-icon { font-size: 20px; }
.force-dialog-body {
  white-space: pre-wrap; word-break: break-word;
  font-size: 14px; line-height: 1.9; color: #4a5568;
  background: #fafafa; border-radius: 12px; padding: 16px;
  max-height: 320px; overflow-y: auto;
}
.force-dialog-foot { display: flex; align-items: center; justify-content: space-between; }
.remain-tip { font-size: 13px; color: #e6a23c; font-weight: 600; }

@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }

@media (max-width: 768px) {
  .back-top { bottom: 20px; right: 12px; width: 38px; height: 38px; }
  .main-content.has-header { margin-top: 56px; }
}
@media (max-width: 480px) {
  .back-top { bottom: 16px; right: 10px; width: 34px; height: 34px; }
}
</style>
