import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/dashboard' },
  { path: '/login', name: 'Login', component: () => import('@/views/LoginView.vue') },
  { path: '/dashboard', name: 'Dashboard', component: () => import('@/views/DashboardView.vue'), meta: { requiresAuth: true } },
  { path: '/users', name: 'Users', component: () => import('@/views/UserManageView.vue'), meta: { requiresAuth: true } },
  { path: '/products', name: 'Products', component: () => import('@/views/ProductManageView.vue'), meta: { requiresAuth: true } },
  { path: '/orders', name: 'Orders', component: () => import('@/views/OrderManageView.vue'), meta: { requiresAuth: true } },
  { path: '/permissions', name: 'Permissions', component: () => import('@/views/UserRoleManageView.vue'), meta: { requiresAuth: true } },
  { path: '/review', name: 'Review', component: () => import('@/views/ReviewManageView.vue'), meta: { requiresAuth: true } }
]

const router = createRouter({ history: createWebHashHistory(), routes })
router.beforeEach((to, from, next) => {
  if (to.meta.requiresAuth && !localStorage.getItem('admin_token')) next('/login')
  else next()
})
export default router
