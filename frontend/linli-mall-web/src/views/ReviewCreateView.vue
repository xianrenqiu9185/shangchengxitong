<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showSuccessToast, showToast } from 'vant'
import { getOrder } from '@/api/order'
import { createReview } from '@/api/review'
import { useSubmit } from '@/composables/useSubmit'
import AppHeader from '@/components/AppHeader.vue'

const route = useRoute()
const router = useRouter()
const order = ref(null)
const productId = ref(null)
const form = reactive({ rating: 5, content: '', anonymous: false })
const loading = ref(true)

const selectedItem = computed(() => order.value?.items.find((item) => item.productId === productId.value))

async function submit() {
  if (!productId.value) {
    showToast('请选择要评价的商品')
    return
  }
  if (!form.content.trim()) {
    showToast('写几句真实感受吧')
    return
  }
  await createReview({
    orderNo: order.value.orderNo,
    productId: productId.value,
    rating: form.rating,
    content: form.content.trim(),
    anonymous: form.anonymous,
  })
  showSuccessToast('评价已发布')
  router.replace(`/products/${productId.value}`)
}

const { submitting, run } = useSubmit(submit)

onMounted(async () => {
  const orderNo = String(route.query.orderNo || '')
  if (!orderNo) {
    showToast('缺少订单信息')
    router.replace('/orders')
    return
  }
  try {
    order.value = await getOrder(orderNo)
    if (order.value.status !== 'COMPLETED') {
      showToast('订单完成后才可以评价')
      router.replace(`/orders/${orderNo}`)
      return
    }
    productId.value = order.value.items[0]?.productId || null
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="page review-page">
    <AppHeader title="发表评价" />

    <van-skeleton v-if="loading" title :row="5" />

    <template v-else-if="order">
      <section class="review-intro">
        <span>🌿</span>
        <div>
          <h1>这次的新鲜，还满意吗？</h1>
          <p>你的真实评价会帮助更多邻居挑到好商品。</p>
        </div>
      </section>

      <section class="review-card">
        <h2>选择商品</h2>
        <button
          v-for="item in order.items"
          :key="item.productId"
          class="product-option"
          :class="{ active: productId === item.productId }"
          @click="productId = item.productId"
        >
          <img :src="item.image" :alt="item.name" />
          <div><b>{{ item.name }}</b><span>{{ item.unit }} × {{ item.quantity }}</span></div>
          <em>{{ productId === item.productId ? '已选择' : '选择' }}</em>
        </button>
      </section>

      <section class="review-card">
        <h2>商品评分</h2>
        <van-rate v-model="form.rating" size="28" color="#ff9f43" void-color="#e8e7df" />
        <p class="rating-text">{{ ['', '很不满意', '不太满意', '一般', '满意', '非常满意'][form.rating] }}</p>
      </section>

      <section class="review-card">
        <h2>评价内容</h2>
        <van-field
          v-model="form.content"
          type="textarea"
          rows="5"
          maxlength="200"
          show-word-limit
          placeholder="说说商品的新鲜度、口感、包装和配送体验..."
        />
        <div class="anonymous-row">
          <div><b>匿名评价</b><span>其他邻居将看不到你的昵称</span></div>
          <van-switch v-model="form.anonymous" size="22px" />
        </div>
      </section>

      <section v-if="selectedItem" class="review-note">
        <b>评价订单</b>
        <span>{{ order.orderNo }}</span>
      </section>

      <div class="review-submit">
        <button :disabled="submitting" @click="run">{{ submitting ? '发布中…' : '发布评价' }}</button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.review-page {
  padding-bottom: 92px;
}

.review-intro {
  display: flex;
  align-items: center;
  gap: 13px;
  padding: 17px;
  border-radius: 20px;
  background: linear-gradient(135deg, #eaf7ee, #d8f0de);
}

.review-intro > span {
  font-size: 38px;
}

.review-intro h1 {
  margin: 0 0 4px;
  font-size: 17px;
}

.review-intro p {
  margin: 0;
  color: #5d6a61;
  font-size: 11px;
}

.review-card {
  margin-top: 13px;
  padding: 16px;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(30, 62, 44, 0.06);
}

.review-card h2 {
  margin: 0 0 12px;
  font-size: 15px;
}

.product-option {
  display: grid;
  grid-template-columns: 54px 1fr auto;
  gap: 10px;
  align-items: center;
  width: 100%;
  margin-bottom: 8px;
  padding: 9px;
  border: 1px solid #ecebe4;
  border-radius: 14px;
  background: #fff;
  text-align: left;
}

.product-option.active {
  border-color: #1f8a4c;
  background: #f3fbf5;
}

.product-option:last-child {
  margin-bottom: 0;
}

.product-option img {
  width: 54px;
  height: 54px;
  border-radius: 11px;
  object-fit: cover;
}

.product-option div {
  display: grid;
  gap: 3px;
}

.product-option b {
  font-size: 12px;
}

.product-option span {
  color: #8a948c;
  font-size: 10px;
}

.product-option em {
  color: #1f8a4c;
  font-size: 10px;
  font-style: normal;
  font-weight: 700;
}

.rating-text {
  margin: 8px 0 0;
  color: #ff8a3d;
  font-size: 11px;
}

.review-card :deep(.van-field) {
  padding: 0;
  background: transparent;
}

.anonymous-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 14px;
  padding-top: 13px;
  border-top: 1px solid #f2f1ec;
}

.anonymous-row div {
  display: grid;
  gap: 3px;
}

.anonymous-row b {
  font-size: 12px;
}

.anonymous-row span {
  color: #8a948c;
  font-size: 10px;
}

.review-note {
  display: flex;
  justify-content: space-between;
  margin-top: 12px;
  padding: 12px 3px;
  color: #8a948c;
  font-size: 10px;
}

.review-note b {
  color: #56625a;
  font-weight: 500;
}

.review-submit {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  max-width: 480px;
  margin: 0 auto;
  padding: 12px 16px calc(12px + env(safe-area-inset-bottom));
  background: rgba(246, 244, 238, 0.96);
  backdrop-filter: blur(12px);
}

.review-submit button {
  width: 100%;
  padding: 13px;
  border: 0;
  border-radius: 14px;
  background: #1f8a4c;
  color: #fff;
  font-size: 14px;
  font-weight: 700;
}

.review-submit button:disabled {
  opacity: 0.6;
}
</style>
