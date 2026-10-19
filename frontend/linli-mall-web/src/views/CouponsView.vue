<script setup>
import { onMounted, ref } from 'vue'
import { showToast } from 'vant'
import { getCoupons, getMyCoupons, receiveCoupon } from '@/api/marketing'
import { formatPrice } from '@/utils/format'
import AppHeader from '@/components/AppHeader.vue'

const active = ref(0)
const available = ref([])
const mine = ref([])

async function load() {
  const [list, myList] = await Promise.all([getCoupons(), getMyCoupons()])
  available.value = list
  mine.value = myList
}

async function receive(coupon) {
  await receiveCoupon(coupon.id)
  showToast('领取成功')
  await load()
}

onMounted(load)
</script>

<template>
  <div class="page coupon-page">
    <AppHeader title="优惠券" />
    <van-tabs v-model:active="active" color="#1f8a4c">
      <van-tab title="领券中心" />
      <van-tab title="我的优惠券" />
    </van-tabs>

    <div v-if="active === 0" class="coupon-list">
      <article v-for="coupon in available" :key="coupon.id" class="coupon-card">
        <div class="coupon-amount"><strong>¥{{ formatPrice(coupon.amount) }}</strong><span>满 ¥{{ formatPrice(coupon.threshold) }} 可用</span></div>
        <div class="coupon-info">
          <b>{{ coupon.name }}</b>
          <p>有效期至 {{ coupon.validUntil }}</p>
        </div>
        <button :disabled="coupon.received" @click="receive(coupon)">{{ coupon.received ? '已领取' : '立即领取' }}</button>
      </article>
    </div>
    <div v-else class="coupon-list">
      <article v-for="coupon in mine" :key="coupon.id" class="coupon-card mine">
        <div class="coupon-amount"><strong>¥{{ formatPrice(coupon.amount) }}</strong><span>满 ¥{{ formatPrice(coupon.threshold) }} 可用</span></div>
        <div class="coupon-info">
          <b>{{ coupon.name }}</b>
          <p>有效期至 {{ coupon.validUntil }}</p>
        </div>
        <button @click="$router.push('/')">去使用</button>
      </article>
      <div v-if="!mine.length" class="empty-block">
        <span class="emoji">🎟️</span>
        还没有领取优惠券
      </div>
    </div>
  </div>
</template>

<style scoped>
.coupon-page {
  padding-top: 2px;
}

.coupon-list {
  display: grid;
  gap: 12px;
  padding-top: 16px;
}

.coupon-card {
  display: grid;
  grid-template-columns: 100px 1fr auto;
  align-items: center;
  gap: 12px;
  padding: 15px;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(30, 62, 44, 0.06);
}

.coupon-card.mine {
  background: linear-gradient(135deg, #fffdf7, #fff4e4);
}

.coupon-amount {
  display: grid;
  gap: 4px;
  padding-right: 12px;
  border-right: 1px dashed #e6ddd0;
}

.coupon-amount strong {
  color: #e55039;
  font-size: 24px;
}

.coupon-amount span {
  color: #8a948c;
  font-size: 9px;
}

.coupon-info b {
  font-size: 13px;
}

.coupon-info p {
  margin: 5px 0 0;
  color: #8a948c;
  font-size: 10px;
}

.coupon-card button {
  padding: 7px 11px;
  border: 0;
  border-radius: 999px;
  background: #eaf7ee;
  color: #1f8a4c;
  font-size: 10px;
  font-weight: 700;
}

.coupon-card button:disabled {
  background: #f2f1ec;
  color: #9aa29c;
}
</style>
