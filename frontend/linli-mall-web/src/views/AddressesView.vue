<script setup>
import { onMounted, reactive, ref } from 'vue'
import { showConfirmDialog, showToast } from 'vant'
import { createAddress, deleteAddress, getAddresses, updateAddress } from '@/api/user'
import AppHeader from '@/components/AppHeader.vue'

const addresses = ref([])
const showForm = ref(false)
const editingId = ref(null)
const form = reactive({ receiver: '', phone: '', province: '上海市', city: '上海市', district: '浦东新区', detail: '', tag: '家', isDefault: false })

async function load() {
  addresses.value = await getAddresses()
}

function openCreate() {
  editingId.value = null
  Object.assign(form, { receiver: '', phone: '', province: '上海市', city: '上海市', district: '浦东新区', detail: '', tag: '家', isDefault: false })
  showForm.value = true
}

function openEdit(address) {
  editingId.value = address.id
  Object.assign(form, address)
  showForm.value = true
}

async function save() {
  if (!form.receiver || !form.phone || !form.detail) {
    showToast('请完整填写收货信息')
    return
  }
  if (editingId.value) {
    await updateAddress(editingId.value, { ...form })
  } else {
    await createAddress({ ...form })
  }
  showForm.value = false
  showToast('地址已保存')
  await load()
}

async function remove(address) {
  await showConfirmDialog({ title: '删除地址', message: `确认删除「${address.receiver}」的收货地址吗？` })
  await deleteAddress(address.id)
  showToast('地址已删除')
  await load()
}

onMounted(load)
</script>

<template>
  <div class="page address-page">
    <AppHeader title="收货地址" />
    <section v-if="addresses.length" class="address-list">
      <article v-for="address in addresses" :key="address.id" class="address-card">
        <div class="address-top">
          <b>{{ address.receiver }}</b>
          <span>{{ address.phone }}</span>
          <em>{{ address.tag }}</em>
          <i v-if="address.isDefault">默认</i>
        </div>
        <p>{{ address.province }}{{ address.city }}{{ address.district }}{{ address.detail }}</p>
        <div class="address-actions">
          <button @click="openEdit(address)">编辑</button>
          <button @click="remove(address)">删除</button>
        </div>
      </article>
    </section>
    <div v-else class="empty-block">
      <span class="emoji">📍</span>
      还没有收货地址
    </div>
    <button class="add-address" @click="openCreate">＋ 新增收货地址</button>

    <van-popup v-model:show="showForm" position="bottom" round>
      <div class="address-form">
        <h2>{{ editingId ? '编辑地址' : '新增地址' }}</h2>
        <van-cell-group inset>
          <van-field v-model="form.receiver" label="收货人" placeholder="请输入姓名" />
          <van-field v-model="form.phone" label="手机号" type="tel" maxlength="11" placeholder="请输入手机号" />
          <van-field v-model="form.province" label="省份" placeholder="省份" />
          <van-field v-model="form.city" label="城市" placeholder="城市" />
          <van-field v-model="form.district" label="区县" placeholder="区县" />
          <van-field v-model="form.detail" label="详细地址" type="textarea" rows="2" autosize placeholder="楼栋、门牌号或自提点" />
          <van-field v-model="form.tag" label="标签" placeholder="家 / 公司 / 自提" />
          <van-cell title="设为默认地址">
            <template #right-icon><van-switch v-model="form.isDefault" size="20px" /></template>
          </van-cell>
        </van-cell-group>
        <button class="save-address" @click="save">保存地址</button>
      </div>
    </van-popup>
  </div>
</template>

<style scoped>
.address-page {
  padding-bottom: 90px;
}

.address-list {
  display: grid;
  gap: 12px;
}

.address-card {
  padding: 15px;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(30, 62, 44, 0.06);
}

.address-top {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.address-top span {
  color: #7b857e;
}

.address-top em,
.address-top i {
  padding: 2px 7px;
  border-radius: 999px;
  background: #eaf7ee;
  color: #1f8a4c;
  font-size: 9px;
  font-style: normal;
}

.address-top i {
  margin-left: auto;
  background: #fff0e8;
  color: #ff8a3d;
}

.address-card p {
  margin: 9px 0 12px;
  color: #56625a;
  font-size: 12px;
  line-height: 1.6;
}

.address-actions {
  display: flex;
  justify-content: flex-end;
  gap: 14px;
  padding-top: 10px;
  border-top: 1px solid #f2f1ec;
}

.address-actions button {
  border: 0;
  background: transparent;
  color: #7b857e;
  font-size: 11px;
}

.add-address {
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

.address-form {
  padding: 20px 16px calc(20px + env(safe-area-inset-bottom));
}

.address-form h2 {
  margin: 0 0 14px;
  font-size: 18px;
}

.address-form :deep(.van-cell-group--inset) {
  margin: 0;
}

.save-address {
  width: 100%;
  margin-top: 16px;
  padding: 13px;
  border: 0;
  border-radius: 14px;
  background: #1f8a4c;
  color: #fff;
  font-size: 14px;
  font-weight: 700;
}
</style>
