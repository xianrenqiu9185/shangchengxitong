<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="not-found">
    <span>🥬</span>
    <h1>这一页走丢了</h1>
    <p>可能是商品已经下架，或者链接不正确。</p>
    <button @click="router.replace('/')">返回首页</button>
  </div>
</template>

<style scoped>
.not-found {
  display: grid;
  min-height: 80vh;
  padding: 20px;
  place-content: center;
  justify-items: center;
  text-align: center;
}

.not-found > span {
  font-size: 62px;
}

.not-found h1 {
  margin: 14px 0 5px;
  font-size: 22px;
}

.not-found p {
  margin: 0 0 18px;
  color: #8a948c;
  font-size: 12px;
}

.not-found button {
  padding: 11px 22px;
  border: 0;
  border-radius: 999px;
  background: #1f8a4c;
  color: #fff;
  font-size: 13px;
}
</style>
