import { createRouter, createWebHashHistory } from 'vue-router'
import { ElMessage } from 'element-plus'

const routes = [
  { path: '/', redirect: '/dashboard' },
  { path: '/login', name: 'Login', component: () => import('@/views/LoginView.vue') },
  { path: '/dashboard', name: 'Dashboard', component: () => import('@/views/DashboardView.vue'), meta: { requiresAuth: true, perm: 'dashboard' } },
  { path: '/users', name: 'Users', component: () => import('@/views/UserManageView.vue'), meta: { requiresAuth: true, perm: 'users' } },
  // 安全设置：所有管理员基础能力（设置自己的二级密码），次级管理员管理部分页面内按总管理员判断
  { path: '/security', name: 'Security', component: () => import('@/views/AdminSecurityView.vue'), meta: { requiresAuth: true } },
  { path: '/products', name: 'Products', component: () => import('@/views/ProductManageView.vue'), meta: { requiresAuth: true, perm: 'products' } },
  { path: '/orders', name: 'Orders', component: () => import('@/views/OrderManageView.vue'), meta: { requiresAuth: true, perm: 'orders' } },
  { path: '/permissions', name: 'Permissions', component: () => import('@/views/UserRoleManageView.vue'), meta: { requiresAuth: true, requiresTop: true } },
  // 系统设置：运行时可调的系统开关（验证码回显、管理员登录锁定等），仅总管理员
  { path: '/config', name: 'SystemConfig', component: () => import('@/views/SystemConfigView.vue'), meta: { requiresAuth: true, requiresTop: true } },
  { path: '/review', name: 'Review', component: () => import('@/views/ReviewManageView.vue'), meta: { requiresAuth: true, perm: 'review' } },
  { path: '/announcement', name: 'Announcement', component: () => import('@/views/AnnouncementManageView.vue'), meta: { requiresAuth: true, perm: 'announcement' } },
  { path: '/banner', name: 'Banner', component: () => import('@/views/BannerManageView.vue'), meta: { requiresAuth: true, perm: 'banner' } }
]

const router = createRouter({ history: createWebHashHistory(), routes })

const getAdminUser = () => {
  try { return JSON.parse(localStorage.getItem('admin_user') || '{}') } catch { return {} }
}
// 总管理员判断：admin_level=2 或 username=admin（兜底，兼容漏执行迁移/旧后端）
const isTopAdmin = (u) => u.adminLevel === 2 || u.username === 'admin'

router.beforeEach((to, from, next) => {
  if (to.meta.requiresAuth && !localStorage.getItem('admin_token')) {
    return next('/login')
  }
  // 权限管理：仅总管理员（admin_level=2 或 username=admin）
  if (to.meta.requiresTop) {
    const u = getAdminUser()
    if (!isTopAdmin(u)) {
      ElMessage.error('仅总管理员可访问该模块')
      return next('/security')
    }
  }
  // 模块权限：次级/子级管理员需具备对应权限码（/security 恒可达，作兜底）
  if (to.meta.perm) {
    const u = getAdminUser()
    const ok = isTopAdmin(u) || (u.adminPerms || '').split(',').includes(to.meta.perm)
    if (!ok) {
      ElMessage.error('当前账号无该模块权限')
      return next('/security')
    }
  }
  next()
})
export default router
