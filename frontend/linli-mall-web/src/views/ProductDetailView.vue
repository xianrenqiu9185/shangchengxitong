<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showSuccessToast, showToast } from 'vant'
import { getProduct, getProductReviews } from '@/api/product'
import { useCartStore } from '@/stores/cart'
import { useUserStore } from '@/stores/user'
import { formatPrice } from '@/utils/format'
import AppHeader from '@/components/AppHeader.vue'

const route = useRoute()
const router = useRouter()
const cart = useCartStore()
const user = useUserStore()
const product = ref(null)
const reviews = ref([])
const quantity = ref(1)
const loading = ref(true)

async function addToCart(goCheckout = false) {
  if (!user.token) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  await cart.add(product.value.id, quantity.value)
  showSuccessToast('已加入购物车')
  if (goCheckout) router.push('/cart')
}

onMounted(async () => {
  try {
    const [item, reviewPage] = await Promise.all([
      getProduct(route.params.id),
      getProductReviews(route.params.id),
    ])
    product.value = item
    reviews.value = reviewPage.list
  } catch {
    showToast('商品不存在或已下架')
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="detail-page">
    <div class="detail-head">
      <AppHeader title="商品详情" />
    </div>
    <van-skeleton v-if="loading" title :row="6" />
    <template v-else-if="product">
      <img class="detail-image" :src="product.image" :alt="product.name" />
      <section class="detail-card">
        <div class="price-row">
          <span class="price"><small>¥</small>{{ formatPrice(product.price) }}</span>
          <span class="original">¥{{ formatPrice(product.originalPrice) }}</span>
          <span class="sales">已售 {{ product.sales }}</span>
        </div>
        <h1>{{ product.name }}</h1>
        <p class="subtitle">{{ product.subtitle }}</p>
        <div class="tags">
          <van-tag v-for="tag in product.tags" :key="tag" plain type="success">{{ tag }}</van-tag>
        </div>
        <div class="stock-row">
          <span>{{ product.unit }}</span>
          <span>库存 {{ product.stock }} 件</span>
        </div>
      </section>

      <section class="detail-card section-card">
        <h2>商品介绍</h2>
        <p>{{ product.description }}</p>
      </section>

      <section class="detail-card section-card">
        <div class="review-head">
          <h2>邻居评价</h2>
          <span>{{ reviews.length }} 条</span>
        </div>
        <div v-if="reviews.length" class="review-list">
          <article v-for="review in reviews" :key="review.id">
            <div>
              <b>{{ review.user }}</b>
              <span>{{ '★'.repeat(review.rating) }}</span>
            </div>
            <p>{{ review.content }}</p>
            <small>{{ review.createdAt }}</small>
          </article>
        </div>
        <p v-else class="muted">还没有评价，等你来尝鲜。</p>
      </section>

      <div class="detail-actions">
        <div class="quantity">
          <span>数量</span>
          <van-stepper v-model="quantity" :max="product.stock" integer />
        </div>
        <button class="cart-button" @click="addToCart(false)">加入购物车</button>
        <button class="buy-button" @click="addToCart(true)">立即购买</button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.detail-page {
  min-height: 100vh;
  padding: 0 16px 112px;
}

.detail-head {
  position: sticky;
  top: 0;
  z-index: 5;
  background: linear-gradient(#f6f4ee 70%, rgba(246, 244, 238, 0));
}

.detail-image {
  width: 100%;
  aspect-ratio: 4 / 3;
  border-radius: 24px;
  object-fit: cover;
  background: #f1efe8;
}

.detail-card {
  margin-top: 14px;
  padding: 17px;
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(30, 62, 44, 0.06);
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.price-row .price {
  font-size: 30px;
}

.original {
  color: #9aa29c;
  font-size: 12px;
  text-decoration: line-through;
}

.sales {
  margin-left: auto;
  color: #8a948c;
  font-size: 11px;
}

.detail-card h1 {
  margin: 12px 0 5px;
  font-size: 21px;
}

.subtitle {
  margin: 0;
  color: #7b857e;
  font-size: 13px;
}

.tags {
  display: flex;
  gap: 7px;
  margin-top: 12px;
}

.stock-row {
  display: flex;
  justify-content: space-between;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid #f0efe9;
  color: #7b857e;
  font-size: 12px;
}

.section-card h2 {
  margin: 0 0 10px;
  font-size: 16px;
}

.section-card > p {
  margin: 0;
  color: #59645d;
  font-size: 13px;
  line-height: 1.75;
}

.review-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}

.review-head span {
  color: #8a948c;
  font-size: 11px;
}

.review-list {
  display: grid;
  gap: 14px;
}

.review-list article {
  padding-top: 2px;
  border-bottom: 1px solid #f3f1ec;
}

.review-list article:last-child {
  border-bottom: 0;
}

.review-list article > div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
}

.review-list article span {
  color: #ff9f43;
  letter-spacing: 1px;
}

.review-list article p {
  margin: 6px 0 4px;
  color: #4f5a53;
  font-size: 13px;
  line-height: 1.6;
}

.review-list small {
  color: #9aa29c;
  font-size: 10px;
}

.detail-actions {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  gap: 8px;
  max-width: 480px;
  margin: 0 auto;
  padding: 12px 16px calc(12px + env(safe-area-inset-bottom));
  background: rgba(246, 244, 238, 0.96);
  backdrop-filter: blur(12px);
}

.quantity {
  display: grid;
  gap: 2px;
  margin-right: auto;
  color: #8a948c;
  font-size: 10px;
}

.quantity :deep(.van-stepper__input) {
  width: 32px;
}

.cart-button,
.buy-button {
  padding: 12px 14px;
  border: 0;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
}

.cart-button {
  background: #eaf7ee;
  color: #1f8a4c;
}

.buy-button {
  background: #1f8a4c;
  color: #fff;
}
</style>
