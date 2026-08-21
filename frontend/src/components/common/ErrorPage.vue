<template>
  <div class="err-page">
    <div class="err-box">
      <div class="err-icon" :class="kind">
        <span v-if="kind === 'error'">✖</span>
        <span v-else-if="kind === 'warn'">!</span>
        <span v-else>😢</span>
      </div>
      <h2 class="err-title">{{ title }}</h2>
      <p class="err-desc">{{ message }}</p>
      <div class="err-actions">
        <el-button v-if="retry" type="primary" round @click="$emit('retry')">
          <el-icon style="margin-right:6px"><Refresh /></el-icon> 重试
        </el-button>
        <el-button round @click="$router.push('/')">返回首页</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  kind: { type: String, default: 'error' },   // error / warn / empty
  title: { type: String, default: '出错了' },
  message: { type: String, default: '请求失败，请稍后重试' },
  retry: { type: Boolean, default: true }
})
defineEmits(['retry'])
</script>

<style scoped>
.err-page { min-height: calc(100vh - 60px); display: flex; align-items: center; justify-content: center; padding: 40px 20px; }
.err-box { text-align: center; max-width: 420px; }
.err-icon {
  width: 96px; height: 96px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 44px; font-weight: 700; color: #fff; margin: 0 auto 20px;
  animation: pulse 2s ease-in-out infinite;
}
.err-icon.error { background: linear-gradient(135deg, #ff6b6b, #e74c3c); box-shadow: 0 12px 40px rgba(231, 76, 60, 0.3); }
.err-icon.warn { background: linear-gradient(135deg, #fdcb6e, #e67e22); box-shadow: 0 12px 40px rgba(230, 126, 34, 0.3); }
.err-icon.empty { background: linear-gradient(135deg, #a29bfe, #6c5ce7); box-shadow: 0 12px 40px rgba(108, 92, 231, 0.3); }
@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}
.err-title { font-size: 22px; font-weight: 700; color: #2d3436; margin-bottom: 8px; }
.err-desc { font-size: 14px; color: #b2bec3; margin-bottom: 24px; line-height: 1.7; }
.err-actions { display: flex; gap: 12px; justify-content: center; }
</style>