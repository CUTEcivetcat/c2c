<template>
  <div class="category-view fade-in-up">
    <div class="cat-head">
      <button class="back-btn" type="button" @click="goBack">← 返回</button>
      <h1 class="cat-title">分类：{{ name }}</h1>
      <span class="cat-count">{{ filtered.length }} 篇</span>
    </div>

    <article
      v-for="a in filtered"
      :key="a.id"
      class="article-card"
      @click="$router.push(`/article/${a.id}`)"
    >
      <div class="card-top">
        <span class="cat-tag">{{ a.category }}</span>
        <span class="card-date">{{ a.date }}</span>
      </div>
      <h2 class="card-title">{{ a.title }}</h2>
      <p class="card-summary">{{ a.summary }}</p>
    </article>

    <p v-if="!filtered.length" class="empty-tip">该分类下暂无文章。</p>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { articles } from '../data/articles'

const route = useRoute()
const router = useRouter()

const name = computed(() => decodeURIComponent(route.params.name || ''))
const filtered = computed(() =>
  articles.filter(a => a.category === name.value).sort((x, y) => y.id - x.id)
)
const goBack = () => {
  if (window.history.length > 1) router.back()
  else router.push('/')
}
</script>

<style scoped>
.cat-head { display: flex; align-items: baseline; gap: 12px; padding: 8px 0 18px; }
.back-btn {
  border: none; background: none; color: #636e72;
  font-size: 14px; cursor: pointer; padding: 0;
}
.back-btn:hover { color: #ff6b35; }
.cat-title { font-size: 22px; color: #2d3436; }
.cat-count { font-size: 13px; color: #b2bec3; }

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
}
.card-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.cat-tag {
  font-size: 12px; color: #ff6b35; background: #fff5f0;
  padding: 2px 10px; border-radius: 10px; font-weight: 600;
}
.card-date { font-size: 12px; color: #b2bec3; }
.card-title { font-size: 18px; color: #2d3436; margin-bottom: 6px; }
.card-summary {
  font-size: 14px; color: #636e72; line-height: 1.6;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}
.empty-tip { text-align: center; color: #b2bec3; padding: 40px 0; }
@media (max-width: 520px) {
  .article-card { padding: 16px; }
}
</style>
