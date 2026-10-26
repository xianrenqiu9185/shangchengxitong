<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import { cancelOrder, confirmOrder, getOrder } from '@/api/order'
import { deliveryTypes, orderStatusMap } from '@/utils/dict'
import { formatDate, formatPrice } from '@/utils/format'
import AppHeader from '@/components/AppHeader.vue'

const route = useRoute()
const router = useRouter()
const order = ref(null)
const status = computed(() => orderStatusMap[order.value?.status] || { label: '', color: '#6b7280' })

async function load() {
  order.value = await getOrder(route.params.orderNo)
}

async function cancel() {
  await showConfirmDialog({ title: '取消订单', message: '取消后将释放锁定库存。' })
  await cancelOrder(order.value.orderNo)
  showToast('订单已取消')
  await load()
}

async function confirm() {
  await showConfirmDialog({ title: '确认收货', message: '确认已收到全部商品吗？' })
  await confirmOrder(order.value.orderNo)
  showToast('已确认收货')
  await load()
}

onMounted(load)
</script>

<template>
  <div class="page order-detail-page">
    <AppHeader title="订单详情" />
    <template v-if="order">
      <section class="status-card" :style="{ background: `linear-gradient(135deg, ${status.color}, ${status.color}cc)` }">
        <span>{{ status.label }}</span>
        <h1>{{ order.status === 'PENDING_PAYMENT' ? '请尽快完成支付' : order.status === 'PENDING_RECEIPT' ? '商品正在配送中' : order.status === 'COMPLETED' ? '订单已完成' : '订单处理中' }}</h1>
        <p>订单号 {{ order.orderNo }}</p>
      </section>

      <section v-if="order.pickupCode" class="pickup-card">
        <div>
          <span>到店自提核销码</span>
          <strong>{{ order.pickupCode }}</strong>
        </div>
        <p>请到邻里鲜生超市前台出示核销码</p>
      </section>

      <section class="detail-card">
        <h2>收货信息</h2>
        <div class="address-line">
          <b>{{ order.address?.receiver }}</b>
          <span>{{ order.address?.phone }}</span>
          <em>{{ deliveryTypes[order.deliveryType] }}</em>
        </div>
        <p>{{ order.address?.province }}{{ order.address?.city }}{{ order.address?.district }}{{ order.address?.detail }}</p>
      </section>

      <section class="detail-card">
        <h2>商品清单</h2>
        <article v-for="item in order.items" :key="item.productId" class="item-row">
          <img :src="item.image" :alt="item.name" />
          <div><b>{{ item.name }}</b><span>{{ item.unit }} × {{ item.quantity }}</span></div>
          <strong>¥{{ formatPrice(item.price * item.quantity) }}</strong>
        </article>
      </section>

      <section class="detail-card amount-list">
        <div><span>商品金额</span><b>¥{{ formatPrice(order.goodsAmount) }}</b></div>
        <div><span>配送费</span><b>{{ order.deliveryFee ? `¥${formatPrice(order.deliveryFee)}` : '免配送费' }}</b></div>
        <div><span>优惠</span><b class="discount">-¥{{ formatPrice(order.discountAmount) }}</b></div>
        <div class="total"><span>实付</span><strong>¥{{ formatPrice(order.payAmount) }}</strong></div>
      </section>

      <section class="detail-card">
        <h2>订单信息</h2>
        <div class="info-row"><span>下单时间</span><b>{{ formatDate(order.createdAt) }}</b></div>
        <div class="info-row"><span>履约方式</span><b>{{ deliveryTypes[order.deliveryType] }}</b></div>
        <div v-if="order.remark" class="info-row"><span>订单备注</span><b>{{ order.remark }}</b></div>
      </section>

      <div class="order-footer-actions">
        <van-button v-if="['PENDING_PAYMENT', 'PENDING_SHIPMENT'].includes(order.status)" round plain @click="cancel">
          取消订单
        </van-button>
        <van-button v-if="order.status === 'PENDING_PAYMENT'" round type="primary" @click="router.push(`/payment/${order.orderNo}`)">
          去支付
        </van-button>
        <van-button v-if="order.status === 'PENDING_RECEIPT'" round type="primary" @click="confirm">确认收货</van-button>
        <van-button
          v-if="order.status === 'COMPLETED'"
          round
          type="primary"
          @click="router.push({ name: 'review-create', query: { orderNo: order.orderNo } })"
        >
          评价商品
        </van-button>
        <van-button v-if="order.status === 'COMPLETED'" round plain @click="router.push('/after-sales')">申请售后</van-button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.order-detail-page {
  padding-bottom: 88px;
}

.status-card {
  padding: 24px 18px;
  border-radius: 22px;
  color: #fff;
  box-shadow: 0 16px 34px rgba(30, 62, 44, 0.16);
}

.status-card span {
  font-size: 11px;
  opacity: 0.8;
}

.status-card h1 {
  margin: 7px 0;
  font-size: 21px;
}

.status-card p {
  margin: 0;
  font-size: 11px;
  opacity: 0.8;
}

.pickup-card {
  display: grid;
  justify-items: center;
  margin-top: 14px;
  padding: 18px;
  border: 1px dashed #1f8a4c;
  border-radius: 18px;
  background: #f2fbf4;
  text-align: center;
}

.pickup-card span {
  color: #5d6a61;
  font-size: 11px;
}

.pickup-card strong {
  display: block;
  margin-top: 5px;
  color: #1f8a4c;
  font-size: 32px;
  letter-spacing: 0.18em;
}

.pickup-card p {
  margin: 4px 0 0;
  color: #8a948c;
  font-size: 10px;
}

.detail-card {
  margin-top: 12px;
  padding: 16px;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(30, 62, 44, 0.06);
}

.detail-card h2 {
  margin: 0 0 12px;
  font-size: 15px;
}

.address-line {
  display: flex;
  align-items: center;
  gap: 9px;
  font-size: 13px;
}

.address-line span {
  color: #7b857e;
}

.address-line em {
  margin-left: auto;
  padding: 2px 7px;
  border-radius: 999px;
  background: #eaf7ee;
  color: #1f8a4c;
  font-size: 10px;
  font-style: normal;
}

.detail-card > p {
  margin: 8px 0 0;
  color: #5d6a61;
  font-size: 12px;
  line-height: 1.6;
}

.item-row {
  display: grid;
  grid-template-columns: 58px 1fr auto;
  gap: 10px;
  align-items: center;
  padding: 7px 0;
}

.item-row img {
  width: 58px;
  height: 58px;
  border-radius: 12px;
  object-fit: cover;
}

.item-row div {
  display: grid;
  gap: 4px;
}

.item-row b {
  font-size: 13px;
}

.item-row span {
  color: #8a948c;
  font-size: 11px;
}

.item-row strong {
  font-size: 13px;
}

.amount-list {
  display: grid;
  gap: 9px;
}

.amount-list > div {
  display: flex;
  justify-content: space-between;
  color: #7b857e;
  font-size: 12px;
}

.amount-list b {
  color: #1c2420;
  font-weight: 500;
}

.amount-list .discount {
  color: #e55039;
}

.amount-list .total {
  padding-top: 9px;
  border-top: 1px solid #f0efe9;
  color: #1c2420;
}

.amount-list .total strong {
  color: #e55039;
  font-size: 20px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 5px 0;
  color: #7b857e;
  font-size: 12px;
}

.info-row b {
  color: #1c2420;
  font-weight: 500;
  text-align: right;
}

.order-footer-actions {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 10;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  max-width: 480px;
  margin: 0 auto;
  padding: 12px 16px calc(12px + env(safe-area-inset-bottom));
  background: rgba(246, 244, 238, 0.96);
  backdrop-filter: blur(12px);
}
</style>
