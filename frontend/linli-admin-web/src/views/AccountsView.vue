<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createResource, getResource } from '@/api/admin'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'

const user = useUserStore()
const rows = ref([])
const loading = ref(false)
const keyword = ref('')
const dialogVisible = ref(false)
const form = reactive({ nickname: '', phone: '', role: 'operator' })

async function load() {
  loading.value = true
  try {
    const data = await getResource('/admin/accounts', { keyword: keyword.value })
    rows.value = data.list || []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, { nickname: '', phone: '', role: 'operator' })
  dialogVisible.value = true
}

async function save() {
  if (!/^1\d{10}$/.test(form.phone)) {
    ElMessage.warning('请输入 11 位手机号')
    return
  }
  if (!form.nickname) {
    ElMessage.warning('请输入账号名称')
    return
  }
  await createResource('/admin/accounts', { ...form, password: '123456' })
  ElMessage.success('后台账号已创建，初始密码为 123456')
  dialogVisible.value = false
  await load()
}

async function toggle(row) {
  const next = row.status ? 0 : 1
  await ElMessageBox.confirm(`确认${next ? '启用' : '禁用'}账号「${row.nickname}」吗？`, '账号状态', { type: 'warning' })
  await request.put(`/admin/accounts/${row.id}/status`, { status: next })
  ElMessage.success('账号状态已更新')
  await load()
}

onMounted(load)
</script>

<template>
  <div class="page-shell">
    <div class="page-title-row">
      <div>
        <h1>账号管理</h1>
        <p>维护后台运营账号，禁用后该账号将无法继续登录。</p>
      </div>
      <el-button v-perm="'system:account:manage'" type="primary" @click="openCreate">＋ 新建后台账号</el-button>
    </div>

    <section class="panel filter-panel">
      <el-input v-model="keyword" clearable placeholder="搜索账号名称或手机号" @keyup.enter="load" />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button @click="keyword = ''; load()">重置</el-button>
      <el-tag effect="plain" round class="count-tag">共 {{ rows.length }} 个账号</el-tag>
    </section>

    <section class="panel table-panel">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column label="账号" min-width="200">
          <template #default="{ row }">
            <div class="account-cell">
              <span class="account-avatar">{{ row.nickname.slice(0, 1) }}</span>
              <div><b>{{ row.nickname }}</b><span>{{ row.phone }}</span></div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="roleName" label="角色" width="130" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status ? 'success' : 'info'" effect="plain" round>
              {{ row.status ? '正常' : '已禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginAt" label="最近登录" min-width="170" />
        <el-table-column prop="createdAt" label="创建日期" width="130" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <span v-if="row.phone === user.profile?.phone" class="muted">当前账号</span>
            <el-button
              v-else
              v-perm="'system:account:manage'"
              link
              :type="row.status ? 'danger' : 'success'"
              @click="toggle(row)"
            >
              {{ row.status ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="dialogVisible" title="新建后台账号" width="480px">
      <el-form label-position="top">
        <el-form-item label="账号名称" required>
          <el-input v-model="form.nickname" placeholder="如：仓库运营" />
        </el-form-item>
        <el-form-item label="手机号" required>
          <el-input v-model="form.phone" maxlength="11" placeholder="用于登录后台的手机号" />
        </el-form-item>
        <el-form-item label="角色" required>
          <el-select v-model="form.role" style="width: 100%">
            <el-option label="商家运营（业务权限）" value="operator" />
            <el-option label="平台管理员（全部权限）" value="admin" />
          </el-select>
        </el-form-item>
        <el-alert
          title="演示环境初始密码统一为 123456，创建后请由管理员在正式环境完成密码更换。"
          type="info"
          :closable="false"
          show-icon
        />
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">创建账号</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.count-tag {
  margin-left: auto;
}

.account-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.account-avatar {
  display: grid;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: #eaf7ee;
  color: #1f8a4c;
  place-items: center;
  font-size: 12px;
  font-weight: 700;
}

.account-cell div {
  display: grid;
  gap: 2px;
}

.account-cell span:last-child {
  color: #8a958d;
  font-size: 10px;
}
</style>
