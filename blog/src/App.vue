<template>
  <div class="blog-app">
    <header class="blog-header">
      <div class="blog-header-inner">
        <router-link to="/" class="blog-logo">
          <span class="logo-mark">📝</span>
          <span>闲小鱼博客</span>
        </router-link>
        <nav class="blog-nav">
          <router-link to="/" class="nav-link" exact-active-class="active">首页</router-link>
          <router-link
            v-for="c in categories"
            :key="c"
            :to="`/category/${encodeURIComponent(c)}`"
            class="nav-link"
            active-class="active"
          >{{ c }}</router-link>
        </nav>
      </div>
    </header>

    <main class="blog-main">
      <router-view />
    </main>

    <footer class="blog-footer">© 2026 闲小鱼博客 · Vue 构建 · 部署于 /blog/</footer>
  </div>
</template>

<script setup>
import { categories } from './data/articles'
</script>

<style scoped>
.blog-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(16px);
  border-bottom: 1px solid #f0f0f0;
}
.blog-header-inner {
  max-width: 720px;
  margin: 0 auto;
  padding: 14px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.blog-logo {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 17px;
  font-weight: 700;
  color: #2d3436;
  text-decoration: none;
}
.logo-mark { font-size: 20px; }
.blog-nav { display: flex; gap: 4px; }
.nav-link {
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 14px;
  color: #636e72;
  text-decoration: none;
  transition: all 0.2s;
}
.nav-link:hover { color: #ff6b35; background: #fff5f0; }
.nav-link.active { color: #ff6b35; background: #fff5f0; font-weight: 600; }

.blog-main { max-width: 720px; margin: 0 auto; padding: 24px 20px 60px; }
.blog-footer {
  max-width: 720px;
  margin: 0 auto;
  padding: 20px;
  text-align: center;
  font-size: 12px;
  color: #b2bec3;
  border-top: 1px solid #f0f0f0;
}

@media (max-width: 520px) {
  .blog-header-inner { padding: 12px 14px; }
  .blog-logo { font-size: 15px; }
  .nav-link { padding: 5px 9px; font-size: 13px; }
  .blog-main { padding: 16px 14px 50px; }
}
</style>
