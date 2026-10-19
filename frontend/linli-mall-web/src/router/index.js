import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  { path: '/', name: 'home', component: () => import('@/views/HomeView.vue'), meta: { tab: true, title: '首页' } },
  { path: '/category', name: 'category', component: () => import('@/views/CategoryView.vue'), meta: { tab: true, title: '分类' } },
  { path: '/search', name: 'search', component: () => import('@/views/SearchView.vue'), meta: { title: '搜索' } },
  { path: '/products/:id', name: 'product-detail', component: () => import('@/views/ProductDetailView.vue'), meta: { title: '商品详情' } },
  { path: '/cart', name: 'cart', component: () => import('@/views/CartView.vue'), meta: { requiresAuth: true, tab: true, title: '购物车' } },
  { path: '/checkout', name: 'checkout', component: () => import('@/views/CheckoutView.vue'), meta: { requiresAuth: true, title: '确认订单' } },
  { path: '/payment/:orderNo', name: 'payment', component: () => import('@/views/PaymentView.vue'), meta: { requiresAuth: true, title: '订单支付' } },
  { path: '/orders', name: 'orders', component: () => import('@/views/OrdersView.vue'), meta: { requiresAuth: true, tab: true, title: '我的订单' } },
  { path: '/orders/:orderNo', name: 'order-detail', component: () => import('@/views/OrderDetailView.vue'), meta: { requiresAuth: true, title: '订单详情' } },
  { path: '/after-sales', name: 'after-sales', component: () => import('@/views/AfterSalesView.vue'), meta: { requiresAuth: true, title: '售后服务' } },
  { path: '/me', name: 'profile', component: () => import('@/views/ProfileView.vue'), meta: { requiresAuth: true, tab: true, title: '个人中心' } },
  { path: '/me/addresses', name: 'addresses', component: () => import('@/views/AddressesView.vue'), meta: { requiresAuth: true, title: '收货地址' } },
  { path: '/coupons', name: 'coupons', component: () => import('@/views/CouponsView.vue'), meta: { requiresAuth: true, title: '优惠券' } },
  { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue'), meta: { title: '登录' } },
  { path: '/register', name: 'register', component: () => import('@/views/RegisterView.vue'), meta: { title: '注册' } },
  { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('@/views/NotFoundView.vue'), meta: { title: '页面不存在' } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

router.beforeEach(async (to) => {
  const user = useUserStore()

  if (to.meta.requiresAuth && !user.token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (user.token && !user.profile) {
    try {
      await user.fetchProfile()
    } catch {
      user.reset()
      if (to.meta.requiresAuth) {
        return { name: 'login', query: { redirect: to.fullPath } }
      }
    }
  }

  if (to.name === 'login' && user.token) {
    return { name: 'home' }
  }

  return true
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · 邻里鲜生` : '邻里鲜生 · 社区超市'
})

export default router
