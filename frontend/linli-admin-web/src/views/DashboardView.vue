<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { getDashboard } from '@/api/admin'
import { orderStatusMap } from '@/utils/dict'
import { formatPrice } from '@/utils/format'

echarts.use([LineChart, GridComponent, TooltipComponent, CanvasRenderer])

const data = ref(null)
const chartEl = ref(null)
let chart = null

const stats = computed(() => [
  { label: '今日订单', value: data.value?.todayOrders ?? 0, unit: '单', icon: '🧾', tone: 'green', hint: '较昨日 +8.2%' },
  { label: '今日销售额', value: `¥${formatPrice(data.value?.todaySales ?? 0)}`, unit: '', icon: '💰', tone: 'orange', hint: '含到家与自提' },
  { label: '待发货', value: data.value?.pendingShipment ?? 0, unit: '单', icon: '📦', tone: 'blue', hint: '请及时处理' },
  { label: '低库存商品', value: data.value?.lowStock ?? 0, unit: '项', icon: '⚠️', tone: 'red', hint: '库存 ≤ 30' },
])

function renderChart() {
  if (!chartEl.value || !data.value) return
  chart ??= echarts.init(chartEl.value)
  const trend = data.value.orderTrend || []
  chart.setOption({
    grid: { left: 16, right: 20, top: 28, bottom: 12, containLabel: true },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: trend.map((item) => item.date),
      axisLine: { lineStyle: { color: '#dce4dd' } },
      axisTick: { show: false },
      axisLabel: { color: '#7b887f' },
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#eef2ee' } },
      axisLabel: { color: '#9aa59d' },
    },
    series: [
      {
        name: '订单量',
        type: 'line',
        smooth: true,
        symbolSize: 7,
        data: trend.map((item) => item.orders),
        lineStyle: { width: 3, color: '#1f8a4c' },
        itemStyle: { color: '#1f8a4c' },
        areaStyle: {
          color: 'rgba(31, 138, 76, 0.18)',
        },
      },
    ],
  })
}

function resizeChart() {
  chart?.resize()
}

onMounted(async () => {
  data.value = await getDashboard()
  requestAnimationFrame(renderChart)
  window.addEventListener('resize', resizeChart)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
  chart?.dispose()
})
</script>

<template>
  <div class="page-shell dashboard-page">
    <div class="page-title-row">
      <div>
        <h1>经营看板</h1>
        <p>今日经营概览与待处理事项，数据来自后台聚合接口。</p>
      </div>
      <el-button plain @click="data = null; $router.go(0)">刷新数据</el-button>
    </div>

    <section class="stat-grid">
      <article v-for="stat in stats" :key="stat.label" class="stat-card" :class="stat.tone">
        <div class="stat-icon">{{ stat.icon }}</div>
        <div>
          <span>{{ stat.label }}</span>
          <strong>{{ stat.value }}<small>{{ stat.unit }}</small></strong>
          <em>{{ stat.hint }}</em>
        </div>
      </article>
    </section>

    <section class="dashboard-grid">
      <div class="panel chart-panel">
        <div class="panel-head">
          <div><h2>近 7 日订单趋势</h2><p>每日成功提交订单量</p></div>
          <el-tag effect="plain" round>实时</el-tag>
        </div>
        <div ref="chartEl" class="chart"></div>
      </div>

      <div class="panel low-stock-panel">
        <div class="panel-head">
          <div><h2>库存预警</h2><p>可用库存低于阈值</p></div>
          <el-button text type="primary" @click="$router.push('/inventory')">查看全部</el-button>
        </div>
        <div class="stock-list">
          <article v-for="product in data?.lowStockProducts || []" :key="product.id">
            <img :src="product.image" :alt="product.name" />
            <div><b>{{ product.name }}</b><span>{{ product.categoryName }}</span></div>
            <em>{{ product.stock }} 件</em>
          </article>
          <p v-if="!data?.lowStockProducts?.length" class="empty-text">暂无低库存商品</p>
        </div>
      </div>
    </section>

    <section class="panel recent-panel">
      <div class="panel-head">
        <div><h2>最近订单</h2><p>最新提交与履约中的订单</p></div>
        <el-button text type="primary" @click="$router.push('/orders')">订单管理</el-button>
      </div>
      <el-table :data="data?.recentOrders || []" stripe>
        <el-table-column prop="orderNo" label="订单号" min-width="170" />
        <el-table-column prop="customer" label="会员" width="110" />
        <el-table-column label="金额" width="120">
          <template #default="{ row }">¥{{ formatPrice(row.payAmount) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="orderStatusMap[row.status]?.type || 'info'" effect="plain" round>
              {{ orderStatusMap[row.status]?.label || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="下单时间" min-width="160" />
      </el-table>
    </section>
  </div>
</template>

<style scoped>
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.stat-card {
  display: flex;
  gap: 14px;
  align-items: center;
  padding: 18px;
  border: 1px solid #e7ede7;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 8px 22px rgba(24, 48, 33, 0.04);
}

.stat-icon {
  display: grid;
  width: 45px;
  height: 45px;
  border-radius: 14px;
  background: #edf7f0;
  place-items: center;
  font-size: 21px;
}

.stat-card.orange .stat-icon { background: #fff2e8; }
.stat-card.blue .stat-icon { background: #eaf2ff; }
.stat-card.red .stat-icon { background: #fff0ef; }

.stat-card > div:last-child {
  display: grid;
  gap: 3px;
}

.stat-card span {
  color: #7b887f;
  font-size: 11px;
}

.stat-card strong {
  font-size: 24px;
  letter-spacing: -0.02em;
}

.stat-card small {
  margin-left: 3px;
  color: #7b887f;
  font-size: 10px;
  font-weight: 400;
}

.stat-card em {
  color: #9aa59d;
  font-size: 9px;
  font-style: normal;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(300px, 0.65fr);
  gap: 14px;
  margin-top: 14px;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 17px 19px 0;
}

.panel-head h2 {
  margin: 0 0 3px;
  font-size: 15px;
}

.panel-head p {
  margin: 0;
  color: #8a958d;
  font-size: 10px;
}

.chart-panel,
.low-stock-panel,
.recent-panel {
  overflow: hidden;
}

.chart {
  height: 268px;
  padding: 0 8px 10px;
}

.stock-list {
  display: grid;
  gap: 2px;
  padding: 12px 19px 17px;
}

.stock-list article {
  display: grid;
  grid-template-columns: 38px 1fr auto;
  gap: 10px;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f1f4f1;
}

.stock-list article:last-child {
  border-bottom: 0;
}

.stock-list img {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  object-fit: cover;
}

.stock-list div {
  display: grid;
  gap: 2px;
}

.stock-list b {
  font-size: 12px;
}

.stock-list span {
  color: #9aa59d;
  font-size: 9px;
}

.stock-list em {
  color: #e55039;
  font-size: 11px;
  font-style: normal;
  font-weight: 600;
}

.empty-text {
  margin: 12px 0;
  color: #9aa59d;
  font-size: 11px;
  text-align: center;
}

.recent-panel {
  margin-top: 14px;
}

.recent-panel :deep(.el-table) {
  margin-top: 12px;
}

@media (max-width: 1180px) {
  .stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .stat-grid {
    grid-template-columns: 1fr;
  }
}
</style>
