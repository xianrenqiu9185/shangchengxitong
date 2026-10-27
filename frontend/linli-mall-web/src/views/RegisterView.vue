<script setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { showSuccessToast, showToast } from 'vant'
import { useUserStore } from '@/stores/user'
import { useSubmit } from '@/composables/useSubmit'
import AppHeader from '@/components/AppHeader.vue'

const router = useRouter()
const user = useUserStore()
const form = reactive({ phone: '', password: '', confirmPassword: '', nickname: '' })

async function register() {
  if (!/^1\d{10}$/.test(form.phone)) {
    showToast('请输入 11 位手机号')
    return
  }
  if (form.password.length < 6) {
    showToast('密码至少 6 位')
    return
  }
  if (form.password !== form.confirmPassword) {
    showToast('两次输入的密码不一致')
    return
  }
  await user.register({ phone: form.phone, password: form.password, nickname: form.nickname })
  showSuccessToast('注册成功，请登录')
  router.replace({ name: 'login', query: { phone: form.phone } })
}

const { submitting, run } = useSubmit(register)
</script>

<template>
  <div class="page register-page">
    <AppHeader title="注册会员" />
    <section class="register-intro">
      <span>🌱</span>
      <h1>加入邻里鲜生</h1>
      <p>新鲜蔬果、社区配送，一键送到家</p>
    </section>
    <van-form class="register-form" @submit="run">
      <van-cell-group inset>
        <van-field v-model="form.nickname" label="昵称" placeholder="怎么称呼你（选填）" />
        <van-field v-model="form.phone" label="手机号" type="tel" maxlength="11" placeholder="请输入手机号" />
        <van-field v-model="form.password" label="密码" type="password" placeholder="至少 6 位" />
        <van-field v-model="form.confirmPassword" label="确认密码" type="password" placeholder="再次输入密码" />
      </van-cell-group>
      <button class="register-submit" type="submit" :disabled="submitting">
        {{ submitting ? '注册中…' : '注册' }}
      </button>
    </van-form>
    <p class="register-link">已有账号？<button @click="router.push('/login')">去登录</button></p>
  </div>
</template>

<style scoped>
.register-page {
  padding-top: 12px;
}

.register-intro {
  padding: 24px 0 20px;
  text-align: center;
}

.register-intro > span {
  font-size: 44px;
}

.register-intro h1 {
  margin: 10px 0 5px;
  font-size: 23px;
}

.register-intro p {
  margin: 0;
  color: #8a948c;
  font-size: 12px;
}

.register-form :deep(.van-cell-group--inset) {
  margin: 0;
  border-radius: 18px;
  overflow: hidden;
  background: #fff;
}

.register-submit {
  width: 100%;
  margin-top: 18px;
  padding: 13px;
  border: 0;
  border-radius: 14px;
  background: #1f8a4c;
  color: #fff;
  font-size: 15px;
  font-weight: 700;
}

.register-link {
  margin-top: 17px;
  color: #8a948c;
  font-size: 12px;
  text-align: center;
}

.register-link button {
  border: 0;
  background: transparent;
  color: #1f8a4c;
  font-size: 12px;
  font-weight: 700;
}
</style>
