<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog } from 'vant'
import { getOrders } from '@/api/order'
import { useUserStore } from '@/stores/user'
import { useCartStore } from '@/stores/cart'
import { maskPhone } from '@/utils/format'

const router = useRouter()
const user = useUserStore()
const cart = useCartStore()
const counts = ref({ PENDING_PAYMENT: 0, PENDING_SHIPMENT: 0, PENDING_RECEIPT: 0 })

const shortcuts = [
  { label: '待付款', status: 'PENDING_PAYMENT', icon: '💳' },
  { label: '待发货', status: 'PENDING_SHIPMENT', icon: '📦' },
  { label: '待收货', status: 'PENDING_RECEIPT', icon: '🚚' },
  { label: '全部订单', status: '', icon: '🧾' },
]

const menus = [
  { label: '个人资料', desc: '修改昵称与基本资料', icon: '👤', to: '/me/edit' },
  { label: '收货地址', desc: '管理常用配送地址', icon: '📍', to: '/me/addresses' },
  { label: '优惠券', desc: '领取与查看我的优惠券', icon: '🎟️', to: '/coupons' },
  { label: '售后服务', desc: '退款、退货与处理进度', icon: '🛠️', to: '/after-sales' },
]

async function logout() {
  await showConfirmDialog({ title: '退出登录', message: '确认退出当前账号吗？' })
  await user.logout()
  cart.items = []
  router.replace('/login')
}

onMounted(async () => {
  await user.fetchProfile().catch(() => {})
  const page = await getOrders({ pageNum: 1, pageSize: 100 }).catch(() => ({ list: [] }))
  counts.value = page.list.reduce(
    (acc, order) => {
      if (acc[order.status] !== undefined) acc[order.status] += 1
      return acc
    },
    { PENDING_PAYMENT: 0, PENDING_SHIPMENT: 0, PENDING_RECEIPT: 0 },
  )
})
</script>

<template>
  <div class="page profile-page with-tabbar">
    <section class="profile-head">
      <div class="avatar">
        <img :src="user.profile?.avatar || '/images/avatar.svg'" alt="会员头像" />
      </div>
      <div class="profile-copy">
        <span>{{ user.profile?.memberLevel || '青芽会员' }}</span>
        <h1>{{ user.displayName }}</h1>
        <p>{{ maskPhone(user.profile?.phone) }} · 积分 {{ user.profile?.points || 0 }}</p>
      </div>
      <button class="setting-button" @click="router.push('/me/edit')">⚙</button>
    </section>

    <section class="member-card">
      <div>
        <span>邻里会员</span>
        <h2>新鲜生活，从每一天开始</h2>
        <p>本月已为你节省 ¥18.60</p>
      </div>
      <strong>🌱</strong>
    </section>

    <section class="order-shortcuts">
      <button
        v-for="item in shortcuts"
        :key="item.label"
        @click="router.push({ name: 'orders', query: item.status ? { status: item.status } : {} })"
      >
        <span>{{ item.icon }}</span>
        <b>{{ item.label }}</b>
        <em v-if="counts[item.status]">{{ counts[item.status] }}</em>
      </button>
    </section>

    <section class="menu-list">
      <button v-for="item in menus" :key="item.label" @click="router.push(item.to)">
        <span class="menu-icon">{{ item.icon }}</span>
        <div><b>{{ item.label }}</b><p>{{ item.desc }}</p></div>
        <em>›</em>
      </button>
    </section>

    <section class="profile-note">
      <b>关于邻里鲜生</b>
      <p>社区最后一公里生鲜即时零售，支持到家配送与到店自提。当前为课程实验演示环境。</p>
    </section>

    <button class="logout-button" @click="logout">退出登录</button>
  </div>
</template>

<style scoped>
.profile-page {
  padding-top: 18px;
}

.profile-head {
  display: flex;
  align-items: center;
  gap: 13px;
}

.avatar {
  width: 64px;
  height: 64px;
  overflow: hidden;
  border: 3px solid #fff;
  border-radius: 22px;
  box-shadow: 0 8px 20px rgba(30, 62, 44, 0.1);
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-copy {
  flex: 1;
}

.profile-copy span {
  padding: 3px 8px;
  border-radius: 999px;
  background: #eaf7ee;
  color: #1f8a4c;
  font-size: 10px;
  font-weight: 700;
}

.profile-copy h1 {
  margin: 7px 0 3px;
  font-size: 20px;
}

.profile-copy p {
  margin: 0;
  color: #8a948c;
  font-size: 11px;
}

.setting-button {
  width: 38px;
  height: 38px;
  border: 1px solid #e6e5dd;
  border-radius: 50%;
  background: #fff;
  color: #7b857e;
  font-size: 17px;
}

.member-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 20px;
  padding: 20px;
  border-radius: 22px;
  background: linear-gradient(135deg, #193c2a, #2f7a4c);
  color: #fff;
  box-shadow: 0 16px 34px rgba(25, 60, 42, 0.22);
}

.member-card span {
  font-size: 10px;
  letter-spacing: 0.1em;
  opacity: 0.72;
}

.member-card h2 {
  margin: 8px 0 5px;
  font-size: 17px;
}

.member-card p {
  margin: 0;
  font-size: 11px;
  opacity: 0.75;
}

.member-card strong {
  font-size: 46px;
}

.order-shortcuts {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  margin-top: 16px;
  padding: 15px 8px;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(30, 62, 44, 0.06);
}

.order-shortcuts button {
  position: relative;
  display: grid;
  justify-items: center;
  gap: 5px;
  border: 0;
  background: transparent;
}

.order-shortcuts span {
  font-size: 20px;
}

.order-shortcuts b {
  color: #56625a;
  font-size: 11px;
  font-weight: 500;
}

.order-shortcuts em {
  position: absolute;
  top: -5px;
  right: 10px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 999px;
  background: #ff8a3d;
  color: #fff;
  font-size: 9px;
  font-style: normal;
  line-height: 16px;
}

.menu-list {
  margin-top: 16px;
  padding: 4px 14px;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(30, 62, 44, 0.06);
}

.menu-list button {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 14px 0;
  border: 0;
  border-bottom: 1px solid #f2f1ec;
  background: transparent;
  text-align: left;
}

.menu-list button:last-child {
  border-bottom: 0;
}

.menu-icon {
  display: grid;
  width: 38px;
  height: 38px;
  border-radius: 13px;
  background: #f4f6f2;
  place-items: center;
  font-size: 18px;
}

.menu-list div {
  flex: 1;
}

.menu-list b {
  font-size: 13px;
}

.menu-list p {
  margin: 3px 0 0;
  color: #8a948c;
  font-size: 10px;
}

.menu-list em {
  color: #a8afa9;
  font-size: 20px;
  font-style: normal;
}

.profile-note {
  margin-top: 16px;
  padding: 15px;
  border-radius: 16px;
  background: #fffaf0;
  color: #7c6947;
}

.profile-note b {
  font-size: 12px;
}

.profile-note p {
  margin: 6px 0 0;
  font-size: 11px;
  line-height: 1.7;
}

.logout-button {
  width: 100%;
  margin-top: 18px;
  padding: 13px;
  border: 1px solid #e9e5dd;
  border-radius: 15px;
  background: #fff;
  color: #7b857e;
  font-size: 13px;
}
</style>
