<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createResource, deleteResource, getResource, updateResource } from '@/api/admin'
import request from '@/utils/request'
import { resources } from '@/config/resources'
import { formatPrice } from '@/utils/format'

const route = useRoute()
const resourceKey = computed(() => route.meta.resource)
const resource = computed(() => resources[resourceKey.value])
const rows = ref([])
const loading = ref(false)
const query = reactive({ keyword: '' })
const dialogVisible = ref(false)
const editing = ref(null)
const form = reactive({})
const actionVisible = ref(false)
const actionRow = ref(null)
const actionForm = reactive({})
const permissionOptions = ref([])

const hasCreate = computed(() => Boolean(resource.value?.createFields?.length))
const hasActions = computed(() => Boolean(resource.value?.actions?.length))
const canAdjust = computed(() => resourceKey.value === 'inventory')

async function load() {
  if (!resource.value) return
  loading.value = true
  try {
    const data = await getResource(resource.value.endpoint, { keyword: query.keyword })
    rows.value = Array.isArray(data) ? data : data.list || []
  } finally {
    loading.value = false
  }
}

function rowId(row) {
  return row.id ?? row.key ?? row.productId
}

function defaultsFor(fields) {
  const result = {}
  for (const field of fields || []) {
    result[field.prop] = field.type === 'switch' ? 1 : field.type === 'number' ? 0 : ''
  }
  return result
}

function openCreate() {
  editing.value = null
  Object.keys(form).forEach((key) => delete form[key])
  Object.assign(form, defaultsFor(resource.value.createFields))
  dialogVisible.value = true
}

function openEdit(row) {
  editing.value = row
  Object.keys(form).forEach((key) => delete form[key])
  for (const field of resource.value.createFields || []) {
    form[field.prop] = row[field.prop] ?? defaultsFor([field])[field.prop]
  }
  dialogVisible.value = true
}

async function save() {
  const payload = { ...form }
  if (editing.value) {
    await updateResource(resource.value.endpoint, rowId(editing.value), payload)
    ElMessage.success('已保存')
  } else {
    await createResource(resource.value.endpoint, payload)
    ElMessage.success('已创建')
  }
  dialogVisible.value = false
  await load()
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除这条${resource.value.title}记录吗？`, '删除确认', { type: 'warning' })
  await deleteResource(resource.value.endpoint, rowId(row))
  ElMessage.success('已删除')
  await load()
}

async function loadPermissions() {
  if (permissionOptions.value.length) return
  permissionOptions.value = await request.get('/admin/permissions')
}

async function handleAction(action, row) {
  if (action.key === 'edit') {
    openEdit(row)
    return
  }
  if (action.key === 'delete') {
    await remove(row)
    return
  }
  if (action.key === 'toggleMember') {
    const next = row.status ? 0 : 1
    await ElMessageBox.confirm(`确认${next ? '启用' : '禁用'}会员「${row.nickname}」吗？`, '会员状态', { type: 'warning' })
    await request.put(`/admin/members/${row.id}/status`, { status: next })
    ElMessage.success('会员状态已更新')
    await load()
    return
  }

  actionRow.value = row
  Object.keys(actionForm).forEach((key) => delete actionForm[key])
  if (action.key === 'adjust') actionForm.available = row.available
  if (action.key === 'reply') actionForm.reply = row.reply || ''
  if (action.key === 'permissions') {
    actionForm.permissions = [...(row.permissions || [])]
    await loadPermissions()
  }
  if (action.key === 'editConfig') actionForm.value = row.value
  actionVisible.value = true
}

async function submitAction() {
  if (resourceKey.value === 'inventory') {
    await request.put(`/admin/inventories/${actionRow.value.productId}`, { available: Number(actionForm.available) })
    ElMessage.success('库存已更新')
  } else if (resourceKey.value === 'reviews') {
    await request.post(`/admin/reviews/${actionRow.value.id}/reply`, { reply: actionForm.reply })
    ElMessage.success('回复已提交')
  } else if (resourceKey.value === 'roles') {
    await request.put(`/admin/roles/${actionRow.value.id}/permissions`, { permissions: actionForm.permissions })
    ElMessage.success('权限已更新，下一次请求即时生效')
  } else if (resourceKey.value === 'configs') {
    await request.put(`/admin/configs/${actionRow.value.key}`, { value: actionForm.value })
    ElMessage.success('配置已保存')
  }
  actionVisible.value = false
  await load()
}

function statusLabel(row) {
  if (resourceKey.value === 'members') return row.status ? '正常' : '禁用'
  return row.status ? '启用' : '停用'
}

function actionTitle() {
  if (resourceKey.value === 'inventory') return '调整库存'
  if (resourceKey.value === 'reviews') return '回复评价'
  if (resourceKey.value === 'roles') return '分配权限点'
  if (resourceKey.value === 'configs') return '修改配置'
  return '操作'
}

onMounted(load)
</script>

<template>
  <div v-if="resource" class="page-shell">
    <div class="page-title-row">
      <div>
        <h1>{{ resource.title }}</h1>
        <p>{{ resource.subtitle }}</p>
      </div>
      <el-button v-if="hasCreate" v-perm="resource.perm" type="primary" @click="openCreate">＋ 新增</el-button>
    </div>

    <section class="panel filter-panel">
      <el-input
        v-model="query.keyword"
        clearable
        :placeholder="`搜索${resource.title}`"
        @keyup.enter="load"
      />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button @click="query.keyword = ''; load()">重置</el-button>
      <el-tag effect="plain" round class="count-tag">共 {{ rows.length }} 条</el-tag>
    </section>

    <section class="panel table-panel">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column
          v-for="column in resource.columns"
          :key="column.prop"
          :prop="column.prop"
          :label="column.label"
          :width="column.width"
          :min-width="column.minWidth"
        >
          <template #default="{ row }">
            <template v-if="column.type === 'price'">
              <b class="price">¥{{ formatPrice(row[column.prop]) }}</b>
            </template>
            <el-tag v-else-if="column.type === 'status'" :type="row[column.prop] ? 'success' : 'info'" effect="plain" round>
              {{ statusLabel(row) }}
            </el-tag>
            <span v-else-if="column.type === 'rating'" class="rating">{{ '★'.repeat(row[column.prop]) }}</span>
            <span v-else>{{ row[column.prop] ?? '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column v-if="hasActions" label="操作" :width="resourceKey === 'roles' ? 150 : 160" fixed="right">
          <template #default="{ row }">
            <el-button
              v-for="action in resource.actions"
              :key="action.key"
              v-perm="action.perm || resource.perm"
              link
              :type="action.type === 'danger' ? 'danger' : 'primary'"
              @click="handleAction(action, row)"
            >
              {{ action.label }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="dialogVisible" :title="editing ? `编辑${resource.title}` : `新增${resource.title}`" width="520px">
      <el-form label-position="top">
        <el-form-item v-for="field in resource.createFields" :key="field.prop" :label="field.label">
          <el-switch
            v-if="field.type === 'switch'"
            v-model="form[field.prop]"
            :active-value="1"
            :inactive-value="0"
            active-text="启用"
            inactive-text="停用"
          />
          <el-input-number
            v-else-if="field.type === 'number'"
            v-model="form[field.prop]"
            :min="0"
            controls-position="right"
            style="width: 100%"
          />
          <el-input v-else v-model="form[field.prop]" :placeholder="`请输入${field.label}`" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="actionVisible" :title="actionTitle()" width="500px">
      <el-form label-position="top">
        <el-form-item v-if="canAdjust" label="可用库存">
          <el-input-number v-model="actionForm.available" :min="0" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item v-else-if="resourceKey === 'reviews'" label="回复内容">
          <el-input v-model="actionForm.reply" type="textarea" :rows="4" placeholder="回复会员评价" />
        </el-form-item>
        <el-form-item v-else-if="resourceKey === 'roles'" label="权限点">
          <el-checkbox-group v-model="actionForm.permissions" class="permission-grid">
            <el-checkbox v-for="item in permissionOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item v-else-if="resourceKey === 'configs'" label="配置值">
          <el-input v-model="actionForm.value" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="actionVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAction">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.price {
  color: #e55039;
}

.rating {
  color: #f0a43c;
  letter-spacing: 1px;
}

.count-tag {
  margin-left: auto;
}

.permission-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
  width: 100%;
}

@media (max-width: 640px) {
  .permission-grid {
    grid-template-columns: 1fr;
  }
}
</style>
