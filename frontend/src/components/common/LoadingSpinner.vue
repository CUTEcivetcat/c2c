<template>
  <div class="spinner-wrap" :class="size">
    <div class="spinner-ring"></div>
    <div class="spinner-ring ring2"></div>
    <div class="spinner-ring ring3"></div>
    <span class="spinner-text" v-if="text">{{ text }}</span>
  </div>
</template>

<script setup>
defineProps({
  size: { type: String, default: 'md' },   // sm / md / lg
  text: { type: String, default: '' }
})
</script>

<style scoped>
.spinner-wrap { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; padding: 40px 0; }
.spinner-ring {
  position: absolute; border-radius: 50%;
  border: 3px solid transparent;
  border-top-color: #ff6b35;
  animation: spin 1s linear infinite;
}
.spinner-wrap.sm .spinner-ring { width: 28px; height: 28px; }
.spinner-wrap.md .spinner-ring { width: 44px; height: 44px; }
.spinner-wrap.lg .spinner-ring { width: 64px; height: 64px; }
.spinner-wrap .ring2 { border-top-color: #ff8c5a; animation-duration: 1.4s; animation-direction: reverse; }
.spinner-wrap .ring3 { border-top-color: #ffd9c4; animation-duration: 0.8s; }
.spinner-wrap.sm { position: relative; width: 28px; height: 28px; padding: 0; }
.spinner-wrap.md { position: relative; width: 44px; height: 44px; padding: 0; }
.spinner-wrap.lg { position: relative; width: 64px; height: 64px; padding: 0; }
.spinner-text { font-size: 13px; color: #b2bec3; margin-top: 70px; position: absolute; }
@keyframes spin { to { transform: rotate(360deg); } }
</style>