<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getAfterSales, createAfterSale } from '@/api/afterSale'
import { getOrders } from '@/api/order'
import AppHeader from '@/components/AppHeader.vue'

const records = ref([])
const router = useRouter()
const orders = ref([])
const showForm = ref(false)
const form = reactive({ orderNo: '', type: 'REFUND', reason: '', description: '' })

async function load() {
  const page = await getAfterSales()
  records.value = page.list
}

async function openForm() {
  const page = await getOrders({ pageNum: 1, pageSize: 50 })
  orders.value = page.list.filter((order) => ['PENDING_SHIPMENT', 'PENDING_RECEIPT', 'COMPLETED'].includes(order.status))
  form.orderNo = orders.value[0]?.orderNo || ''
  showForm.value = true
}

async function submit() {
  if (!form.orderNo || !form.reason) {
    showToast('请选择订单并填写售后原因')
    return
  }
  await createAfterSale({ ...form })
  showForm.value = false
  showToast('售后申请已提交')
  await load()
}

onMounted(load)
</script>

<template>
  <div class="page after-sale-page">
    <AppHeader title="售后服务" />
    <section class="after-sale-intro">
      <span>🛠️</span>
      <div><h1>售后无忧</h1><p>退款、退货申请与处理进度都在这里</p></div>
    </section>

    <div v-if="records.length" class="after-sale-list">
      <article v-for="record in records" :key="record.saleNo" class="after-sale-card">
        <div><b>{{ record.type === 'RETURN' ? '退货退款' : '仅退款' }}</b><em>{{ record.status === 'PENDING' ? '待审核' : record.status }}</em></div>
        <p>{{ record.reason }}</p>
        <div class="after-sale-foot">
          <span>订单 {{ record.orderNo }} · {{ record.createdAt }}</span>
          <button @click="router.push(`/after-sales/${record.saleNo}`)">查看进度 ›</button>
        </div>
      </article>
    </div>
    <div v-else class="empty-block">
      <span class="emoji">🧾</span>
      暂无售后记录
    </div>

    <button class="apply-button" @click="openForm">＋ 申请售后</button>

    <van-popup v-model:show="showForm" position="bottom" round>
      <div class="after-sale-form">
        <h2>申请售后</h2>
        <van-cell-group inset>
          <van-field label="选择订单">
            <template #input>
              <van-picker
                :columns="orders.map((order) => ({ text: `${order.orderNo} · ¥${(order.payAmount / 100).toFixed(2)}`, value: order.orderNo }))"
                @confirm="({ selectedOptions }) => (form.orderNo = selectedOptions[0]?.value)"
              />
            </template>
          </van-field>
          <van-field label="售后类型">
            <template #input>
              <van-radio-group v-model="form.type" direction="horizontal">
                <van-radio name="REFUND">仅退款</van-radio>
                <van-radio name="RETURN">退货退款</van-radio>
              </van-radio-group>
            </template>
          </van-field>
          <van-field v-model="form.reason" label="售后原因" placeholder="如：商品破损、少件" />
          <van-field v-model="form.description" label="补充说明" type="textarea" rows="2" autosize placeholder="请描述具体情况" />
        </van-cell-group>
        <button class="submit-after-sale" @click="submit">提交申请</button>
      </div>
    </van-popup>
  </div>
</template>

<style scoped>
.after-sale-page {
  padding-bottom: 90px;
}

.after-sale-intro {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px;
  border-radius: 20px;
  background: linear-gradient(135deg, #eaf7ee, #d8f0de);
}

.after-sale-intro > span {
  font-size: 38px;
}

.after-sale-intro h1 {
  margin: 0 0 4px;
  font-size: 18px;
}

.after-sale-intro p {
  margin: 0;
  color: #5d6a61;
  font-size: 11px;
}

.after-sale-list {
  display: grid;
  gap: 12px;
  margin-top: 16px;
}

.after-sale-card {
  padding: 15px;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(30, 62, 44, 0.06);
}

.after-sale-card div {
  display: flex;
  justify-content: space-between;
}

.after-sale-card b {
  font-size: 13px;
}

.after-sale-card em {
  color: #ff8a3d;
  font-size: 11px;
  font-style: normal;
}

.after-sale-card p {
  margin: 9px 0 7px;
  color: #56625a;
  font-size: 12px;
}

.after-sale-card span {
  color: #9aa29c;
  font-size: 10px;
}

.after-sale-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.after-sale-foot button {
  border: 0;
  background: transparent;
  color: #1f8a4c;
  font-size: 10px;
  font-weight: 700;
}

.apply-button {
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

.after-sale-form {
  max-height: 80vh;
  padding: 20px 16px calc(20px + env(safe-area-inset-bottom));
  overflow-y: auto;
}

.after-sale-form h2 {
  margin: 0 0 14px;
  font-size: 18px;
}

.after-sale-form :deep(.van-cell-group--inset) {
  margin: 0;
}

.submit-after-sale {
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
