<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="not-found">
    <span>🧭</span>
    <h1>页面不存在</h1>
    <p>检查地址是否正确，或返回经营看板继续操作。</p>
    <el-button type="primary" @click="router.replace('/dashboard')">返回经营看板</el-button>
  </div>
</template>

<style scoped>
.not-found {
  display: grid;
  min-height: 100vh;
  padding: 30px;
  place-content: center;
  justify-items: center;
  text-align: center;
}

.not-found span {
  font-size: 58px;
}

.not-found h1 {
  margin: 14px 0 6px;
  font-size: 24px;
}

.not-found p {
  margin: 0 0 18px;
  color: #7b887f;
  font-size: 12px;
}
</style>
