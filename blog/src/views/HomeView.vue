<template>
  <div class="home-view">
    <div class="blog-hero">
      <h1>闲小鱼博客</h1>
      <p>记录技术学习与生活随笔</p>
    </div>

    <article
      v-for="a in sorted"
      :key="a.id"
      class="article-card fade-in-up"
      @click="$router.push(`/article/${a.id}`)"
    >
      <div class="card-top">
        <span class="cat-tag">{{ a.category }}</span>
        <span class="card-date">{{ a.date }}</span>
      </div>
      <h2 class="card-title">{{ a.title }}</h2>
      <p class="card-summary">{{ a.summary }}</p>
      <span class="card-more">阅读全文 →</span>
    </article>

    <p v-if="!sorted.length" class="empty-tip">还没有文章，敬请期待。</p>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { articles } from '../data/articles'

const sorted = computed(() => [...articles].sort((x, y) => y.id - x.id))
</script>

<style scoped>
.blog-hero {
  text-align: center;
  padding: 32px 0 28px;
}
.blog-hero h1 { font-size: 28px; color: #2d3436; }
.blog-hero p { margin-top: 8px; color: #b2bec3; font-size: 14px; }

.article-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 14px;
  padding: 20px 24px;
  margin-bottom: 14px;
  cursor: pointer;
  transition: all 0.25s;
}
.article-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  border-color: #ffe0d3;
}
.card-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.cat-tag {
  font-size: 12px;
  color: #ff6b35;
  background: #fff5f0;
  padding: 2px 10px;
  border-radius: 10px;
  font-weight: 600;
}
.card-date { font-size: 12px; color: #b2bec3; }
.card-title { font-size: 19px; color: #2d3436; margin-bottom: 6px; }
.card-summary {
  font-size: 14px;
  color: #636e72;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.card-more { display: inline-block; margin-top: 10px; font-size: 13px; color: #ff6b35; }
.empty-tip { text-align: center; color: #b2bec3; padding: 40px 0; }
@media (max-width: 520px) {
  .article-card { padding: 16px 16px; }
  .card-title { font-size: 17px; }
}
</style>
