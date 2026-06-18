<template>
  <div class="role-view">
    <!-- Search Bar -->
    <el-card class="search-card" shadow="never">
      <el-form :model="searchForm" inline>
        <el-form-item label="角色名称">
          <el-input
            v-model="searchForm.roleName"
            placeholder="请输入角色名称"
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
          <span>角色管理</span>
          <el-button type="primary" @click="handleAdd">新增角色</el-button>
        </div>
      </template>
      <el-table :data="tableData" border stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="roleName" label="角色名称" width="150" />
        <el-table-column prop="roleDesc" label="角色描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="success" link size="small" @click="handleAssignPermission(row)">权限分配</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
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
      width="550px"
      :close-on-click-modal="false"
      @close="handleDialogClose"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色描述" prop="roleDesc">
          <el-input v-model="form.roleDesc" type="textarea" :rows="3" placeholder="请输入角色描述" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.statusBool" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- Permission Assignment Dialog -->
    <el-dialog
      v-model="permissionVisible"
      title="权限分配"
      width="500px"
      :close-on-click-modal="false"
      @close="handlePermissionClose"
    >
      <el-tree
        ref="treeRef"
        :data="permissionTree"
        show-checkbox
        node-key="id"
        default-expand-all
        :props="{ label: 'name', children: 'children' }"
      />
      <template #footer>
        <el-button @click="permissionVisible = false">取消</el-button>
        <el-button type="primary" :loading="permissionLoading" @click="handlePermissionSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import type { ElTree } from 'element-plus'
import { getRoleList, createRole, updateRole, deleteRole, getRolePermissions, assignPermissions } from '@/api/role'
import { getPermissionTree } from '@/api/permission'

interface Role {
  id?: number
  roleName: string
  roleDesc: string
  status: number
  createTime?: string
}

interface RoleForm {
  roleName: string
  roleDesc: string
  statusBool: boolean
}

interface PermissionNode {
  id: number
  name: string
  children?: PermissionNode[]
}

// ---------- Search ----------
const searchForm = reactive({
  roleName: '',
  status: null as number | null,
})

// ---------- Table ----------
const tableData = ref<Role[]>([])
const loading = ref(false)
const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0,
})

async function fetchData() {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      page: pagination.page,
      pageSize: pagination.pageSize,
    }
    if (searchForm.roleName) params.roleName = searchForm.roleName
    if (searchForm.status !== null) params.status = searchForm.status

    const res = await getRoleList(params)
    const data = res.data || res
    tableData.value = data.records ?? data.list ?? []
    pagination.total = data.total ?? 0
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  fetchData()
}

function handleReset() {
  searchForm.roleName = ''
  searchForm.status = null
  handleSearch()
}

// ---------- Add / Edit ----------
const dialogVisible = ref(false)
const dialogTitle = ref('新增角色')
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<number | null>(null)

const form = reactive<RoleForm>({
  roleName: '',
  roleDesc: '',
  statusBool: true,
})

const formRules: FormRules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
}

function handleAdd() {
  dialogTitle.value = '新增角色'
  isEdit.value = false
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row: Role) {
  dialogTitle.value = '编辑角色'
  isEdit.value = true
  editingId.value = row.id ?? null
  form.roleName = row.roleName
  form.roleDesc = row.roleDesc ?? ''
  form.statusBool = row.status === 1
  dialogVisible.value = true
}

function resetForm() {
  form.roleName = ''
  form.roleDesc = ''
  form.statusBool = true
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
      roleName: form.roleName,
      roleDesc: form.roleDesc,
      status: form.statusBool ? 1 : 0,
      ...(isEdit.value && { id: editingId.value }),
    }
    if (isEdit.value) {
      await updateRole(payload)
      ElMessage.success('编辑成功')
    } else {
      await createRole(payload)
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
function handleDelete(row: Role) {
  ElMessageBox.confirm(`确定要删除角色 "${row.roleName}" 吗？`, '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    try {
      await deleteRole(row.id!)
      ElMessage.success('删除成功')
      fetchData()
    } catch {
      // handled by interceptor
    }
  }).catch(() => {})
}

// ---------- Permission Assignment ----------
const permissionVisible = ref(false)
const permissionLoading = ref(false)
const permissionTree = ref<PermissionNode[]>([])
const treeRef = ref<InstanceType<typeof ElTree>>()
const assigningRoleId = ref<number | null>(null)

async function handleAssignPermission(row: Role) {
  assigningRoleId.value = row.id ?? null
  try {
    const [treeRes, checkedRes] = await Promise.all([
      getPermissionTree(),
      getRolePermissions(row.id!),
    ])
    permissionTree.value = treeRes.data ?? treeRes ?? []
    const checkedIds: number[] = checkedRes.data ?? checkedRes ?? []
    // Wait for tree to render, then set checked keys
    await new Promise((r) => setTimeout(r, 0))
    treeRef.value?.setCheckedKeys(checkedIds)
  } catch {
    // handled by interceptor
  }
  permissionVisible.value = true
}

function handlePermissionClose() {
  assigningRoleId.value = null
}

async function handlePermissionSubmit() {
  const checkedKeys = treeRef.value?.getCheckedKeys() as number[]
  const halfCheckedKeys = treeRef.value?.getHalfCheckedKeys() as number[]
  const allKeys = [...checkedKeys, ...halfCheckedKeys]

  permissionLoading.value = true
  try {
    await assignPermissions({ roleId: assigningRoleId.value, permissionIds: allKeys })
    ElMessage.success('权限分配成功')
    permissionVisible.value = false
  } catch {
    // handled by interceptor
  } finally {
    permissionLoading.value = false
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.role-view {
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
