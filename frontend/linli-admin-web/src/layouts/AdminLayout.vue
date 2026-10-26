<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowDown,
  Box,
  ChatDotRound,
  DataLine,
  Document,
  Fold,
  Goods,
  Grid,
  List,
  Lock,
  Setting,
  SwitchButton,
  Tickets,
  User,
  UserFilled,
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const collapsed = ref(false)

const menuItems = [
  { path: '/dashboard', label: '经营看板', icon: DataLine, perm: 'statistics:dashboard:view' },
  { path: '/products', label: '商品管理', icon: Goods, perm: 'product:goods:manage' },
  { path: '/categories', label: '分类管理', icon: Grid, perm: 'product:category:manage' },
  { path: '/inventory', label: '库存管理', icon: Box, perm: 'stock:info:view' },
  { path: '/orders', label: '订单管理', icon: List, perm: 'trade:order:query' },
  { path: '/after-sales', label: '售后管理', icon: ChatDotRound, perm: 'after:sale:audit' },
  { path: '/coupons', label: '优惠券管理', icon: Tickets, perm: 'marketing:coupon:manage' },
  { path: '/reviews', label: '评价管理', icon: ChatDotRound, perm: 'review:goods:reply' },
  { path: '/members', label: '会员管理', icon: User, perm: 'user:member:manage' },
  { path: '/roles', label: '角色权限', icon: Lock, perm: 'system:role:manage' },
  { path: '/configs', label: '系统配置', icon: Setting, perm: 'system:config:manage' },
  { path: '/logs', label: '日志审计', icon: Document, perm: 'system:log:audit' },
  { path: '/accounts', label: '账号管理', icon: UserFilled, perm: 'system:account:manage' },
]

const visibleMenu = computed(() => menuItems.filter((item) => user.hasPerm(item.perm)))
const pageTitle = computed(() => route.meta.title || '运营后台')
const today = new Intl.DateTimeFormat('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' }).format(new Date())

async function handleCommand(command) {
  if (command === 'logout') {
    await user.logout()
    router.replace('/login')
  }
}
</script>

<template>
  <el-container class="admin-layout">
    <el-aside :width="collapsed ? '76px' : '238px'" class="admin-aside">
      <div class="admin-logo">
        <div class="logo-mark">邻</div>
        <div v-if="!collapsed" class="logo-copy">
          <b>邻里鲜生</b>
          <span>OPERATIONS CENTER</span>
        </div>
      </div>
      <div v-if="!collapsed" class="aside-caption">社区生鲜 · 运营工作台</div>
      <el-menu
        :default-active="route.path"
        :collapse="collapsed"
        :collapse-transition="false"
        router
        class="admin-menu"
      >
        <el-menu-item v-for="item in visibleMenu" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <template #title>{{ item.label }}</template>
        </el-menu-item>
      </el-menu>
      <div v-if="!collapsed" class="aside-footer">
        <span>当前版本 v0.1.0</span>
        <span>接口契约 /api/v1</span>
      </div>
    </el-aside>

    <el-container>
      <el-header class="admin-header">
        <div class="header-left">
          <button class="collapse-button" @click="collapsed = !collapsed">
            <el-icon><Fold /></el-icon>
          </button>
          <div class="breadcrumb">
            <span>运营后台</span>
            <i>/</i>
            <b>{{ pageTitle }}</b>
          </div>
        </div>
        <div class="header-right">
          <span class="today">{{ today }}</span>
          <el-tag :type="user.isAdmin ? 'success' : 'info'" effect="plain" round>
            {{ user.profile?.roleName || '商家运营' }}
          </el-tag>
          <el-dropdown trigger="click" @command="handleCommand">
            <button class="user-chip">
              <span class="avatar">{{ user.profile?.avatarText || '运' }}</span>
              <span>{{ user.displayName }}</span>
              <el-icon><ArrowDown /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="admin-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.admin-layout {
  min-height: 100vh;
}

.admin-aside {
  position: sticky;
  top: 0;
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
  background: linear-gradient(180deg, #102e20 0%, #173f2b 55%, #1d4b32 100%);
  color: #fff;
  transition: width 0.2s ease;
}

.admin-logo {
  display: flex;
  align-items: center;
  gap: 11px;
  height: 74px;
  padding: 0 20px;
}

.logo-mark {
  display: grid;
  flex: 0 0 38px;
  width: 38px;
  height: 38px;
  border: 1px solid rgba(255, 255, 255, 0.45);
  border-radius: 13px;
  background: rgba(255, 255, 255, 0.09);
  place-items: center;
  font-size: 20px;
  font-weight: 800;
}

.logo-copy {
  display: grid;
  gap: 3px;
}

.logo-copy b {
  font-size: 17px;
  letter-spacing: 0.04em;
}

.logo-copy span {
  color: rgba(255, 255, 255, 0.45);
  font-size: 8px;
  letter-spacing: 0.18em;
}

.aside-caption {
  padding: 0 21px 13px;
  color: rgba(255, 255, 255, 0.42);
  font-size: 10px;
}

.admin-menu {
  flex: 1;
  border-right: 0;
  background: transparent;
  overflow-y: auto;
}

.admin-menu :deep(.el-menu-item) {
  height: 44px;
  margin: 3px 12px;
  border-radius: 12px;
  color: rgba(255, 255, 255, 0.66);
}

.admin-menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
}

.admin-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, #2f9c5b, #247e49);
  color: #fff;
  box-shadow: 0 8px 18px rgba(16, 46, 32, 0.3);
}

.aside-footer {
  display: grid;
  gap: 4px;
  padding: 16px 21px 20px;
  color: rgba(255, 255, 255, 0.35);
  font-size: 9px;
}

.admin-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 68px;
  padding: 0 24px;
  border-bottom: 1px solid #e8ede8;
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(14px);
}

.header-left,
.header-right,
.user-chip {
  display: flex;
  align-items: center;
}

.header-left {
  gap: 16px;
}

.header-right {
  gap: 15px;
}

.collapse-button {
  display: grid;
  width: 34px;
  height: 34px;
  border: 1px solid #e4eae4;
  border-radius: 10px;
  background: #fff;
  color: #5d6d62;
  place-items: center;
  cursor: pointer;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #8a958d;
  font-size: 12px;
}

.breadcrumb i {
  color: #c2cbc3;
  font-style: normal;
}

.breadcrumb b {
  color: #1b2a22;
}

.today {
  color: #7b887f;
  font-size: 12px;
}

.user-chip {
  gap: 8px;
  padding: 5px 10px 5px 6px;
  border: 1px solid #e5ebe5;
  border-radius: 999px;
  background: #fff;
  color: #33443a;
  font-size: 12px;
  cursor: pointer;
}

.avatar {
  display: grid;
  width: 27px;
  height: 27px;
  border-radius: 50%;
  background: #e7f4eb;
  color: #1f8a4c;
  place-items: center;
  font-size: 11px;
  font-weight: 700;
}

.admin-main {
  padding: 0;
  background: transparent;
}

@media (max-width: 900px) {
  .admin-aside {
    width: 76px !important;
  }

  .logo-copy,
  .aside-caption,
  .aside-footer {
    display: none;
  }

  .today {
    display: none;
  }
}
</style>
