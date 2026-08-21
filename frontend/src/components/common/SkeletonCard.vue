<template>
  <div class="skeleton-grid" :class="{ 'skeleton-list': list }">
    <div v-for="i in count" :key="i" class="sk-card">
      <div class="sk-img sk-animate"></div>
      <div class="sk-body">
        <div class="sk-line sk-animate" style="width: 80%"></div>
        <div class="sk-line sk-animate" style="width: 50%"></div>
        <div class="sk-line sk-animate" style="width: 30%"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  count: { type: Number, default: 8 },
  list: { type: Boolean, default: false }
})
</script>

<style scoped>
.skeleton-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.skeleton-list { grid-template-columns: 1fr; }
.sk-card { background: #fff; border-radius: 16px; overflow: hidden; border: 1px solid #f0f2f5; }
.sk-img { height: 200px; background: #f0f2f5; }
.sk-body { padding: 14px 16px; display: flex; flex-direction: column; gap: 10px; }
.sk-line { height: 14px; border-radius: 8px; background: #f0f2f5; }
.sk-animate {
  position: relative; overflow: hidden;
  background: linear-gradient(90deg, #f0f2f5 25%, #e4e7ed 37%, #f0f2f5 63%);
  background-size: 400% 100%;
  animation: shimmer 1.4s ease infinite;
}
@keyframes shimmer {
  0% { background-position: 100% 50%; }
  100% { background-position: 0 50%; }
}
@media (max-width: 900px) { .skeleton-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 480px) { .skeleton-grid { grid-template-columns: 1fr; } }
</style>