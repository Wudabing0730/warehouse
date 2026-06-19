<template>
  <div class="user-view">
    <!-- Search Bar -->
    <el-card class="search-card" shadow="never">
      <el-form :model="searchForm" inline>
        <el-form-item label="用户名">
          <el-input
            v-model="searchForm.username"
            placeholder="请输入用户名"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="真实姓名">
          <el-input
            v-model="searchForm.realName"
            placeholder="请输入真实姓名"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable style="width: 120px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Data Table -->
    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <el-button type="primary" @click="handleAdd">新增用户</el-button>
        </div>
      </template>
      <el-table :data="tableData" border stripe v-loading="loading" style="width: 100%"
        @sort-change="handleSortChange">
        <el-table-column prop="id" label="ID" width="80" align="center" sortable="custom" />
        <el-table-column prop="username" label="用户名" width="120" sortable="custom" />
        <el-table-column prop="realName" label="真实姓名" width="120" sortable="custom" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" sortable="custom" />
        <el-table-column label="操作" width="260" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
            <el-button type="warning" link size="small" @click="handleResetPassword(row)">重置密码</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSearch"
          @current-change="handleSearch"
        />
      </div>
    </el-card>

    <!-- Add/Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      :close-on-click-modal="false"
      @close="handleDialogClose"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="isEdit" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!isEdit">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.statusBool" active-text="启用" inactive-text="禁用" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select
            v-model="form.roleIds"
            multiple
            placeholder="请选择角色"
            style="width: 100%"
          >
            <el-option
              v-for="role in roleList"
              :key="role.id"
              :label="role.roleName"
              :value="role.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- Reset Password Dialog -->
    <el-dialog
      v-model="resetPwdVisible"
      title="重置密码"
      width="450px"
      :close-on-click-modal="false"
      @close="handleResetPwdClose"
    >
      <el-form ref="resetPwdFormRef" :model="resetPwdForm" :rules="resetPwdRules" label-width="100px">
        <el-form-item label="新密码" prop="password">
          <el-input v-model="resetPwdForm.password" type="password" placeholder="请输入新密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetPwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="resetPwdLoading" @click="handleResetPwdSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { getUserList, createUser, updateUser, deleteUser, resetPasswordAdmin } from '@/api/user'
import { getRoleList } from '@/api/role'

interface User {
  id?: number
  username: string
  realName: string
  phone: string
  email: string
  status: number
  createTime?: string
  roleIds?: number[]
}

interface Role {
  id: number
  roleName: string
}

interface UserForm {
  username: string
  password: string
  realName: string
  phone: string
  email: string
  statusBool: boolean
  roleIds: number[]
}

// ---------- Search ----------
const searchForm = reactive({
  username: '',
  realName: '',
  status: null as number | null,
})

// ---------- Table ----------
const tableData = ref<User[]>([])
const loading = ref(false)
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0,
})

// 排序状态:P0 修复 — 支持点击列头切换升降序
// sortProp 映射到后端 orderBy 字段(白名单)
// null = 不传 orderBy,后端用默认 userId ASC
const sortState = reactive({
  orderBy: '' as string,
  order: '' as 'asc' | 'desc' | '',
})

async function fetchData() {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      page: pagination.page,
      size: pagination.size,
    }
    if (searchForm.username) params.username = searchForm.username
    if (searchForm.realName) params.realName = searchForm.realName
    if (searchForm.status !== null) params.status = searchForm.status
    if (sortState.orderBy) params.orderBy = sortState.orderBy
    if (sortState.order) params.order = sortState.order

    const res = await getUserList(params)
    const data = res.data || res
    tableData.value = data.records ?? data.list ?? []
    pagination.total = data.total ?? 0
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

// 列头点击 → 更新 sortState → 重新查询
function handleSortChange({ prop, order }: { prop: string | null; order: 'ascending' | 'descending' | null }) {
  // Element Plus 列 prop('id') → 后端字段名('userId')的映射
  const propToField: Record<string, string> = {
    id: 'userId',
    username: 'username',
    realName: 'realName',
    createTime: 'createTime',
  }
  if (!prop || !order) {
    // 第三次点击:清除排序,后端用默认 userId ASC
    sortState.orderBy = ''
    sortState.order = ''
  } else {
    sortState.orderBy = propToField[prop] || prop
    sortState.order = order === 'ascending' ? 'asc' : 'desc'
  }
  pagination.page = 1
  fetchData()
}

function handleSearch() {
  pagination.page = 1
  fetchData()
}

function handleReset() {
  searchForm.username = ''
  searchForm.realName = ''
  searchForm.status = null
  sortState.orderBy = ''
  sortState.order = ''
  handleSearch()
}

// ---------- Add / Edit ----------
const dialogVisible = ref(false)
const dialogTitle = ref('新增用户')
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<number | null>(null)

const form = reactive<UserForm>({
  username: '',
  password: '',
  realName: '',
  phone: '',
  email: '',
  statusBool: true,
  roleIds: [],
})

const formRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 30, message: '长度在 3 到 30 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 30, message: '长度在 6 到 30 个字符', trigger: 'blur' },
  ],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  email: [{ type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }],
}

const roleList = ref<Role[]>([])

async function fetchRoles() {
  try {
    const res = await getRoleList({ size: 999 })
    const data = res.data || res
    roleList.value = data.records ?? data.list ?? data ?? []
  } catch {
    // handled by interceptor
  }
}

function handleAdd() {
  dialogTitle.value = '新增用户'
  isEdit.value = false
  editingId.value = null
  resetForm()
  fetchRoles()
  dialogVisible.value = true
}

function handleEdit(row: User) {
  dialogTitle.value = '编辑用户'
  isEdit.value = true
  editingId.value = row.id ?? null
  form.username = row.username
  form.password = ''
  form.realName = row.realName
  form.phone = row.phone ?? ''
  form.email = row.email ?? ''
  form.statusBool = row.status === 1
  form.roleIds = row.roleIds ?? []
  fetchRoles()
  dialogVisible.value = true
}

function resetForm() {
  form.username = ''
  form.password = ''
  form.realName = ''
  form.phone = ''
  form.email = ''
  form.statusBool = true
  form.roleIds = []
  formRef.value?.resetFields()
}

function handleDialogClose() {
  resetForm()
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const payload = {
      username: form.username,
      realName: form.realName,
      phone: form.phone,
      email: form.email,
      status: form.statusBool ? 1 : 0,
      roleIds: form.roleIds,
      ...(isEdit.value ? { id: editingId.value } : { password: form.password }),
    }
    if (isEdit.value) {
      await updateUser(payload)
      ElMessage.success('编辑成功')
    } else {
      await createUser(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch {
    // handled by interceptor
  } finally {
    submitLoading.value = false
  }
}

// ---------- Delete ----------
function handleDelete(row: User) {
  ElMessageBox.confirm(`确定要删除用户 "${row.username}" 吗？`, '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    try {
      await deleteUser(row.id!)
      ElMessage.success('删除成功')
      fetchData()
    } catch {
      // handled by interceptor
    }
  }).catch(() => {})
}

// ---------- Reset Password ----------
const resetPwdVisible = ref(false)
const resetPwdLoading = ref(false)
const resetPwdFormRef = ref<FormInstance>()
const resetPwdEditingId = ref<number | null>(null)

const resetPwdForm = reactive({
  password: '',
})

const resetPwdRules: FormRules = {
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 30, message: '长度在 6 到 30 个字符', trigger: 'blur' },
  ],
}

function handleResetPassword(row: User) {
  resetPwdEditingId.value = row.id ?? null
  resetPwdForm.password = ''
  resetPwdVisible.value = true
}

function handleResetPwdClose() {
  resetPwdForm.password = ''
  resetPwdFormRef.value?.resetFields()
}

async function handleResetPwdSubmit() {
  const valid = await resetPwdFormRef.value?.validate().catch(() => false)
  if (!valid) return

  resetPwdLoading.value = true
  try {
    await resetPasswordAdmin(resetPwdEditingId.value!, resetPwdForm.password)
    ElMessage.success('密码重置成功')
    resetPwdVisible.value = false
  } catch {
    // handled by interceptor
  } finally {
    resetPwdLoading.value = false
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.user-view {
  padding: 16px;
}
.search-card {
  margin-bottom: 16px;
}
.table-card {
  min-height: 400px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
