<template>
  <div class="page-back-bar fade-in-up">
    <button class="back-btn" type="button" @click="goBack" :aria-label="'返回'">
      <el-icon :size="18"><ArrowLeft /></el-icon>
    </button>
    <h2 class="page-back-title">{{ title }}</h2>
    <div class="page-back-extra"><slot /></div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

defineProps({
  title: { type: String, default: '' }
})

const router = useRouter()
const goBack = () => {
  if (window.history.length > 1) router.back()
  else router.push('/')
}
</script>

<style scoped>
.page-back-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.back-btn {
  width: 34px; height: 34px;
  display: flex; align-items: center; justify-content: center;
  border: none; border-radius: 50%;
  background: #fff; color: #2d3436;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  cursor: pointer; transition: all 0.25s; flex-shrink: 0;
}
.back-btn:hover { background: #ff6b35; color: #fff; transform: translateX(-2px); }
.page-back-title { font-size: 18px; font-weight: 700; color: #2d3436; margin: 0; }
.page-back-extra { margin-left: auto; }
@media (max-width: 480px) {
  .page-back-bar { padding: 10px 12px 2px; }
  .page-back-title { font-size: 16px; }
}
</style>
