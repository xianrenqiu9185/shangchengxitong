<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { auditAfterSale, getAfterSales } from '@/api/admin'
import { afterSaleStatusMap } from '@/utils/dict'
import { formatPrice } from '@/utils/format'

const rows = ref([])
const loading = ref(false)
const query = reactive({ status: '' })
const dialogVisible = ref(false)
const current = ref(null)
const auditForm = reactive({ approved: true, remark: '' })

async function load() {
  loading.value = true
  try {
    const data = await getAfterSales(query)
    rows.value = data.list
  } finally {
    loading.value = false
  }
}

function openAudit(row, approved) {
  current.value = row
  auditForm.approved = approved
  auditForm.remark = approved ? '审核通过，按原支付方式退回' : '凭证不足，暂不支持退款'
  dialogVisible.value = true
}

async function submitAudit() {
  await auditAfterSale(current.value.saleNo, auditForm)
  ElMessage.success(auditForm.approved ? '已同意售后并触发退款' : '已驳回售后申请')
  dialogVisible.value = false
  await load()
}

onMounted(load)
</script>

<template>
  <div class="page-shell">
    <div class="page-title-row">
      <div>
        <h1>售后管理</h1>
        <p>审核退款与退货申请，同意后由后端编排沙箱退款。</p>
      </div>
      <el-tag effect="plain" round>待审核 {{ rows.filter((item) => item.status === 'PENDING').length }}</el-tag>
    </div>

    <section class="panel filter-panel">
      <el-select v-model="query.status" placeholder="售后状态" clearable>
        <el-option label="待审核" value="PENDING" />
        <el-option label="已同意" value="APPROVED" />
        <el-option label="已驳回" value="REJECTED" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
      <el-button @click="query.status = ''; load()">重置</el-button>
    </section>

    <section class="panel table-panel">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="saleNo" label="售后单号" min-width="160" />
        <el-table-column prop="orderNo" label="订单号" min-width="160" />
        <el-table-column prop="customer" label="会员" width="110" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">{{ row.type === 'RETURN' ? '退货退款' : '仅退款' }}</template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" min-width="210" />
        <el-table-column label="金额" width="110">
          <template #default="{ row }"><b class="price">¥{{ formatPrice(row.amount) }}</b></template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="afterSaleStatusMap[row.status]?.type || 'info'" effect="plain" round>
              {{ afterSaleStatusMap[row.status]?.label || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="申请时间" min-width="150" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING'">
              <el-button v-perm="'after:sale:audit'" link type="success" @click="openAudit(row, true)">同意</el-button>
              <el-button v-perm="'after:sale:audit'" link type="danger" @click="openAudit(row, false)">驳回</el-button>
            </template>
            <span v-else class="muted">已处理</span>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="dialogVisible" :title="auditForm.approved ? '同意售后' : '驳回售后'" width="460px">
      <el-form label-position="top">
        <el-form-item label="售后单号"><el-input :model-value="current?.saleNo" disabled /></el-form-item>
        <el-form-item label="审核意见">
          <el-input v-model="auditForm.remark" type="textarea" :rows="3" />
        </el-form-item>
        <el-alert
          v-if="auditForm.approved"
          title="同意后将调用沙箱退款接口，退款成功会更新售后与订单状态。"
          type="warning"
          :closable="false"
          show-icon
        />
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button :type="auditForm.approved ? 'primary' : 'danger'" @click="submitAudit">
          {{ auditForm.approved ? '确认同意' : '确认驳回' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.price {
  color: #e55039;
}
</style>
