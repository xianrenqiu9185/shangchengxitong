<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const loading = ref(false)
const form = reactive({ phone: '13800000001', password: '123456' })

async function submit() {
  loading.value = true
  try {
    await user.login(form)
    ElMessage.success('登录成功')
    router.replace(String(route.query.redirect || '/dashboard'))
  } finally {
    loading.value = false
  }
}

function fill(role) {
  form.phone = role === 'admin' ? '13800000001' : '13800000002'
  form.password = '123456'
}
</script>

<template>
  <div class="login-page">
    <section class="login-brand">
      <div class="brand-top">
        <div class="brand-mark">邻</div>
        <div>
          <b>邻里鲜生</b>
          <span>社区超市商城系统</span>
        </div>
      </div>
      <div class="brand-copy">
        <p>OPERATIONS CENTER</p>
        <h1>让社区里的<br />每一份新鲜有序抵达</h1>
        <span>商品、库存、订单、售后与经营数据，在一个工作台完成。</span>
      </div>
      <div class="brand-stats">
        <div><b>200</b><span>峰值 TPS</span></div>
        <div><b>30min</b><span>配送时效</span></div>
        <div><b>99.5%</b><span>可用性目标</span></div>
      </div>
      <div class="brand-decoration">🥬</div>
    </section>

    <section class="login-panel">
      <div class="login-form">
        <p class="form-eyebrow">WELCOME BACK</p>
        <h2>登录运营后台</h2>
        <p class="form-sub">使用后台账号登录，查看今日经营与待处理事项</p>
        <el-form label-position="top" @submit.prevent="submit">
          <el-form-item label="手机号">
            <el-input v-model="form.phone" size="large" maxlength="11" placeholder="请输入后台账号手机号" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="form.password" size="large" type="password" show-password placeholder="请输入密码" @keyup.enter="submit" />
          </el-form-item>
          <el-button type="primary" size="large" class="login-button" :loading="loading" @click="submit">
            登录工作台
          </el-button>
        </el-form>

        <div class="quick-login">
          <span>演示账号</span>
          <button @click="fill('admin')">平台管理员</button>
          <button @click="fill('operator')">商家运营</button>
        </div>
        <p class="login-hint">密码统一为 123456。运营账号不会显示会员、角色、配置与日志菜单。</p>
      </div>
      <footer>邻里鲜生 · 课程实验项目 · v0.1.0</footer>
    </section>
  </div>
</template>

<style scoped>
.login-page {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(420px, 0.95fr);
  min-height: 100vh;
  background: #f4f6f3;
}

.login-brand {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  overflow: hidden;
  padding: 46px 54px;
  background:
    radial-gradient(circle at 84% 12%, rgba(255, 255, 255, 0.16), transparent 24%),
    linear-gradient(145deg, #102e20 0%, #1c5c3a 50%, #2c8a55 100%);
  color: #fff;
}

.brand-top {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-mark {
  display: grid;
  width: 46px;
  height: 46px;
  border: 1px solid rgba(255, 255, 255, 0.45);
  border-radius: 15px;
  background: rgba(255, 255, 255, 0.1);
  place-items: center;
  font-size: 23px;
  font-weight: 800;
}

.brand-top div:last-child {
  display: grid;
  gap: 2px;
}

.brand-top b {
  font-size: 17px;
  letter-spacing: 0.05em;
}

.brand-top span {
  color: rgba(255, 255, 255, 0.5);
  font-size: 10px;
}

.brand-copy {
  position: relative;
  z-index: 2;
  max-width: 560px;
}

.brand-copy p {
  margin: 0 0 16px;
  color: rgba(255, 255, 255, 0.5);
  font-size: 10px;
  letter-spacing: 0.24em;
}

.brand-copy h1 {
  margin: 0;
  font-size: clamp(34px, 4vw, 54px);
  line-height: 1.18;
  letter-spacing: -0.02em;
}

.brand-copy > span {
  display: block;
  max-width: 420px;
  margin-top: 20px;
  color: rgba(255, 255, 255, 0.65);
  font-size: 13px;
  line-height: 1.8;
}

.brand-stats {
  position: relative;
  z-index: 2;
  display: flex;
  gap: 44px;
}

.brand-stats div {
  display: grid;
  gap: 4px;
}

.brand-stats b {
  font-size: 22px;
}

.brand-stats span {
  color: rgba(255, 255, 255, 0.45);
  font-size: 10px;
}

.brand-decoration {
  position: absolute;
  right: -18px;
  bottom: 80px;
  font-size: 230px;
  opacity: 0.13;
  transform: rotate(-12deg);
}

.login-panel {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 40px 8vw;
}

.login-form {
  width: 100%;
  max-width: 390px;
  margin: 0 auto;
}

.form-eyebrow {
  margin: 0 0 7px;
  color: #1f8a4c;
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.22em;
}

.login-form h2 {
  margin: 0;
  font-size: 28px;
}

.form-sub {
  margin: 8px 0 25px;
  color: #7b887f;
  font-size: 12px;
  line-height: 1.7;
}

.login-button {
  width: 100%;
  margin-top: 4px;
  border-radius: 11px;
}

.quick-login {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 19px;
  padding: 11px;
  border-radius: 10px;
  background: #edf5ef;
  font-size: 10px;
}

.quick-login span {
  color: #7b887f;
}

.quick-login button {
  padding: 4px 8px;
  border: 1px solid #cfe3d6;
  border-radius: 999px;
  background: #fff;
  color: #1f8a4c;
  font-size: 10px;
  cursor: pointer;
}

.login-hint {
  margin: 12px 0 0;
  color: #9aa59d;
  font-size: 10px;
  line-height: 1.7;
}

.login-panel footer {
  position: absolute;
  bottom: 22px;
  color: #a1aba4;
  font-size: 9px;
}

@media (max-width: 900px) {
  .login-page {
    grid-template-columns: 1fr;
  }

  .login-brand {
    min-height: 300px;
    padding: 30px;
  }

  .brand-copy h1 {
    font-size: 30px;
  }

  .brand-stats,
  .brand-decoration {
    display: none;
  }

  .login-panel {
    padding: 34px 24px 70px;
  }
}
</style>
