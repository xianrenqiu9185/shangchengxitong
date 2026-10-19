<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createProduct,
  deleteProduct,
  getProducts,
  getResource,
  updateProduct,
  updateProductStatus,
} from '@/api/admin'
import { formatPrice } from '@/utils/format'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const categories = ref([])
const query = reactive({ keyword: '', status: '', pageNum: 1, pageSize: 10 })
const dialogVisible = ref(false)
const editingId = ref(null)
const form = reactive({
  name: '',
  subtitle: '',
  categoryId: null,
  priceYuan: '',
  stock: 0,
  image: '/images/apple.svg',
  status: 1,
})

async function load() {
  loading.value = true
  try {
    const data = await getProducts(query)
    rows.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, {
    name: '',
    subtitle: '',
    categoryId: categories.value[0]?.id || null,
    priceYuan: '',
    stock: 0,
    image: '/images/apple.svg',
    status: 1,
  })
}

function openCreate() {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  Object.assign(form, {
    name: row.name,
    subtitle: row.subtitle,
    categoryId: row.categoryId,
    priceYuan: (row.price / 100).toFixed(2),
    stock: row.stock,
    image: row.image,
    status: row.status,
  })
  dialogVisible.value = true
}

async function save() {
  if (!form.name || !form.categoryId || !form.priceYuan) {
    ElMessage.warning('请填写商品名称、分类和价格')
    return
  }
  const category = categories.value.find((item) => item.id === Number(form.categoryId))
  const payload = {
    name: form.name,
    subtitle: form.subtitle,
    categoryId: Number(form.categoryId),
    categoryName: category?.name || '',
    price: Math.round(Number(form.priceYuan) * 100),
    stock: Number(form.stock),
    image: form.image,
    status: Number(form.status),
  }
  if (editingId.value) {
    await updateProduct(editingId.value, payload)
    ElMessage.success('商品已更新')
  } else {
    await createProduct(payload)
    ElMessage.success('商品已创建')
  }
  dialogVisible.value = false
  await load()
}

async function toggleStatus(row) {
  try {
    await updateProductStatus(row.id, row.status)
    ElMessage.success(row.status ? '商品已上架' : '商品已下架')
  } catch {
    row.status = row.status ? 0 : 1
  }
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除商品「${row.name}」吗？`, '删除商品', { type: 'warning' })
  await deleteProduct(row.id)
  ElMessage.success('商品已删除')
  await load()
}

onMounted(async () => {
  const categoryPage = await getResource('/admin/categories')
  categories.value = categoryPage.list
  await load()
})
</script>

<template>
  <div class="page-shell">
    <div class="page-title-row">
      <div>
        <h1>商品管理</h1>
        <p>维护商品基础信息、价格、库存与上下架状态。</p>
      </div>
      <el-button v-perm="'product:goods:manage'" type="primary" @click="openCreate">＋ 新增商品</el-button>
    </div>

    <section class="panel filter-panel">
      <el-input v-model="query.keyword" clearable placeholder="搜索商品名称或分类" @keyup.enter="query.pageNum = 1; load()" />
      <el-select v-model="query.status" placeholder="上架状态" clearable>
        <el-option label="已上架" :value="1" />
        <el-option label="已下架" :value="0" />
      </el-select>
      <el-button type="primary" @click="query.pageNum = 1; load()">查询</el-button>
      <el-button @click="query.keyword = ''; query.status = ''; query.pageNum = 1; load()">重置</el-button>
    </section>

    <section class="panel table-panel">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column label="商品" min-width="240">
          <template #default="{ row }">
            <div class="product-cell">
              <img :src="row.image" :alt="row.name" />
              <div><b>{{ row.name }}</b><span>{{ row.subtitle }}</span></div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="categoryName" label="分类" width="120" />
        <el-table-column label="售价" width="120">
          <template #default="{ row }"><b class="price">¥{{ formatPrice(row.price) }}</b></template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="90" />
        <el-table-column prop="sales" label="销量" width="90" />
        <el-table-column label="上架" width="90">
          <template #default="{ row }">
            <el-switch v-model="row.status" :active-value="1" :inactive-value="0" @change="toggleStatus(row)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          layout="total, prev, pager, next"
          :total="total"
          @current-change="load"
        />
      </div>
    </section>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑商品' : '新增商品'" width="560px" destroy-on-close>
      <el-form label-position="top">
        <el-form-item label="商品名称" required>
          <el-input v-model="form.name" placeholder="如：红富士苹果" />
        </el-form-item>
        <el-form-item label="副标题">
          <el-input v-model="form.subtitle" placeholder="如：冰糖心 · 脆甜多汁" />
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="分类" required>
            <el-select v-model="form.categoryId" placeholder="请选择分类">
              <el-option v-for="category in categories" :key="category.id" :label="category.name" :value="category.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="售价（元）" required>
            <el-input v-model="form.priceYuan" placeholder="29.90" />
          </el-form-item>
          <el-form-item label="可用库存">
            <el-input-number v-model="form.stock" :min="0" :max="99999" controls-position="right" />
          </el-form-item>
          <el-form-item label="上架状态">
            <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="上架" inactive-text="下架" />
          </el-form-item>
        </div>
        <el-form-item label="商品图片路径">
          <el-input v-model="form.image" placeholder="/images/apple.svg" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.product-cell {
  display: flex;
  align-items: center;
  gap: 11px;
}

.product-cell img {
  width: 44px;
  height: 44px;
  border-radius: 11px;
  object-fit: cover;
}

.product-cell div {
  display: grid;
  gap: 3px;
}

.product-cell b {
  font-size: 13px;
}

.product-cell span {
  color: #8a958d;
  font-size: 10px;
}

.price {
  color: #e55039;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 14px;
}

.form-grid :deep(.el-select),
.form-grid :deep(.el-input-number) {
  width: 100%;
}

@media (max-width: 640px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
