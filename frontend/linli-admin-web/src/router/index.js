import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { public: true, title: '登录' },
  },
  {
    path: '/',
    component: () => import('@/layouts/AdminLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'dashboard', component: () => import('@/views/DashboardView.vue'), meta: { title: '经营看板', perm: 'statistics:dashboard:view' } },
      { path: 'products', name: 'products', component: () => import('@/views/ProductsView.vue'), meta: { title: '商品管理', perm: 'product:goods:manage' } },
      { path: 'categories', name: 'categories', component: () => import('@/views/ResourceListView.vue'), meta: { title: '分类管理', perm: 'product:category:manage', resource: 'categories' } },
      { path: 'inventory', name: 'inventory', component: () => import('@/views/ResourceListView.vue'), meta: { title: '库存管理', perm: 'stock:info:view', resource: 'inventory' } },
      { path: 'orders', name: 'orders', component: () => import('@/views/OrdersView.vue'), meta: { title: '订单管理', perm: 'trade:order:query' } },
      { path: 'after-sales', name: 'after-sales', component: () => import('@/views/AfterSalesView.vue'), meta: { title: '售后管理', perm: 'after:sale:audit' } },
      { path: 'coupons', name: 'coupons', component: () => import('@/views/ResourceListView.vue'), meta: { title: '优惠券管理', perm: 'marketing:coupon:manage', resource: 'coupons' } },
      { path: 'reviews', name: 'reviews', component: () => import('@/views/ResourceListView.vue'), meta: { title: '评价管理', perm: 'review:goods:reply', resource: 'reviews' } },
      { path: 'members', name: 'members', component: () => import('@/views/ResourceListView.vue'), meta: { title: '会员管理', perm: 'user:member:manage', resource: 'members' } },
      { path: 'roles', name: 'roles', component: () => import('@/views/ResourceListView.vue'), meta: { title: '角色权限', perm: 'system:role:manage', resource: 'roles' } },
      { path: 'configs', name: 'configs', component: () => import('@/views/ResourceListView.vue'), meta: { title: '系统配置', perm: 'system:config:manage', resource: 'configs' } },
      { path: 'logs', name: 'logs', component: () => import('@/views/ResourceListView.vue'), meta: { title: '日志审计', perm: 'system:log:audit', resource: 'logs' } },
      { path: 'accounts', name: 'accounts', component: () => import('@/views/AccountsView.vue'), meta: { title: '账号管理', perm: 'system:account:manage' } },
    ],
  },
  { path: '/403', name: 'forbidden', component: () => import('@/views/ForbiddenView.vue'), meta: { public: true, title: '无操作权限' } },
  { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('@/views/NotFoundView.vue'), meta: { public: true, title: '页面不存在' } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

router.beforeEach(async (to) => {
  const user = useUserStore()

  if (to.meta.public) {
    if (to.name === 'login' && user.token) return { name: 'dashboard' }
    return true
  }

  if (!user.token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (!user.profile) {
    try {
      await user.fetchProfile()
    } catch {
      user.reset()
      return { name: 'login', query: { redirect: to.fullPath } }
    }
  }

  if (to.meta.perm && !user.hasPerm(to.meta.perm)) {
    return { name: 'forbidden' }
  }

  return true
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · 邻里鲜生运营后台` : '邻里鲜生 · 运营后台'
})

export default router
