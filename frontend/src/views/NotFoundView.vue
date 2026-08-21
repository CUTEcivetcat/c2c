<template>
  <div class="nf-page">
    <div class="nf-bg">
      <span v-for="i in 12" :key="i" class="nf-dot" :style="{ left: (i * 8) + '%', animationDelay: (i * 0.3) + 's' }"></span>
    </div>
    <div class="nf-content">
      <div class="nf-404">
        <span class="nf-digit">4</span>
        <span class="nf-zero">
          <span class="zero-inner">🔄</span>
        </span>
        <span class="nf-digit">4</span>
      </div>
      <h1 class="nf-title">页面走丢了</h1>
      <p class="nf-desc">你要找的页面可能已被移除、改名或暂时不可用</p>
      <div class="nf-actions">
        <el-button type="primary" round size="large" @click="$router.push('/')">
          <el-icon style="margin-right:6px"><House /></el-icon> 返回首页
        </el-button>
        <el-button round size="large" @click="$router.back()">返回上一页</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.nf-page {
  min-height: calc(100vh - 60px);
  display: flex; align-items: center; justify-content: center;
  position: relative; overflow: hidden;
  background: linear-gradient(135deg, #fff5f0 0%, #f8f9fa 50%, #fff 100%);
}
.nf-bg { position: absolute; inset: 0; pointer-events: none; }
.nf-dot {
  position: absolute; bottom: 10%;
  width: 12px; height: 12px; border-radius: 50%;
  background: rgba(255, 107, 53, 0.25);
  animation: floatUp 6s ease-in infinite;
}
@keyframes floatUp {
  0% { transform: translateY(0) scale(1); opacity: 1; }
  100% { transform: translateY(-80vh) scale(0.5); opacity: 0; }
}
.nf-content { text-align: center; position: relative; z-index: 1; padding: 20px; }
.nf-404 { display: flex; align-items: center; justify-content: center; gap: 8px; margin-bottom: 8px; }
.nf-digit {
  font-size: 120px; font-weight: 900; line-height: 1;
  background: linear-gradient(135deg, #ff6b35, #ff8c5a);
  -webkit-background-clip: text; background-clip: text;
  -webkit-text-fill-color: transparent;
  animation: bounce 2s ease-in-out infinite;
}
.nf-digit:last-child { animation-delay: 0.3s; }
.nf-zero {
  width: 110px; height: 110px; border-radius: 50%;
  background: linear-gradient(135deg, #ff6b35, #e55a2b);
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 12px 40px rgba(255, 107, 53, 0.35);
  animation: spin 8s linear infinite;
}
.zero-inner { font-size: 40px; }
@keyframes bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-12px); }
}
@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
.nf-title { font-size: 28px; font-weight: 800; color: #2d3436; margin: 16px 0 8px; }
.nf-desc { font-size: 14px; color: #b2bec3; margin-bottom: 28px; }
.nf-actions { display: flex; gap: 12px; justify-content: center; }
@media (max-width: 480px) {
  .nf-digit { font-size: 80px; }
  .nf-zero { width: 76px; height: 76px; }
  .zero-inner { font-size: 28px; }
}
</style>