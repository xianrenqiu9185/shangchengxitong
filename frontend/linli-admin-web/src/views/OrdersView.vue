<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { closeOrder, getOrder, getOrders, shipOrder } from '@/api/admin'
import { deliveryTypes, orderStatusMap } from '@/utils/dict'
import { formatPrice } from '@/utils/format'

const rows = ref([])
const loading = ref(false)
const query = reactive({ keyword: '', status: '' })
const drawerVisible = ref(false)
const current = ref(null)

const statusOptions = Object.entries(orderStatusMap).map(([value, item]) => ({ value, label: item.label }))

async function load() {
  loading.value = true
  try {
    const data = await getOrders(query)
    rows.value = data.list
  } finally {
    loading.value = false
  }
}

async function openDetail(row) {
  current.value = await getOrder(row.orderNo)
  drawerVisible.value = true
}

async function ship(row) {
  await ElMessageBox.confirm(`确认对订单 ${row.orderNo} 执行发货/备货完成？`, '确认发货', { type: 'warning' })
  await shipOrder(row.orderNo)
  ElMessage.success('订单已进入待收货')
  await load()
}

async function close(row) {
  await ElMessageBox.confirm(`确认关闭订单 ${row.orderNo}？关闭将释放库存并按规则退款。`, '关闭订单', { type: 'warning' })
  await closeOrder(row.orderNo)
  ElMessage.success('订单已关闭')
  await load()
}

onMounted(load)
</script>

<template>
  <div class="page-shell">
    <div class="page-title-row">
      <div>
        <h1>订单管理</h1>
        <p>查询全量订单，处理发货、关闭与履约异常。</p>
      </div>
      <el-tag effect="plain" round>共 {{ rows.length }} 笔</el-tag>
    </div>

    <section class="panel filter-panel">
      <el-input v-model="query.keyword" clearable placeholder="订单号 / 会员 / 手机号" @keyup.enter="load" />
      <el-select v-model="query.status" placeholder="订单状态" clearable>
        <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
      <el-button @click="query.keyword = ''; query.status = ''; load()">重置</el-button>
    </section>

    <section class="panel table-panel">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="orderNo" label="订单号" min-width="180" />
        <el-table-column label="会员" width="150">
          <template #default="{ row }">
            <div class="member-cell"><b>{{ row.customer }}</b><span>{{ row.phone }}</span></div>
          </template>
        </el-table-column>
        <el-table-column label="实付" width="120">
          <template #default="{ row }"><b class="price">¥{{ formatPrice(row.payAmount) }}</b></template>
        </el-table-column>
        <el-table-column label="履约" width="110">
          <template #default="{ row }">{{ deliveryTypes[row.deliveryType] }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="orderStatusMap[row.status]?.type || 'info'" effect="plain" round>
              {{ orderStatusMap[row.status]?.label || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="下单时间" min-width="160" />
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button
              v-if="row.status === 'PAID'"
              v-perm="'trade:order:ship'"
              link
              type="success"
              @click="ship(row)"
            >
              发货
            </el-button>
            <el-button
              v-if="!['COMPLETED', 'CANCELLED'].includes(row.status)"
              v-perm="'trade:order:close'"
              link
              type="danger"
              @click="close(row)"
            >
              关闭
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-drawer v-model="drawerVisible" title="订单详情" size="520px">
      <template v-if="current">
        <div class="order-summary">
          <div><span>订单号</span><b>{{ current.orderNo }}</b></div>
          <div><span>状态</span><el-tag :type="orderStatusMap[current.status]?.type" effect="plain">{{ orderStatusMap[current.status]?.label }}</el-tag></div>
          <div><span>实付</span><b class="price">¥{{ formatPrice(current.payAmount) }}</b></div>
        </div>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="会员">{{ current.customer }} · {{ current.phone }}</el-descriptions-item>
          <el-descriptions-item label="履约方式">{{ deliveryTypes[current.deliveryType] }}</el-descriptions-item>
          <el-descriptions-item label="收货地址">{{ current.address }}</el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ current.createdAt }}</el-descriptions-item>
        </el-descriptions>
        <h3 class="drawer-title">商品清单</h3>
        <div class="order-items">
          <article v-for="item in current.items" :key="item.productId">
            <img :src="item.image" :alt="item.name" />
            <div><b>{{ item.name }}</b><span>{{ item.unit }} × {{ item.quantity }}</span></div>
            <strong>¥{{ formatPrice(item.price * item.quantity) }}</strong>
          </article>
        </div>
        <div class="amount-box">
          <div><span>商品金额</span><b>¥{{ formatPrice(current.goodsAmount) }}</b></div>
          <div><span>配送费</span><b>¥{{ formatPrice(current.deliveryFee) }}</b></div>
          <div><span>优惠</span><b class="discount">-¥{{ formatPrice(current.discountAmount) }}</b></div>
          <div class="total"><span>实付</span><strong>¥{{ formatPrice(current.payAmount) }}</strong></div>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped>
.member-cell {
  display: grid;
  gap: 2px;
}

.member-cell span {
  color: #8a958d;
  font-size: 10px;
}

.price {
  color: #e55039;
}

.order-summary {
  display: grid;
  gap: 8px;
  margin-bottom: 16px;
  padding: 15px;
  border-radius: 13px;
  background: #f3f8f4;
}

.order-summary > div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
}

.order-summary span {
  color: #7b887f;
}

.drawer-title {
  margin: 20px 0 9px;
  font-size: 14px;
}

.order-items {
  display: grid;
  gap: 4px;
}

.order-items article {
  display: grid;
  grid-template-columns: 46px 1fr auto;
  gap: 10px;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f0f3f0;
}

.order-items img {
  width: 46px;
  height: 46px;
  border-radius: 10px;
  object-fit: cover;
}

.order-items div {
  display: grid;
  gap: 3px;
}

.order-items b {
  font-size: 12px;
}

.order-items span {
  color: #8a958d;
  font-size: 10px;
}

.order-items strong {
  font-size: 12px;
}

.amount-box {
  display: grid;
  gap: 8px;
  margin-top: 14px;
  padding: 15px;
  border-radius: 13px;
  background: #fffaf4;
}

.amount-box > div {
  display: flex;
  justify-content: space-between;
  color: #7b887f;
  font-size: 11px;
}

.amount-box b {
  color: #33443a;
}

.amount-box .discount {
  color: #e55039;
}

.amount-box .total {
  padding-top: 8px;
  border-top: 1px solid #efe6da;
  color: #33443a;
}

.amount-box .total strong {
  color: #e55039;
  font-size: 18px;
}
</style>
