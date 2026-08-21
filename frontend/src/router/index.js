import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/HomeView.vue')
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/RegisterView.vue')
  },
  {
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: () => import('@/views/ForgotPasswordView.vue')
  },
  {
    path: '/product/:id',
    name: 'ProductDetail',
    component: () => import('@/views/ProductDetailView.vue')
  },
  {
    path: '/publish',
    name: 'Publish',
    component: () => import('@/views/ProductPublishView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/search',
    name: 'Search',
    component: () => import('@/views/ProductSearchView.vue')
  },
  {
    path: '/order/list',
    name: 'OrderList',
    component: () => import('@/views/OrderListView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/order/checkout',
    name: 'OrderCheckout',
    component: () => import('@/views/OrderDetailView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/order/:id',
    name: 'OrderDetail',
    component: () => import('@/views/OrderDetailView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/chat',
    name: 'ChatList',
    component: () => import('@/views/ChatListView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/chat/:id',
    name: 'Chat',
    component: () => import('@/views/ChatView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/ProfileView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/user/:id',
    name: 'UserProfile',
    component: () => import('@/views/UserProfileView.vue')
  },
  {
    path: '/address',
    name: 'Address',
    component: () => import('@/views/AddressListView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/favorite',
    name: 'Favorite',
    component: () => import('@/views/FavoriteListView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/intent',
    name: 'Intent',
    component: () => import('@/views/IntentListView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/rating/:orderId',
    name: 'Rating',
    component: () => import('@/views/RatingView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/review',
    name: 'ReviewWorkbench',
    component: () => import('@/views/ReviewWorkbenchView.vue'),
    meta: { requiresAuth: true, reviewerOnly: true }
  },
  {
    path: '/my/report',
    name: 'MyReport',
    component: () => import('@/views/MyReportView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/my/appeal',
    name: 'MyAppeal',
    component: () => import('@/views/MyAppealView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/announcement',
    name: 'AnnouncementList',
    component: () => import('@/views/AnnouncementListView.vue')
  },
  {
    path: '/wallet',
    name: 'Wallet',
    component: () => import('@/views/WalletView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFoundView.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && !token) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
    return
  }
  // 审核工作台仅审核员（role=2）可进入
  if (to.meta.reviewerOnly) {
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || 'null')
    if (!userInfo || userInfo.role !== 2) {
      next({ name: 'Home' })
      return
    }
  }
  next()
})

export default router
