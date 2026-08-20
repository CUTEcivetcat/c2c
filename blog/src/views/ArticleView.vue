<template>
  <div class="article-view fade-in-up" v-if="article">
    <button class="back-btn" type="button" @click="goBack">← 返回</button>
    <h1 class="article-title">{{ article.title }}</h1>
    <div class="article-meta">
      <span class="cat-tag">{{ article.category }}</span>
      <span>{{ article.date }}</span>
    </div>
    <div class="article-content" v-html="html"></div>
  </div>
  <div v-else class="not-found">
    <p>文章不存在或已被删除。</p>
    <router-link to="/">返回首页</router-link>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { marked } from 'marked'
import { articles } from '../data/articles'

const route = useRoute()
const router = useRouter()

const article = computed(() => articles.find(a => String(a.id) === route.params.id))
const html = computed(() => (article.value ? marked.parse(article.value.content) : ''))

const goBack = () => {
  if (window.history.length > 1) router.back()
  else router.push('/')
}
</script>

<style scoped>
.back-btn {
  border: none;
  background: none;
  color: #636e72;
  font-size: 14px;
  cursor: pointer;
  padding: 4px 0;
  margin-bottom: 12px;
  transition: color 0.2s;
}
.back-btn:hover { color: #ff6b35; }

.article-title { font-size: 26px; color: #2d3436; line-height: 1.4; }
.article-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 10px 0 22px;
  font-size: 13px;
  color: #b2bec3;
}
.cat-tag {
  font-size: 12px;
  color: #ff6b35;
  background: #fff5f0;
  padding: 2px 10px;
  border-radius: 10px;
  font-weight: 600;
}

.article-content {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 14px;
  padding: 28px 32px;
  line-height: 1.8;
  font-size: 15px;
  color: #3a3f44;
}
.article-content :deep(h1),
.article-content :deep(h2),
.article-content :deep(h3) {
  color: #2d3436;
  margin: 22px 0 10px;
  line-height: 1.4;
}
.article-content :deep(h1) { font-size: 22px; }
.article-content :deep(h2) { font-size: 19px; padding-bottom: 8px; border-bottom: 1px solid #f0f0f0; }
.article-content :deep(h3) { font-size: 16px; }
.article-content :deep(p) { margin: 10px 0; }
.article-content :deep(ul),
.article-content :deep(ol) { margin: 10px 0 10px 24px; }
.article-content :deep(li) { margin: 4px 0; }
.article-content :deep(blockquote) {
  margin: 14px 0;
  padding: 10px 16px;
  border-left: 4px solid #ff6b35;
  background: #fff5f0;
  color: #636e72;
  border-radius: 0 8px 8px 0;
}
.article-content :deep(code) {
  background: #f3f4f6;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
  font-family: "SF Mono", Consolas, monospace;
}
.article-content :deep(pre) {
  background: #2d3436;
  color: #e8eaed;
  padding: 16px;
  border-radius: 10px;
  overflow-x: auto;
  margin: 14px 0;
}
.article-content :deep(pre code) { background: none; padding: 0; color: inherit; }
.article-content :deep(hr) { border: none; border-top: 1px solid #f0f0f0; margin: 22px 0; }

.not-found { text-align: center; padding: 60px 0; color: #b2bec3; }
.not-found a { color: #ff6b35; }

@media (max-width: 520px) {
  .article-title { font-size: 21px; }
  .article-content { padding: 20px 18px; font-size: 14px; }
}
</style>
