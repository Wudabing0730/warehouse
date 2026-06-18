<template>
  <div class="base-page">
    <!-- Search / Filter -->
    <el-card class="search-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="供应商编码">
          <el-input v-model="searchForm.supplierCode" placeholder="输入供应商编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="供应商名称">
          <el-input v-model="searchForm.supplierName" placeholder="输入供应商名称" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Table -->
    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>供应商管理</span>
          <el-button type="primary" @click="handleAdd">新增供应商</el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" border stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="supplierCode" label="供应商编码" width="130" />
        <el-table-column prop="supplierName" label="供应商名称" width="160" />
        <el-table-column prop="contactPerson" label="联系人" width="110" />
        <el-table-column prop="contactPhone" label="联系电话" width="130" />
        <el-table-column prop="contactEmail" label="邮箱" width="180" show-overflow-tooltip />
        <el-table-column prop="address" label="地址" width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        class="pagination"
        @size-change="handleSearch"
        @current-change="handleSearch"
      />
    </el-card>

    <!-- Add / Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      :close-on-click-modal="false"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="供应商编码" prop="supplierCode">
              <el-input v-model="form.supplierCode" placeholder="输入供应商编码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="供应商名称" prop="supplierName">
              <el-input v-model="form.supplierName" placeholder="输入供应商名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="联系人">
              <el-input v-model="form.contactPerson" placeholder="输入联系人" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话">
              <el-input v-model="form.contactPhone" placeholder="输入联系电话" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="邮箱">
              <el-input v-model="form.contactEmail" placeholder="输入邮箱" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width: 100%">
                <el-option label="启用" :value="1" />
                <el-option label="禁用" :value="0" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="地址">
          <el-input v-model="form.address" placeholder="输入地址" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  getSupplierList,
  createSupplier,
  updateSupplier,
  deleteSupplier,
} from '@/api/supplier'

// ---- Search ----
const searchForm = reactive({
  supplierCode: '',
  supplierName: '',
})

// ---- Table ----
const loading = ref(false)
const tableData = ref<any[]>([])
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const fetchList = async () => {
  loading.value = true
  try {
    const params: Record<string, any> = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
    }
    if (searchForm.supplierCode) params.supplierCode = searchForm.supplierCode
    if (searchForm.supplierName) params.supplierName = searchForm.supplierName

    const res = await getSupplierList(params)
    tableData.value = res.data.records ?? res.data.list ?? []
    pagination.total = res.data.total ?? 0
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.pageNum = 1
  fetchList()
}

const handleReset = () => {
  searchForm.supplierCode = ''
  searchForm.supplierName = ''
  handleSearch()
}

// ---- Dialog ----
const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  id: null as number | null,
  supplierCode: '',
  supplierName: '',
  contactPerson: '',
  contactPhone: '',
  contactEmail: '',
  address: '',
  status: 1,
})

const rules: FormRules = {
  supplierCode: [{ required: true, message: '请输入供应商编码', trigger: 'blur' }],
  supplierName: [{ required: true, message: '请输入供应商名称', trigger: 'blur' }],
}

const resetForm = () => {
  formRef.value?.resetFields()
  form.id = null
  form.supplierCode = ''
  form.supplierName = ''
  form.contactPerson = ''
  form.contactPhone = ''
  form.contactEmail = ''
  form.address = ''
  form.status = 1
}

const handleAdd = () => {
  resetForm()
  dialogTitle.value = '新增供应商'
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  resetForm()
  dialogTitle.value = '编辑供应商'
  form.id = row.id
  form.supplierCode = row.supplierCode ?? ''
  form.supplierName = row.supplierName ?? ''
  form.contactPerson = row.contactPerson ?? ''
  form.contactPhone = row.contactPhone ?? ''
  form.contactEmail = row.contactEmail ?? ''
  form.address = row.address ?? ''
  form.status = row.status ?? 1
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const payload = { ...form }
    if (payload.id) {
      await updateSupplier(payload)
      ElMessage.success('更新成功')
    } else {
      await createSupplier(payload)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchList()
  } catch {
    // handled by interceptor
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确定要删除供应商「${row.supplierName}」吗？`, '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    await deleteSupplier(row.id)
    ElMessage.success('删除成功')
    fetchList()
  }).catch(() => { /* cancelled */ })
}

// ---- Init ----
onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.base-page {
  padding: 16px;
}

.search-card {
  margin-bottom: 16px;
}

.table-card {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
