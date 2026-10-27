<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { createOrder, previewOrder } from '@/api/order'
import { getAddresses } from '@/api/user'
import { getMyCoupons } from '@/api/marketing'
import { useCartStore } from '@/stores/cart'
import { useSubmit } from '@/composables/useSubmit'
import { formatPrice } from '@/utils/format'
import AppHeader from '@/components/AppHeader.vue'

const router = useRouter()
const cart = useCartStore()
const addresses = ref([])
const coupons = ref([])
const addressId = ref(null)
const deliveryType = ref('DELIVERY')
const couponId = ref(null)
const remark = ref('')
const preview = ref(null)
const showAddress = ref(false)
const showCoupon = ref(false)

const selectedAddress = () => addresses.value.find((item) => item.id === addressId.value)

const fulfillType = computed(() => (deliveryType.value === 'PICKUP' ? 2 : 1))
const orderItems = computed(() => cart.selectedItems.map((item) => ({
  productId: item.product.id,
  quantity: item.quantity,
})))

async function loadPreview() {
  preview.value = await previewOrder({
    fulfillType: fulfillType.value,
    couponId: couponId.value,
    items: orderItems.value,
  })
}

async function submitOrder() {
  if (!addressId.value) {
    showToast('请选择收货地址')
    return
  }
  const order = await createOrder({
    addressId: addressId.value,
    fulfillType: fulfillType.value,
    couponId: couponId.value,
    remark: remark.value,
    items: orderItems.value,
  })
  await cart.fetch()
  router.replace(`/payment/${order.orderNo}`)
}

const { submitting, run } = useSubmit(submitOrder)

watch([deliveryType, couponId], loadPreview)

onMounted(async () => {
  if (!cart.selectedItems.length) {
    await cart.fetch()
  }
  if (!cart.selectedItems.length) {
    showToast('请先选择结算商品')
    router.replace('/cart')
    return
  }
  const [addressList, couponList] = await Promise.all([getAddresses(), getMyCoupons()])
  addresses.value = addressList
  coupons.value = couponList
  addressId.value = addressList.find((item) => item.isDefault)?.id || addressList[0]?.id || null
  await loadPreview()
})
</script>

<template>
  <div class="page checkout-page">
    <AppHeader title="确认订单" />

    <section class="checkout-card address-card" @click="showAddress = true">
      <div v-if="selectedAddress()">
        <div class="address-line">
          <b>{{ selectedAddress().receiver }}</b>
          <span>{{ selectedAddress().phone }}</span>
          <em>{{ selectedAddress().tag }}</em>
        </div>
        <p>{{ selectedAddress().province }}{{ selectedAddress().city }}{{ selectedAddress().district }}{{ selectedAddress().detail }}</p>
      </div>
      <div v-else class="address-empty">请选择收货地址</div>
      <span class="chevron">›</span>
    </section>

    <section class="checkout-card">
      <h2>履约方式</h2>
      <van-radio-group v-model="deliveryType" direction="horizontal">
        <van-radio name="DELIVERY">到家配送</van-radio>
        <van-radio name="PICKUP">到店自提</van-radio>
      </van-radio-group>
      <p class="hint">{{ deliveryType === 'DELIVERY' ? '预计 30 分钟送达' : '到店后凭核销码取货' }}</p>
    </section>

    <section class="checkout-card items-card">
      <h2>商品清单</h2>
      <article v-for="item in cart.selectedItems" :key="item.id" class="checkout-item">
        <img :src="item.product.image" :alt="item.product.name" />
        <div>
          <b>{{ item.product.name }}</b>
          <span>{{ item.product.unit }} × {{ item.quantity }}</span>
        </div>
        <strong>¥{{ formatPrice(item.subtotal) }}</strong>
      </article>
    </section>

    <section class="checkout-card option-card" @click="showCoupon = true">
      <span>优惠券</span>
      <b>{{ coupons.find((item) => item.id === couponId)?.name || (coupons.length ? '选择优惠券' : '暂无可用') }}</b>
      <em>›</em>
    </section>

    <section class="checkout-card">
      <h2>订单备注</h2>
      <van-field v-model="remark" rows="2" autosize type="textarea" placeholder="选填，如：放门口、不要辣" />
    </section>

    <section v-if="preview" class="checkout-card summary-card">
      <div><span>商品金额</span><b>¥{{ formatPrice(preview.goodsAmount) }}</b></div>
      <div><span>配送费</span><b>{{ preview.deliveryFee ? `¥${formatPrice(preview.deliveryFee)}` : '免配送费' }}</b></div>
      <div><span>优惠</span><b class="discount">-¥{{ formatPrice(preview.discountAmount) }}</b></div>
      <div class="total"><span>应付</span><strong>¥{{ formatPrice(preview.payAmount) }}</strong></div>
    </section>

    <div class="submit-bar">
      <div>
        <span>应付</span>
        <strong>¥{{ preview ? formatPrice(preview.payAmount) : '0.00' }}</strong>
      </div>
      <button :disabled="submitting" @click="run">{{ submitting ? '提交中…' : '提交订单' }}</button>
    </div>

    <van-popup v-model:show="showAddress" position="bottom" round>
      <div class="popup-content">
        <h2>选择收货地址</h2>
        <article
          v-for="address in addresses"
          :key="address.id"
          class="address-option"
          :class="{ active: addressId === address.id }"
          @click="addressId = address.id; showAddress = false"
        >
          <div><b>{{ address.receiver }}</b><span>{{ address.phone }}</span></div>
          <p>{{ address.detail }}</p>
        </article>
        <button class="popup-action" @click="router.push('/me/addresses')">管理收货地址</button>
      </div>
    </van-popup>

    <van-popup v-model:show="showCoupon" position="bottom" round>
      <div class="popup-content">
        <h2>选择优惠券</h2>
        <article
          v-for="coupon in coupons"
          :key="coupon.id"
          class="coupon-option"
          :class="{ active: couponId === coupon.id, disabled: preview && preview.goodsAmount < coupon.threshold }"
          @click="preview && preview.goodsAmount >= coupon.threshold && (couponId = coupon.id, showCoupon = false)"
        >
          <strong>¥{{ formatPrice(coupon.amount) }}</strong>
          <div><b>{{ coupon.name }}</b><span>满 ¥{{ formatPrice(coupon.threshold) }} 可用</span></div>
        </article>
        <button class="popup-action" @click="couponId = null; showCoupon = false">不使用优惠券</button>
      </div>
    </van-popup>
  </div>
</template>

<style scoped>
.checkout-page {
  padding-bottom: 100px;
}

.checkout-card {
  position: relative;
  margin-bottom: 12px;
  padding: 16px;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(30, 62, 44, 0.06);
}

.address-card {
  min-height: 78px;
  padding-right: 36px;
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
  padding: 2px 7px;
  border-radius: 999px;
  background: #eaf7ee;
  color: #1f8a4c;
  font-size: 10px;
  font-style: normal;
}

.address-card p {
  margin: 8px 0 0;
  color: #56625a;
  font-size: 12px;
  line-height: 1.6;
}

.chevron {
  position: absolute;
  top: 50%;
  right: 16px;
  color: #a8afa9;
  font-size: 24px;
  transform: translateY(-50%);
}

.checkout-card h2 {
  margin: 0 0 12px;
  font-size: 16px;
}

.hint {
  margin: 10px 0 0;
  color: #8a948c;
  font-size: 11px;
}

.checkout-item {
  display: grid;
  grid-template-columns: 62px 1fr auto;
  gap: 10px;
  align-items: center;
  padding: 8px 0;
}

.checkout-item img {
  width: 62px;
  height: 62px;
  border-radius: 12px;
  object-fit: cover;
}

.checkout-item div {
  display: grid;
  gap: 4px;
}

.checkout-item b {
  font-size: 13px;
}

.checkout-item span {
  color: #8a948c;
  font-size: 11px;
}

.checkout-item strong {
  font-size: 13px;
}

.option-card {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
}

.option-card b {
  margin-left: auto;
  color: #1f8a4c;
  font-size: 12px;
}

.option-card em {
  color: #a8afa9;
  font-size: 20px;
  font-style: normal;
}

.summary-card {
  display: grid;
  gap: 10px;
}

.summary-card > div {
  display: flex;
  justify-content: space-between;
  color: #7b857e;
  font-size: 12px;
}

.summary-card b {
  color: #1c2420;
  font-weight: 500;
}

.summary-card .discount {
  color: #e55039;
}

.summary-card .total {
  padding-top: 10px;
  border-top: 1px solid #f0efe9;
  color: #1c2420;
}

.summary-card .total strong {
  color: #e55039;
  font-size: 21px;
}

.submit-bar {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  gap: 14px;
  max-width: 480px;
  margin: 0 auto;
  padding: 12px 16px calc(12px + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 -8px 24px rgba(30, 62, 44, 0.06);
}

.submit-bar > div {
  display: grid;
  margin-left: auto;
  text-align: right;
}

.submit-bar span {
  color: #8a948c;
  font-size: 10px;
}

.submit-bar strong {
  color: #e55039;
  font-size: 20px;
}

.submit-bar button {
  padding: 12px 24px;
  border: 0;
  border-radius: 999px;
  background: #1f8a4c;
  color: #fff;
  font-size: 14px;
  font-weight: 700;
}

.submit-bar button:disabled {
  opacity: 0.6;
}

.popup-content {
  max-height: 70vh;
  padding: 20px 16px calc(20px + env(safe-area-inset-bottom));
  overflow-y: auto;
}

.popup-content h2 {
  margin: 0 0 14px;
  font-size: 18px;
}

.address-option,
.coupon-option {
  margin-bottom: 9px;
  padding: 13px;
  border: 1px solid #ecebe4;
  border-radius: 14px;
}

.address-option.active,
.coupon-option.active {
  border-color: #1f8a4c;
  background: #f2fbf4;
}

.address-option div,
.coupon-option {
  display: flex;
  align-items: center;
  gap: 9px;
}

.address-option span {
  color: #7b857e;
  font-size: 12px;
}

.address-option p {
  margin: 7px 0 0;
  color: #56625a;
  font-size: 12px;
}

.coupon-option {
  align-items: center;
}

.coupon-option > strong {
  color: #e55039;
  font-size: 20px;
}

.coupon-option div {
  display: grid;
  gap: 3px;
}

.coupon-option b {
  font-size: 13px;
}

.coupon-option span {
  color: #8a948c;
  font-size: 11px;
}

.coupon-option.disabled {
  opacity: 0.45;
}

.popup-action {
  width: 100%;
  margin-top: 8px;
  padding: 13px;
  border: 0;
  border-radius: 14px;
  background: #eaf7ee;
  color: #1f8a4c;
  font-size: 13px;
  font-weight: 700;
}
</style>
