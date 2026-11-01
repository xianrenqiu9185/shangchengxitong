<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getProducts } from '@/api/product'
import { useCartStore } from '@/stores/cart'
import { useUserStore } from '@/stores/user'
import AppHeader from '@/components/AppHeader.vue'
import ProductCard from '@/components/ProductCard.vue'

const router = useRouter()
const cart = useCartStore()
const user = useUserStore()
const keyword = ref('')
const results = ref([])
const searched = ref(false)
const loading = ref(false)
const sort = ref('')
const history = ref(JSON.parse(localStorage.getItem('linli_search_history') || '[]'))

async function search(value = keyword.value) {
  const text = String(value || '').trim()
  keyword.value = text
  if (!text) return
  loading.value = true
  searched.value = true
  try {
    const page = await getProducts({ keyword: text, sort: sort.value, pageNum: 1, pageSize: 50 })
    results.value = page.list
    history.value = [text, ...history.value.filter((item) => item !== text)].slice(0, 6)
    localStorage.setItem('linli_search_history', JSON.stringify(history.value))
  } finally {
    loading.value = false
  }
}

async function addToCart(product) {
  if (!user.token) {
    router.push({ name: 'login', query: { redirect: '/search' } })
    return
  }
  await cart.add(product.id, 1)
  showToast('已加入购物车')
}

function clearHistory() {
  history.value = []
  localStorage.removeItem('linli_search_history')
}

onMounted(() => {
  if (keyword.value) search()
})
</script>

<template>
  <div class="page search-page">
    <AppHeader title="搜索商品" />
    <van-search
      v-model="keyword"
      shape="round"
      placeholder="搜一搜新鲜蔬果、牛奶、鸡蛋"
      autofocus
      @search="search"
    />

    <section v-if="!searched" class="search-start">
      <div class="search-history-head">
        <h2>最近搜索</h2>
        <button v-if="history.length" @click="clearHistory">清空</button>
      </div>
      <div v-if="history.length" class="chip-row">
        <button v-for="item in history" :key="item" @click="search(item)">{{ item }}</button>
      </div>
      <p v-else class="muted">试试搜索“苹果”“牛奶”或“鸡蛋”</p>
      <div class="hot-card">
        <span>🔥</span>
        <div>
          <b>今日热搜</b>
          <p>红富士苹果 · 鲜牛奶 · 谷物鸡蛋</p>
        </div>
      </div>
    </section>

    <section v-else class="search-result">
      <div class="result-head">
        <div>
          <h2>“{{ keyword }}”</h2>
          <span>找到 {{ results.length }} 件商品</span>
        </div>
        <van-dropdown-menu>
          <van-dropdown-item
            v-model="sort"
            :options="[
              { text: '综合排序', value: '' },
              { text: '销量优先', value: 'salesDesc' },
              { text: '价格从低到高', value: 'priceAsc' },
            ]"
            @change="search"
          />
        </van-dropdown-menu>
      </div>
      <div v-if="loading" class="loading-list">
        <van-skeleton v-for="i in 3" :key="i" title :row="2" />
      </div>
      <div v-else-if="results.length" class="product-grid">
        <ProductCard v-for="product in results" :key="product.id" :product="product" @add="addToCart" />
      </div>
      <div v-else class="empty-block">
        <span class="emoji">🔍</span>
        没有找到相关商品，换个关键词试试
      </div>
    </section>
  </div>
</template>

<style scoped>
.search-page {
  padding-top: 12px;
}

.search-start {
  padding-top: 20px;
}

.search-history-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.search-history-head h2 {
  margin: 0;
  font-size: 16px;
}

.search-history-head button {
  border: 0;
  background: transparent;
  color: #8a948c;
  font-size: 12px;
}

.chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.chip-row button {
  padding: 8px 13px;
  border: 1px solid #e6e5dd;
  border-radius: 999px;
  background: #fff;
  color: #56625a;
  font-size: 12px;
}

.hot-card {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 22px;
  padding: 16px;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(30, 62, 44, 0.06);
}

.hot-card > span {
  display: grid;
  width: 42px;
  height: 42px;
  border-radius: 14px;
  background: #fff0e8;
  place-items: center;
  font-size: 22px;
}

.hot-card b {
  font-size: 14px;
}

.hot-card p {
  margin: 4px 0 0;
  color: #8a948c;
  font-size: 11px;
}

.result-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 14px 0 12px;
}

.result-head h2 {
  margin: 0;
  font-size: 17px;
}

.result-head span {
  color: #8a948c;
  font-size: 11px;
}

.result-head :deep(.van-dropdown-menu__bar) {
  height: 40px;
  border-radius: 12px;
  background: #fff;
  box-shadow: none;
}

.loading-list {
  display: grid;
  gap: 12px;
}
</style>
