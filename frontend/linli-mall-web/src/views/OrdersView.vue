<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import { cancelOrder, confirmOrder, getOrders } from '@/api/order'
import { orderStatuses } from '@/utils/dict'
import AppHeader from '@/components/AppHeader.vue'
import OrderCard from '@/components/OrderCard.vue'

const router = useRouter()
const active = ref('')
const orders = ref([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const page = await getOrders({ status: active.value, pageNum: 1, pageSize: 30 })
    orders.value = page.list
  } finally {
    loading.value = false
  }
}

async function cancel(order) {
  await showConfirmDialog({ title: '取消订单', message: '取消后将释放锁定库存，确认继续吗？' })
  await cancelOrder(order.orderNo)
  showToast('订单已取消')
  await load()
}

async function confirm(order) {
  await showConfirmDialog({ title: '确认收货', message: '请确认已收到商品，确认后订单将完成。' })
  await confirmOrder(order.orderNo)
  showToast('已确认收货')
  await load()
}

onMounted(load)
</script>

<template>
  <div class="page orders-page with-tabbar">
    <AppHeader title="我的订单" :back="false" />
    <van-tabs v-model:active="active" sticky color="#1f8a4c" @change="load">
      <van-tab v-for="status in orderStatuses" :key="status.value" :name="status.value" :title="status.label" />
    </van-tabs>

    <div class="orders-list">
      <div v-if="loading" class="loading-list">
        <van-skeleton v-for="i in 3" :key="i" title :row="4" />
      </div>
      <template v-else-if="orders.length">
        <OrderCard v-for="order in orders" :key="order.orderNo" :order="order">
          <van-button
            v-if="order.status === 'PENDING_PAYMENT'"
            size="small"
            round
            plain
            type="default"
            @click="cancel(order)"
          >
            取消订单
          </van-button>
          <van-button
            v-if="order.status === 'PENDING_PAYMENT'"
            size="small"
            round
            type="primary"
            @click="router.push(`/payment/${order.orderNo}`)"
          >
            去支付
          </van-button>
          <van-button
            v-if="order.status === 'PENDING_SHIPMENT'"
            size="small"
            round
            plain
            type="default"
            @click="cancel(order)"
          >
            取消订单
          </van-button>
          <van-button
            v-if="order.status === 'PENDING_RECEIPT'"
            size="small"
            round
            type="primary"
            @click="confirm(order)"
          >
            确认收货
          </van-button>
          <van-button size="small" round plain type="default" @click="router.push(`/orders/${order.orderNo}`)">
            查看详情
          </van-button>
        </OrderCard>
      </template>
      <div v-else class="empty-block">
        <span class="emoji">🧾</span>
        这里还没有订单，去挑点新鲜的吧
      </div>
    </div>
  </div>
</template>

<style scoped>
.orders-page {
  padding-top: 2px;
}

.orders-page :deep(.van-tabs__nav) {
  background: transparent;
}

.orders-list {
  padding-top: 14px;
}

.loading-list {
  display: grid;
  gap: 12px;
}
</style>
