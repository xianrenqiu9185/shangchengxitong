<script setup>
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import { useCartStore } from '@/stores/cart'
import { formatPrice } from '@/utils/format'
import AppHeader from '@/components/AppHeader.vue'

const router = useRouter()
const cart = useCartStore()
const allChecked = computed(() => cart.allChecked)

async function updateQuantity(item, value) {
  await cart.update(item.id, { quantity: Number(value) })
}

async function removeItem(item) {
  await showConfirmDialog({ title: '移除商品', message: `确认从购物车移除「${item.product.name}」吗？` })
  await cart.remove(item.id)
}

async function clear() {
  await showConfirmDialog({ title: '清空购物车', message: '确认清空全部购物车商品吗？' })
  await cart.clear()
}

function checkout() {
  if (!cart.selectedItems.length) {
    showToast('请先选择要结算的商品')
    return
  }
  router.push('/checkout')
}

onMounted(() => cart.fetch())
</script>

<template>
  <div class="page cart-page with-tabbar">
    <AppHeader title="购物车" :back="false">
      <template #right>
        <button v-if="cart.items.length" class="text-button" @click="clear">清空</button>
      </template>
    </AppHeader>

    <div v-if="cart.loading" class="loading-list">
      <van-skeleton v-for="i in 3" :key="i" title :row="2" />
    </div>

    <div v-else-if="!cart.items.length" class="empty-block cart-empty">
      <span class="emoji">🛒</span>
      <p>购物车还是空的</p>
      <button @click="router.push('/')">去逛逛</button>
    </div>

    <template v-else>
      <section class="cart-list">
        <article v-for="item in cart.items" :key="item.id" class="cart-item">
          <van-checkbox
            :model-value="item.checked"
            shape="round"
            @update:model-value="cart.update(item.id, { checked: $event })"
          />
          <img :src="item.product.image" :alt="item.product.name" />
          <div class="cart-info">
            <h3>{{ item.product.name }}</h3>
            <p>{{ item.product.unit }}</p>
            <div class="cart-price-row">
              <span class="price"><small>¥</small>{{ formatPrice(item.product.price) }}</span>
              <van-stepper
                :model-value="item.quantity"
                :max="item.product.stock"
                integer
                @change="updateQuantity(item, $event)"
              />
            </div>
          </div>
          <button class="remove" aria-label="删除" @click="removeItem(item)">×</button>
        </article>
      </section>

      <div class="cart-summary">
        <van-checkbox :model-value="allChecked" shape="round" @update:model-value="cart.toggleAll($event)">
          全选
        </van-checkbox>
        <div class="summary-price">
          <span>合计</span>
          <strong>¥{{ formatPrice(cart.total) }}</strong>
        </div>
        <button class="checkout-button" @click="checkout">结算（{{ cart.selectedCount }}）</button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.text-button {
  border: 0;
  background: transparent;
  color: #8a948c;
  font-size: 12px;
}

.loading-list {
  display: grid;
  gap: 12px;
}

.cart-empty p {
  margin: 0 0 14px;
  color: #7b857e;
}

.cart-empty button {
  padding: 10px 20px;
  border: 0;
  border-radius: 999px;
  background: #1f8a4c;
  color: #fff;
  font-size: 13px;
}

.cart-list {
  display: grid;
  gap: 12px;
}

.cart-item {
  position: relative;
  display: grid;
  grid-template-columns: 22px 84px 1fr;
  gap: 10px;
  align-items: center;
  padding: 12px;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(30, 62, 44, 0.06);
}

.cart-item > img {
  width: 84px;
  height: 84px;
  border-radius: 14px;
  object-fit: cover;
  background: #f3f1ea;
}

.cart-info h3 {
  margin: 0;
  padding-right: 16px;
  font-size: 14px;
}

.cart-info p {
  margin: 4px 0 10px;
  color: #8a948c;
  font-size: 11px;
}

.cart-price-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.cart-price-row .price {
  font-size: 17px;
}

.cart-price-row :deep(.van-stepper__input) {
  width: 30px;
}

.remove {
  position: absolute;
  top: 8px;
  right: 9px;
  border: 0;
  background: transparent;
  color: #a8afa9;
  font-size: 19px;
}

.cart-summary {
  position: fixed;
  right: 0;
  bottom: 50px;
  left: 0;
  z-index: 9;
  display: flex;
  align-items: center;
  gap: 12px;
  max-width: 480px;
  margin: 0 auto;
  padding: 10px 16px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 -8px 24px rgba(30, 62, 44, 0.06);
  backdrop-filter: blur(12px);
}

.summary-price {
  display: grid;
  margin-left: auto;
  text-align: right;
}

.summary-price span {
  color: #8a948c;
  font-size: 10px;
}

.summary-price strong {
  color: #e55039;
  font-size: 18px;
}

.checkout-button {
  padding: 11px 17px;
  border: 0;
  border-radius: 999px;
  background: #1f8a4c;
  color: #fff;
  font-size: 13px;
  font-weight: 700;
}
</style>
