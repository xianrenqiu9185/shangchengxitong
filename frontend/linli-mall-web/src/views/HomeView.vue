<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getCategories, getProducts } from '@/api/product'
import { useCartStore } from '@/stores/cart'
import { useUserStore } from '@/stores/user'
import { formatPrice } from '@/utils/format'
import ProductCard from '@/components/ProductCard.vue'
import SectionHead from '@/components/SectionHead.vue'

const router = useRouter()
const cart = useCartStore()
const user = useUserStore()
const categories = ref([])
const products = ref([])
const loading = ref(true)

async function load() {
  loading.value = true
  try {
    const [categoryList, productPage] = await Promise.all([
      getCategories(),
      getProducts({ pageNum: 1, pageSize: 12 }),
    ])
    categories.value = categoryList
    products.value = productPage.list
  } finally {
    loading.value = false
  }
}

async function addToCart(product) {
  if (!user.token) {
    router.push({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })
    return
  }
  await cart.add(product.id, 1)
  showToast('已加入购物车')
}

onMounted(load)
</script>

<template>
  <div class="page with-tabbar home-page">
    <header class="home-head">
      <div class="location">
        <span>📍 邻里社区 · 3号楼</span>
        <button>切换</button>
      </div>
      <div class="brand-row">
        <div>
          <p>FRESH NEIGHBORHOOD</p>
          <h1>邻里鲜生</h1>
        </div>
        <button class="cart-entry" aria-label="购物车" @click="router.push('/cart')">
          🛒
          <span v-if="cart.count">{{ cart.count }}</span>
        </button>
      </div>
      <button class="search-bar" @click="router.push('/search')">
        <span>⌕</span>
        搜一搜 新鲜蔬果、牛奶、鸡蛋
      </button>
    </header>

    <section class="hero-card">
      <div class="hero-copy">
        <span>社区新鲜直达</span>
        <h2>今天下单<br />30 分钟送到家</h2>
        <button @click="router.push('/category')">去逛逛 →</button>
      </div>
      <div class="hero-art">
        <span>🥬</span>
        <i>🍅</i>
        <b>🥛</b>
      </div>
    </section>

    <section class="category-grid">
      <button
        v-for="category in categories.filter((item) => item.id !== 0).slice(0, 7)"
        :key="category.id"
        class="category-item"
        @click="router.push({ name: 'category', query: { categoryId: category.id } })"
      >
        <span :style="{ background: category.color }">{{ category.icon }}</span>
        <b>{{ category.name }}</b>
      </button>
      <button class="category-item" @click="router.push('/category')">
        <span style="background: #f2f1ed">⋯</span>
        <b>全部分类</b>
      </button>
    </section>

    <section class="section">
      <SectionHead title="限时特惠" subtitle="今日 18:00 前">
        <span>今日鲜价</span>
      </SectionHead>
      <div class="flash-list">
        <article
          v-for="product in products.slice(0, 4)"
          :key="product.id"
          class="flash-card"
          @click="router.push(`/products/${product.id}`)"
        >
          <img :src="product.image" :alt="product.name" />
          <div>
            <b>{{ product.name }}</b>
            <span class="price"><small>¥</small>{{ formatPrice(product.price) }}</span>
          </div>
          <button @click.stop="addToCart(product)">＋</button>
        </article>
      </div>
    </section>

    <section class="section">
      <SectionHead title="为你推荐" subtitle="社区邻居都在买" />
      <div v-if="loading" class="product-grid">
        <van-skeleton v-for="i in 4" :key="i" title :row="3" />
      </div>
      <div v-else class="product-grid">
        <ProductCard v-for="product in products" :key="product.id" :product="product" @add="addToCart" />
      </div>
    </section>
  </div>
</template>

<style scoped>
.home-page {
  padding-top: 10px;
}

.home-head {
  padding: 2px 0 6px;
}

.location {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #56625a;
  font-size: 12px;
}

.location button {
  border: 0;
  background: transparent;
  color: #1f8a4c;
  font-size: 11px;
}

.brand-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
}

.brand-row p {
  margin: 0 0 2px;
  color: #8a948c;
  font-size: 9px;
  letter-spacing: 0.18em;
}

.brand-row h1 {
  margin: 0;
  font-size: 26px;
  letter-spacing: 0.02em;
}

.cart-entry {
  position: relative;
  width: 42px;
  height: 42px;
  border: 1px solid #e8e7df;
  border-radius: 50%;
  background: #fff;
  font-size: 19px;
  box-shadow: 0 5px 16px rgba(30, 62, 44, 0.06);
}

.cart-entry span {
  position: absolute;
  top: -3px;
  right: -3px;
  min-width: 18px;
  height: 18px;
  padding: 0 4px;
  border-radius: 999px;
  background: #ff8a3d;
  color: #fff;
  font-size: 10px;
  line-height: 18px;
  text-align: center;
}

.search-bar {
  display: flex;
  align-items: center;
  gap: 9px;
  width: 100%;
  margin-top: 14px;
  padding: 13px 15px;
  border: 0;
  border-radius: 16px;
  background: #fff;
  color: #8a948c;
  font-size: 13px;
  text-align: left;
  box-shadow: 0 8px 22px rgba(30, 62, 44, 0.06);
}

.search-bar span {
  color: #1f8a4c;
  font-size: 19px;
  line-height: 1;
}

.hero-card {
  position: relative;
  display: flex;
  min-height: 166px;
  margin-top: 16px;
  padding: 22px;
  overflow: hidden;
  border-radius: 24px;
  background: linear-gradient(135deg, #1f8a4c 0%, #35a966 62%, #82c98f 100%);
  color: #fff;
  box-shadow: 0 18px 38px rgba(31, 138, 76, 0.22);
}

.hero-copy {
  position: relative;
  z-index: 2;
}

.hero-copy > span {
  font-size: 11px;
  letter-spacing: 0.08em;
  opacity: 0.82;
}

.hero-copy h2 {
  margin: 9px 0 16px;
  font-size: 24px;
  line-height: 1.22;
}

.hero-copy button {
  padding: 8px 15px;
  border: 0;
  border-radius: 999px;
  background: #fff;
  color: #1f8a4c;
  font-size: 12px;
  font-weight: 700;
}

.hero-art {
  position: absolute;
  right: -4px;
  bottom: -8px;
  width: 150px;
  height: 160px;
}

.hero-art span,
.hero-art i,
.hero-art b {
  position: absolute;
  filter: drop-shadow(0 10px 16px rgba(0, 0, 0, 0.12));
}

.hero-art span {
  right: 10px;
  bottom: 10px;
  font-size: 92px;
  transform: rotate(-8deg);
}

.hero-art i {
  right: 94px;
  bottom: 18px;
  font-size: 46px;
  font-style: normal;
  transform: rotate(-18deg);
}

.hero-art b {
  right: 18px;
  top: 8px;
  font-size: 38px;
  transform: rotate(12deg);
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 15px 8px;
  margin-top: 20px;
}

.category-item {
  display: grid;
  justify-items: center;
  gap: 7px;
  border: 0;
  background: transparent;
  color: #48534c;
}

.category-item span {
  display: grid;
  width: 48px;
  height: 48px;
  border-radius: 17px;
  place-items: center;
  font-size: 23px;
}

.category-item b {
  font-size: 11px;
  font-weight: 500;
}

.flash-card {
  flex: 0 0 168px;
  padding: 9px;
  border-radius: 17px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(30, 62, 44, 0.06);
}

.flash-card img {
  width: 100%;
  height: 92px;
  border-radius: 12px;
  object-fit: cover;
}

.flash-card > div {
  display: grid;
  gap: 3px;
  margin-top: 8px;
}

.flash-card b {
  overflow: hidden;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.flash-card .price {
  font-size: 16px;
}

.flash-card > button {
  float: right;
  width: 24px;
  height: 24px;
  margin-top: -22px;
  border: 0;
  border-radius: 50%;
  background: #1f8a4c;
  color: #fff;
  font-size: 16px;
  line-height: 1;
}
</style>
