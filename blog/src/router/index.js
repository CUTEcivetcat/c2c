import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', name: 'Home', component: () => import('@/views/HomeView.vue') },
  { path: '/article/:id', name: 'Article', component: () => import('@/views/ArticleView.vue') },
  { path: '/category/:name', name: 'Category', component: () => import('@/views/CategoryView.vue') }
]

const router = createRouter({
  history: createWebHistory('/blog/'),
  routes
})

export default router
