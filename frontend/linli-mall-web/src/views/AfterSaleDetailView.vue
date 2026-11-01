<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getAfterSale } from '@/api/afterSale'
import { formatDate } from '@/utils/format'
import AppHeader from '@/components/AppHeader.vue'

const route = useRoute()
const record = ref(null)

const statusMeta = computed(() => {
  const map = {
    PENDING: { label: '等待平台审核', color: '#ff8a3d', desc: '平台会在 24 小时内处理你的申请' },
    APPROVED: { label: '审核已通过', color: '#1f8a4c', desc: '退款将按原支付方式退回' },
    REJECTED: { label: '申请已驳回', color: '#8a948c', desc: '如有疑问可联系门店客服' },
  }
  return map[record.value?.status] || { label: record.value?.status || '', color: '#8a948c', desc: '' }
})

onMounted(async () => {
  record.value = await getAfterSale(route.params.saleNo)
})
</script>

<template>
  <div class="page after-sale-detail-page">
    <AppHeader title="售后详情" />
    <template v-if="record">
      <section class="status-card" :style="{ background: `linear-gradient(135deg, ${statusMeta.color}, ${statusMeta.color}cc)` }">
        <span>售后单 {{ record.saleNo }}</span>
        <h1>{{ statusMeta.label }}</h1>
        <p>{{ statusMeta.desc }}</p>
      </section>

      <section class="detail-card">
        <h2>申请信息</h2>
        <div class="info-row"><span>售后类型</span><b>{{ record.type === 'RETURN' ? '退货退款' : '仅退款' }}</b></div>
        <div class="info-row"><span>关联订单</span><b>{{ record.orderNo }}</b></div>
        <div class="info-row"><span>申请时间</span><b>{{ formatDate(record.createdAt) }}</b></div>
        <div class="info-row"><span>申请原因</span><b>{{ record.reason }}</b></div>
        <p v-if="record.description" class="description">{{ record.description }}</p>
      </section>

      <section class="detail-card">
        <h2>处理进度</h2>
        <div class="timeline">
          <div class="step done"><i>✓</i><div><b>提交售后申请</b><span>{{ record.createdAt }}</span></div></div>
          <div class="step" :class="{ done: record.status !== 'PENDING' }"><i>{{ record.status === 'PENDING' ? '2' : '✓' }}</i><div><b>平台审核</b><span>{{ record.status === 'PENDING' ? '等待处理中' : statusMeta.label }}</span></div></div>
          <div class="step" :class="{ done: record.status === 'APPROVED' }"><i>{{ record.status === 'APPROVED' ? '✓' : '3' }}</i><div><b>退款到账</b><span>{{ record.status === 'APPROVED' ? '退款已发起' : '审核通过后处理' }}</span></div></div>
        </div>
      </section>

      <section class="tip-card">
        <b>需要帮助？</b>
        <p>请保留商品与包装，平台审核通过后会按原支付方式退款。当前为课程实验演示环境。</p>
      </section>
    </template>
  </div>
</template>

<style scoped>
.after-sale-detail-page {
  padding-bottom: 30px;
}

.status-card {
  padding: 23px 18px;
  border-radius: 22px;
  color: #fff;
  box-shadow: 0 16px 34px rgba(30, 62, 44, 0.16);
}

.status-card span {
  font-size: 10px;
  opacity: 0.8;
}

.status-card h1 {
  margin: 7px 0 5px;
  font-size: 21px;
}

.status-card p {
  margin: 0;
  font-size: 11px;
  opacity: 0.78;
}

.detail-card {
  margin-top: 13px;
  padding: 16px;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(30, 62, 44, 0.06);
}

.detail-card h2 {
  margin: 0 0 12px;
  font-size: 15px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  padding: 6px 0;
  color: #7b857e;
  font-size: 12px;
}

.info-row b {
  color: #1c2420;
  font-weight: 500;
  text-align: right;
}

.description {
  margin: 9px 0 0;
  padding: 11px;
  border-radius: 11px;
  background: #f7f8f5;
  color: #56625a;
  font-size: 12px;
  line-height: 1.7;
}

.timeline {
  display: grid;
  gap: 16px;
}

.step {
  display: grid;
  grid-template-columns: 26px 1fr;
  gap: 10px;
  align-items: start;
  opacity: 0.45;
}

.step.done {
  opacity: 1;
}

.step i {
  display: grid;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: #eaf7ee;
  color: #1f8a4c;
  place-items: center;
  font-size: 11px;
  font-style: normal;
  font-weight: 700;
}

.step:not(.done) i {
  background: #f2f1ec;
  color: #9aa29c;
}

.step div {
  display: grid;
  gap: 3px;
}

.step b {
  font-size: 12px;
}

.step span {
  color: #8a948c;
  font-size: 10px;
}

.tip-card {
  margin-top: 13px;
  padding: 15px;
  border-radius: 16px;
  background: #fffaf0;
  color: #7c6947;
}

.tip-card b {
  font-size: 12px;
}

.tip-card p {
  margin: 6px 0 0;
  font-size: 11px;
  line-height: 1.7;
}
</style>
