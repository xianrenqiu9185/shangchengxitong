<script setup>
import { onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { showSuccessToast, showToast } from 'vant'
import { useUserStore } from '@/stores/user'
import { useSubmit } from '@/composables/useSubmit'
import { maskPhone } from '@/utils/format'
import AppHeader from '@/components/AppHeader.vue'

const router = useRouter()
const user = useUserStore()
const form = reactive({ nickname: '', phone: '' })

async function save() {
  if (!form.nickname.trim()) {
    showToast('昵称不能为空')
    return
  }
  await user.updateProfile({ nickname: form.nickname.trim() })
  showSuccessToast('资料已更新')
  router.back()
}

const { submitting, run } = useSubmit(save)

onMounted(async () => {
  await user.fetchProfile().catch(() => {})
  form.nickname = user.profile?.nickname || ''
  form.phone = user.profile?.phone || ''
})
</script>

<template>
  <div class="page profile-edit-page">
    <AppHeader title="编辑资料" />
    <section class="profile-preview">
      <img :src="user.profile?.avatar || '/images/avatar.svg'" alt="会员头像" />
      <div>
        <h1>{{ form.nickname || '邻里会员' }}</h1>
        <p>{{ maskPhone(form.phone) }} · {{ user.profile?.memberLevel || '青芽会员' }}</p>
      </div>
    </section>

    <section class="edit-card">
      <h2>基本资料</h2>
      <van-cell-group inset>
        <van-field v-model="form.nickname" label="昵称" maxlength="20" show-word-limit placeholder="请输入昵称" />
        <van-field :model-value="form.phone" label="手机号" disabled placeholder="手机号不可修改" />
        <van-field :model-value="user.profile?.memberLevel || '青芽会员'" label="会员等级" disabled />
      </van-cell-group>
      <p class="edit-hint">头像与会员等级当前由系统维护，本期仅开放昵称修改。</p>
    </section>

    <button class="save-button" :disabled="submitting" @click="run">
      {{ submitting ? '保存中…' : '保存资料' }}
    </button>
  </div>
</template>

<style scoped>
.profile-edit-page {
  padding-bottom: 90px;
}

.profile-preview {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 20px;
  border-radius: 20px;
  background: linear-gradient(135deg, #193c2a, #2f7a4c);
  color: #fff;
}

.profile-preview img {
  width: 62px;
  height: 62px;
  border: 2px solid rgba(255, 255, 255, 0.6);
  border-radius: 20px;
  object-fit: cover;
}

.profile-preview h1 {
  margin: 0 0 5px;
  font-size: 19px;
}

.profile-preview p {
  margin: 0;
  color: rgba(255, 255, 255, 0.68);
  font-size: 11px;
}

.edit-card {
  margin-top: 16px;
}

.edit-card h2 {
  margin: 0 0 10px;
  font-size: 15px;
}

.edit-card :deep(.van-cell-group--inset) {
  margin: 0;
  overflow: hidden;
  border-radius: 16px;
}

.edit-hint {
  margin: 10px 3px 0;
  color: #8a948c;
  font-size: 10px;
  line-height: 1.6;
}

.save-button {
  position: fixed;
  right: 16px;
  bottom: calc(16px + env(safe-area-inset-bottom));
  left: 16px;
  max-width: 448px;
  margin: 0 auto;
  padding: 13px;
  border: 0;
  border-radius: 15px;
  background: #1f8a4c;
  color: #fff;
  font-size: 14px;
  font-weight: 700;
}

.save-button:disabled {
  opacity: 0.6;
}
</style>
