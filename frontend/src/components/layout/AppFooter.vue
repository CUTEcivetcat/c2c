<template>
  <footer class="app-footer">
    <div class="footer-inner">
      <div class="footer-brand">
        <span class="footer-logo">🔄 闲小鱼</span>
        <span class="footer-desc">C2C 二手交易平台</span>
      </div>
      <div class="footer-links">
        <span>Vue 3</span><span class="dot">·</span>
        <span>Spring Boot</span><span class="dot">·</span>
        <span>MySQL</span><span class="dot">·</span>
        <span>Redis</span>
      </div>
      <div class="footer-nav">
        <router-link to="/announcement" class="footer-nav-link">📢 平台公告</router-link>
      </div>
      <p class="footer-copy">© 2026 C2C Platform. All rights reserved.</p>
    </div>
  </footer>
</template>

<style scoped>
.app-footer {
  border-top: 1px solid #e8ecf1;
  background: #fff;
  padding: 28px 20px;
  margin-top: auto;
}
.footer-inner {
  max-width: 1200px; margin: 0 auto;
  display: flex; flex-direction: column; align-items: center; gap: 8px;
}
.footer-brand { display: flex; align-items: center; gap: 10px; }
.footer-logo { font-size: 16px; font-weight: 700; color: #2d3436; }
.footer-desc { font-size: 12px; color: #ff6b35; background: #fff5f0; padding: 2px 10px; border-radius: 8px; }
.footer-links { font-size: 12px; color: #b2bec3; display: flex; gap: 6px; align-items: center; }
.footer-links .dot { font-size: 16px; }
.footer-nav { display: flex; gap: 16px; }
.footer-nav-link { font-size: 12px; color: #b2bec3; text-decoration: none; transition: color 0.2s; }
.footer-nav-link:hover { color: #ff6b35; }
.footer-copy { font-size: 11px; color: #b2bec3; }
</style>
