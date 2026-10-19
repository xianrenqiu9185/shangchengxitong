<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="result-page">
    <div class="result-icon">🔒</div>
    <h1>无操作权限</h1>
    <p>当前账号没有访问该页面的权限。前端菜单已隐藏，后端仍会返回 10003 做最终拦截。</p>
    <el-button type="primary" @click="router.replace('/dashboard')">返回经营看板</el-button>
  </div>
</template>

<style scoped>
.result-page {
  display: grid;
  min-height: 100vh;
  padding: 30px;
  place-content: center;
  justify-items: center;
  text-align: center;
  background: #f4f6f3;
}

.result-icon {
  font-size: 62px;
}

.result-page h1 {
  margin: 15px 0 7px;
  font-size: 24px;
}

.result-page p {
  max-width: 420px;
  margin: 0 0 20px;
  color: #7b887f;
  font-size: 12px;
  line-height: 1.8;
}
</style>
