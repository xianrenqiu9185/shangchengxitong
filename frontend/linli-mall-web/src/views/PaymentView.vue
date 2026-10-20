<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showSuccessToast, showToast } from 'vant'
import { getOrder, getPayment, payOrder } from '@/api/order'
import { formatPrice } from '@/utils/format'
import { useSubmit } from '@/composables/useSubmit'
import AppHeader from '@/components/AppHeader.vue'

const route = useRoute()
const router = useRouter()
const order = ref(null)
const method = ref('MOCK')
const paid = ref(false)
const payNo = ref('')
const polling = ref(false)
let pollCancelled = false

async function waitForPayment(no) {
  polling.value = true
  try {
    for (let attempt = 0; attempt < 30; attempt += 1) {
      if (pollCancelled) return
      await new Promise((resolve) => setTimeout(resolve, 1000))
      const result = await getPayment(no)
      if (result.status === 'PAID') {
        paid.value = true
        showSuccessToast('支付成功')
        return
      }
      if (['FAILED', 'CLOSED'].includes(result.status)) {
        showToast('支付未完成，请稍后重试')
        return
      }
    }
    showToast('支付结果确认超时，请稍后在订单详情查看')
  } finally {
    polling.value = false
  }
}

async function pay() {
  const result = await payOrder({ orderNo: order.value.orderNo, method: method.value })
  payNo.value = result.payNo
  if (result.status === 'PAID') {
    paid.value = true
    showSuccessToast('支付成功')
  } else {
    await waitForPayment(result.payNo)
  }
}

const { submitting, run } = useSubmit(pay)

onMounted(async () => {
  order.value = await getOrder(route.params.orderNo)
  if (order.value.status === 'CANCELLED') {
    showToast('订单已关闭，无法支付')
    router.replace(`/orders/${order.value.orderNo}`)
  } else if (order.value.status !== 'PENDING_PAYMENT') {
    paid.value = true
    payNo.value = order.value.orderNo
  }
})

onBeforeUnmount(() => {
  pollCancelled = true
})
</script>

<template>
  <div class="page payment-page">
    <AppHeader title="订单支付" />

    <template v-if="paid">
      <section class="success-card">
        <div class="success-icon">✓</div>
        <h1>支付成功</h1>
        <p>订单已进入待发货，我们会尽快为你备货。</p>
        <span>支付单号 {{ payNo }}</span>
        <button @click="router.replace(`/orders/${order.orderNo}`)">查看订单</button>
        <button class="ghost" @click="router.replace('/orders')">返回订单列表</button>
      </section>
    </template>

    <template v-else-if="order">
      <section class="amount-card">
        <span>待支付金额</span>
        <strong><small>¥</small>{{ formatPrice(order.payAmount) }}</strong>
        <p>订单号 {{ order.orderNo }}</p>
        <em>请在 30 分钟内完成支付，超时将自动关闭订单</em>
      </section>

      <section v-if="polling" class="poll-card">
        <span class="poll-dot" />
        <div>
          <b>正在确认支付结果</b>
          <p>后端回调确认中，请不要关闭页面。</p>
        </div>
      </section>

      <section class="method-card">
        <h2>选择支付方式</h2>
        <van-radio-group v-model="method">
          <van-cell-group inset>
            <van-cell title="沙箱模拟支付" label="课程演示使用，不产生真实扣款" clickable @click="method = 'MOCK'">
              <template #icon><span class="method-icon">🧪</span></template>
              <template #right-icon><van-radio name="MOCK" /></template>
            </van-cell>
            <van-cell title="微信支付（沙箱）" label="模拟微信支付流程" clickable @click="method = 'WECHAT'">
              <template #icon><span class="method-icon wechat">微</span></template>
              <template #right-icon><van-radio name="WECHAT" /></template>
            </van-cell>
            <van-cell title="支付宝（沙箱）" label="模拟支付宝支付流程" clickable @click="method = 'ALIPAY'">
              <template #icon><span class="method-icon alipay">支</span></template>
              <template #right-icon><van-radio name="ALIPAY" /></template>
            </van-cell>
          </van-cell-group>
        </van-radio-group>
      </section>

      <section class="security-note">
        <b>安全说明</b>
        <p>本页面为支付沙箱，不会进行真实资金交易。支付结果由后端回调确认，前端不直接修改订单状态。</p>
      </section>

      <div class="pay-bar">
        <span>应付 <b>¥{{ formatPrice(order.payAmount) }}</b></span>
        <button :disabled="submitting || polling" @click="run">{{ submitting || polling ? '确认中…' : '确认支付' }}</button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.payment-page {
  padding-bottom: 96px;
}

.amount-card {
  display: grid;
  justify-items: center;
  padding: 30px 18px 24px;
  border-radius: 24px;
  background: linear-gradient(145deg, #1f8a4c, #3eaa68);
  color: #fff;
  box-shadow: 0 18px 38px rgba(31, 138, 76, 0.22);
}

.amount-card > span {
  font-size: 12px;
  opacity: 0.78;
}

.amount-card strong {
  margin: 8px 0 4px;
  font-size: 38px;
}

.amount-card strong small {
  font-size: 18px;
}

.amount-card p {
  margin: 4px 0 12px;
  font-size: 11px;
  opacity: 0.8;
}

.amount-card em {
  padding: 6px 11px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
  font-size: 10px;
  font-style: normal;
}

.method-card {
  margin-top: 16px;
  padding: 16px;
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(30, 62, 44, 0.06);
}

.method-card h2 {
  margin: 0 0 12px;
  font-size: 16px;
}

.method-card :deep(.van-cell-group--inset) {
  margin: 0;
}

.method-icon {
  display: grid;
  width: 34px;
  height: 34px;
  margin-right: 10px;
  border-radius: 11px;
  background: #eaf7ee;
  place-items: center;
  font-size: 16px;
}

.method-icon.wechat {
  background: #e8f8ee;
  color: #07c160;
  font-weight: 700;
}

.method-icon.alipay {
  background: #e8f3ff;
  color: #1677ff;
  font-weight: 700;
}

.security-note {
  margin-top: 14px;
  padding: 15px;
  border-radius: 16px;
  background: #fffaf0;
  color: #8a6d3b;
}

.poll-card {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 14px;
  padding: 14px 16px;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(30, 62, 44, 0.06);
}

.poll-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #1f8a4c;
  box-shadow: 0 0 0 6px rgba(31, 138, 76, 0.12);
  animation: pulse 1.2s infinite;
}

.poll-card div {
  display: grid;
  gap: 3px;
}

.poll-card b {
  font-size: 12px;
}

.poll-card p {
  margin: 0;
  color: #8a948c;
  font-size: 10px;
}

@keyframes pulse {
  50% {
    opacity: 0.35;
  }
}

.security-note b {
  font-size: 12px;
}

.security-note p {
  margin: 6px 0 0;
  font-size: 11px;
  line-height: 1.7;
}

.pay-bar {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  display: flex;
  align-items: center;
  gap: 14px;
  max-width: 480px;
  margin: 0 auto;
  padding: 12px 16px calc(12px + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 -8px 24px rgba(30, 62, 44, 0.06);
}

.pay-bar > span {
  margin-left: auto;
  color: #7b857e;
  font-size: 12px;
}

.pay-bar b {
  color: #e55039;
  font-size: 20px;
}

.pay-bar button {
  padding: 12px 24px;
  border: 0;
  border-radius: 999px;
  background: #1f8a4c;
  color: #fff;
  font-size: 14px;
  font-weight: 700;
}

.success-card {
  display: grid;
  justify-items: center;
  padding: 34px 22px;
  border-radius: 24px;
  background: #fff;
  text-align: center;
  box-shadow: 0 8px 24px rgba(30, 62, 44, 0.06);
}

.success-icon {
  display: grid;
  width: 68px;
  height: 68px;
  border-radius: 50%;
  background: #eaf7ee;
  color: #1f8a4c;
  place-items: center;
  font-size: 34px;
  font-weight: 800;
}

.success-card h1 {
  margin: 16px 0 6px;
  font-size: 22px;
}

.success-card p {
  margin: 0;
  color: #7b857e;
  font-size: 13px;
}

.success-card span {
  margin: 12px 0 20px;
  color: #9aa29c;
  font-size: 10px;
}

.success-card button {
  width: 100%;
  padding: 12px;
  border: 0;
  border-radius: 14px;
  background: #1f8a4c;
  color: #fff;
  font-size: 14px;
  font-weight: 700;
}

.success-card button.ghost {
  margin-top: 9px;
  background: #f2f6f2;
  color: #56625a;
}
</style>
