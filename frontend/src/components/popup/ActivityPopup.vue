<template>
  <el-dialog v-model="visible" width="360px" align-center :close-on-click-modal="false"
    :destroy-on-close="true" append-to-body @closed="onClosed">
    <template #header>
      <span class="popup-title">🎉 {{ current?.title || '活动提示' }}</span>
    </template>
    <div class="popup-body">
      <img v-if="current?.imageUrl" :src="current.imageUrl" class="popup-img" alt="活动配图" />
      <div class="popup-content">{{ current?.content }}</div>
    </div>
    <template #footer>
      <el-button @click="close">稍后再说</el-button>
      <el-button v-if="hasJump" type="primary" @click="goJump">去看看</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/userStore'
import { getActivePopups } from '@/api/wallet'

const router = useRouter()
const store = useUserStore()

const visible = ref(false)
const queue = ref([])
const current = ref(null)

// 已弹记录按账号隔离存 localStorage：每个用户对同一弹窗只展示一次（跨会话持久）
const shownKey = () => `c2c_popup_shown_${store.userInfo?.id || 'guest'}`

const hasJump = computed(() => {
  const t = current.value?.jumpType
  return t && t !== 'none'
})

const load = async () => {
  try {
    const list = await getActivePopups() || []
    const shown = JSON.parse(localStorage.getItem(shownKey()) || '[]')
    queue.value = list.filter(p => !shown.includes(p.id))
    if (queue.value.length) showNext()
  } catch (e) { /* 接口异常不影响主流程 */ }
}

const showNext = () => {
  if (!queue.value.length) { visible.value = false; return }
  current.value = queue.value.shift()
  visible.value = true
}

const markShown = () => {
  if (!current.value) return
  const shown = JSON.parse(localStorage.getItem(shownKey()) || '[]')
  if (!shown.includes(current.value.id)) {
    shown.push(current.value.id)
    localStorage.setItem(shownKey(), JSON.stringify(shown))
  }
}

const close = () => {
  markShown()
  visible.value = false
}

const onClosed = () => {
  current.value = null
  showNext()
}

const goJump = () => {
  const p = current.value
  markShown()
  visible.value = false
  switch (p.jumpType) {
    case 'product':
      router.push(`/product/${p.jumpTarget}`)
      break
    case 'coupon':
      router.push('/coupon/my')
      break
    case 'announcement':
      router.push('/announcement')
      break
    case 'url':
      if (p.jumpTarget) window.open(p.jumpTarget, '_blank')
      break
    default:
      break
  }
}

// 登录后拉取生效弹窗；当前账号已弹过的不再重复（localStorage 按账号持久）
watch(() => store.isLoggedIn(), (logged) => { if (logged) load() }, { immediate: true })
</script>

<style scoped>
.popup-title { font-size: 17px; font-weight: 700; }
.popup-body { text-align: center; }
.popup-img { width: 100%; border-radius: 12px; margin-bottom: 12px; }
.popup-content { white-space: pre-wrap; word-break: break-word; font-size: 14px; line-height: 1.8; color: var(--c2c-text-secondary); }
</style>
