<script setup>
import { reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showSuccessToast, showToast } from 'vant'
import { useUserStore } from '@/stores/user'
import { useSubmit } from '@/composables/useSubmit'

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const form = reactive({ phone: '13800000003', password: '123456' })

async function login() {
  if (!/^1\d{10}$/.test(form.phone)) {
    showToast('请输入 11 位手机号')
    return
  }
  if (!form.password) {
    showToast('请输入登录密码')
    return
  }
  await user.login({ phone: form.phone, password: form.password })
  showSuccessToast('登录成功')
  router.replace(String(route.query.redirect || '/'))
}

const { submitting, run } = useSubmit(login)
</script>

<template>
  <div class="auth-page">
    <div class="auth-art">
      <span class="leaf one">🥬</span>
      <span class="leaf two">🍅</span>
      <span class="leaf three">🥛</span>
      <div class="brand-mark">邻</div>
      <h1>邻里鲜生</h1>
      <p>新鲜生活，从邻里开始</p>
    </div>

    <section class="auth-card">
      <h2>欢迎回来</h2>
      <p class="auth-sub">登录后查看购物车、订单与专属优惠</p>
      <van-form @submit="run">
        <van-cell-group inset>
          <van-field
            v-model="form.phone"
            name="phone"
            label="手机号"
            placeholder="请输入手机号"
            type="tel"
            maxlength="11"
            clearable
          />
          <van-field
            v-model="form.password"
            name="password"
            label="密码"
            placeholder="请输入密码"
            type="password"
            clearable
          />
        </van-cell-group>
        <button class="auth-submit" type="submit" :disabled="submitting">
          {{ submitting ? '登录中…' : '登录' }}
        </button>
      </van-form>
      <div class="test-account">
        <b>演示账号</b>
        <span>13800000003 / 123456</span>
      </div>
      <p class="auth-link">还没有账号？<button @click="router.push('/register')">立即注册</button></p>
    </section>

    <button class="skip-button" @click="router.push('/')">先随便逛逛</button>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  padding: 0 20px 30px;
  background:
    radial-gradient(circle at 85% 8%, rgba(255, 255, 255, 0.28), transparent 26%),
    linear-gradient(160deg, #1b4b31 0%, #1f8a4c 55%, #7fc18f 100%);
}

.auth-art {
  position: relative;
  padding: 56px 0 34px;
  color: #fff;
  text-align: center;
}

.brand-mark {
  display: grid;
  width: 62px;
  height: 62px;
  margin: 0 auto 13px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.13);
  place-items: center;
  font-size: 30px;
  font-weight: 800;
  backdrop-filter: blur(10px);
}

.auth-art h1 {
  margin: 0;
  font-size: 30px;
  letter-spacing: 0.05em;
}

.auth-art p {
  margin: 8px 0 0;
  font-size: 12px;
  letter-spacing: 0.16em;
  opacity: 0.72;
}

.leaf {
  position: absolute;
  filter: drop-shadow(0 10px 18px rgba(0, 0, 0, 0.15));
}

.leaf.one {
  top: 44px;
  left: 12px;
  font-size: 42px;
  transform: rotate(-18deg);
}

.leaf.two {
  top: 70px;
  right: 4px;
  font-size: 34px;
  transform: rotate(16deg);
}

.leaf.three {
  right: 34px;
  bottom: 8px;
  font-size: 30px;
  transform: rotate(10deg);
}

.auth-card {
  padding: 22px 16px 18px;
  border-radius: 24px;
  background: #fff;
  box-shadow: 0 24px 50px rgba(9, 40, 23, 0.24);
}

.auth-card h2 {
  margin: 0 0 5px;
  font-size: 22px;
}

.auth-sub {
  margin: 0 0 18px;
  color: #8a948c;
  font-size: 12px;
}

.auth-card :deep(.van-cell-group--inset) {
  margin: 0;
  border-radius: 15px;
  background: #f7f8f5;
}

.auth-submit {
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

.auth-submit:disabled {
  opacity: 0.65;
}

.test-account {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 14px;
  padding: 11px 12px;
  border-radius: 12px;
  background: #f2fbf4;
  color: #4e6b58;
  font-size: 11px;
}

.test-account b {
  color: #1f8a4c;
}

.auth-link {
  margin: 17px 0 0;
  color: #8a948c;
  font-size: 12px;
  text-align: center;
}

.auth-link button {
  border: 0;
  background: transparent;
  color: #1f8a4c;
  font-size: 12px;
  font-weight: 700;
}

.skip-button {
  display: block;
  margin: 18px auto 0;
  border: 0;
  background: transparent;
  color: rgba(255, 255, 255, 0.75);
  font-size: 12px;
}
</style>
