<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getCategories, getProducts } from '@/api/product'
import { useCartStore } from '@/stores/cart'
import { useUserStore } from '@/stores/user'
import ProductCard from '@/components/ProductCard.vue'

const route = useRoute()
const router = useRouter()
const cart = useCartStore()
const user = useUserStore()
const categories = ref([])
const activeId = ref(Number(route.query.categoryId || 0))
const products = ref([])
const loading = ref(false)

async function loadProducts() {
  loading.value = true
  try {
    const page = await getProducts({ categoryId: activeId.value, pageNum: 1, pageSize: 50 })
    products.value = page.list
  } finally {
    loading.value = false
  }
}

async function addToCart(product) {
  if (!user.token) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  await cart.add(product.id, 1)
  showToast('已加入购物车')
}

watch(activeId, loadProducts)

onMounted(async () => {
  categories.value = await getCategories()
  await loadProducts()
})
</script>

<template>
  <div class="page category-page with-tabbar">
    <header class="category-head">
      <div>
        <p>COMMUNITY MARKET</p>
        <h1>全部分类</h1>
      </div>
      <button class="icon-button" @click="router.push('/search')">⌕</button>
    </header>

    <div class="category-layout">
      <nav class="category-nav">
        <button
          v-for="category in categories"
          :key="category.id"
          :class="{ active: activeId === category.id }"
          @click="activeId = category.id"
        >
          <span>{{ category.icon }}</span>
          {{ category.name }}
        </button>
      </nav>
      <main class="category-content">
        <div class="category-title">
          <h2>{{ categories.find((item) => item.id === activeId)?.name || '全部商品' }}</h2>
          <span>{{ products.length }} 件</span>
        </div>
        <div v-if="loading" class="loading-list">
          <van-skeleton v-for="i in 3" :key="i" title :row="2" />
        </div>
        <div v-else-if="products.length" class="product-grid">
          <ProductCard v-for="product in products" :key="product.id" :product="product" @add="addToCart" />
        </div>
        <div v-else class="empty-block">
          <span class="emoji">🥬</span>
          这个分类暂时没有商品
        </div>
      </main>
    </div>
  </div>
</template>

<style scoped>
.category-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 0 10px;
}

.category-head p {
  margin: 0 0 3px;
  color: #8a948c;
  font-size: 9px;
  letter-spacing: 0.16em;
}

.category-head h1 {
  margin: 0;
  font-size: 24px;
}

.category-layout {
  display: grid;
  grid-template-columns: 92px 1fr;
  gap: 12px;
  align-items: start;
}

.category-nav {
  position: sticky;
  top: 8px;
  display: grid;
  gap: 4px;
  padding: 6px;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(30, 62, 44, 0.06);
}

.category-nav button {
  display: grid;
  justify-items: center;
  gap: 3px;
  padding: 10px 4px;
  border: 0;
  border-radius: 13px;
  background: transparent;
  color: #667168;
  font-size: 11px;
}

.category-nav button span {
  font-size: 18px;
}

.category-nav button.active {
  background: #eaf7ee;
  color: #1f8a4c;
  font-weight: 700;
}

.category-title {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 10px;
}

.category-title h2 {
  margin: 0;
  font-size: 16px;
}

.category-title span {
  color: #8a948c;
  font-size: 11px;
}

.category-content :deep(.product-grid) {
  grid-template-columns: 1fr;
}

.loading-list {
  display: grid;
  gap: 12px;
}
</style>
