<template>
  <div class="base-page">
    <!-- Search / Filter -->
    <el-card class="search-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="产品编码">
          <el-input v-model="searchForm.productCode" placeholder="输入产品编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="产品名称">
          <el-input v-model="searchForm.productName" placeholder="输入产品名称" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="产品类别">
          <el-tree-select
            v-model="searchForm.categoryId"
            :data="categoryTree"
            :props="{ label: 'categoryName', value: 'id', children: 'children' }"
            placeholder="选择类别"
            clearable
            check-strictly
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="选择状态" clearable style="width: 120px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
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
          <span>产品管理</span>
          <el-button type="primary" @click="handleAdd">新增产品</el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" border stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="productCode" label="产品编码" width="120" />
        <el-table-column prop="productName" label="产品名称" width="150" />
        <el-table-column prop="categoryName" label="类别" width="120" />
        <el-table-column prop="unit" label="单位" width="80" />
        <el-table-column prop="spec" label="规格" width="120" />
        <el-table-column prop="upperLimit" label="库存上限" width="100" />
        <el-table-column prop="lowerLimit" label="库存下限" width="100" />
        <el-table-column prop="defaultLocationName" label="默认库位" width="120" />
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
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
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
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="产品编码" prop="productCode">
              <el-input v-model="form.productCode" placeholder="输入产品编码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="产品名称" prop="productName">
              <el-input v-model="form.productName" placeholder="输入产品名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="产品类别" prop="categoryId">
              <el-tree-select
                v-model="form.categoryId"
                :data="categoryTree"
                :props="{ label: 'categoryName', value: 'id', children: 'children' }"
                placeholder="选择类别"
                check-strictly
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单位" prop="unit">
              <el-input v-model="form.unit" placeholder="输入单位" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="规格">
              <el-input v-model="form.spec" placeholder="输入规格" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="库存上限">
              <el-input-number v-model="form.upperLimit" :min="0" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="库存下限">
              <el-input-number v-model="form.lowerLimit" :min="0" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="默认库位">
              <el-select v-model="form.defaultLocationId" placeholder="选择库位" clearable style="width: 100%">
                <el-option
                  v-for="loc in locationList"
                  :key="loc.id"
                  :label="loc.locationName"
                  :value="loc.id"
                />
              </el-select>
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
  getProductList,
  createProduct,
  updateProduct,
  deleteProduct,
} from '@/api/product'
import { getCategoryTree, type CategoryTreeNode } from '@/api/category'
import { getLocationList, type LocationItem } from '@/api/location'

// ---- Search ----
const searchForm = reactive({
  productCode: '',
  productName: '',
  categoryId: null as number | null,
  status: null as number | null,
})

// ---- Table ----
const loading = ref(false)
const tableData = ref<any[]>([])
const pagination = reactive({ page: 1, size: 10, total: 0 })

const fetchList = async () => {
  loading.value = true
  try {
    const params: Record<string, any> = {
      page: pagination.page,
      size: pagination.size,
    }
    if (searchForm.productCode) params.productCode = searchForm.productCode
    if (searchForm.productName) params.productName = searchForm.productName
    if (searchForm.categoryId != null) params.categoryId = searchForm.categoryId
    if (searchForm.status != null) params.status = searchForm.status

    const res = await getProductList(params)
    tableData.value = res.data.records ?? res.data.list ?? []
    pagination.total = res.data.total ?? 0
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  fetchList()
}

const handleReset = () => {
  searchForm.productCode = ''
  searchForm.productName = ''
  searchForm.categoryId = null
  searchForm.status = null
  handleSearch()
}

// ---- Category tree (for search & dialog) ----
const categoryTree = ref<CategoryTreeNode[]>([])
const fetchCategoryTree = async () => {
  try {
    const res = await getCategoryTree()
    categoryTree.value = res.data ?? []
  } catch { /* ignore */ }
}

// ---- Location list (for dialog) ----
const locationList = ref<LocationItem[]>([])
const fetchLocationList = async () => {
  try {
    const res = await getLocationList({ page: 1, size: 999 })
    locationList.value = res.data.records ?? res.data.list ?? []
  } catch { /* ignore */ }
}

// ---- Dialog ----
const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  id: null as number | null,
  productCode: '',
  productName: '',
  categoryId: null as number | null,
  unit: '',
  spec: '',
  upperLimit: 0,
  lowerLimit: 0,
  defaultLocationId: null as number | null,
  status: 1,
})

const rules: FormRules = {
  productCode: [{ required: true, message: '请输入产品编码', trigger: 'blur' }],
  productName: [{ required: true, message: '请输入产品名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择产品类别', trigger: 'change' }],
  unit: [{ required: true, message: '请输入单位', trigger: 'blur' }],
}

const resetForm = () => {
  formRef.value?.resetFields()
  form.id = null
  form.productCode = ''
  form.productName = ''
  form.categoryId = null
  form.unit = ''
  form.spec = ''
  form.upperLimit = 0
  form.lowerLimit = 0
  form.defaultLocationId = null
  form.status = 1
}

const handleAdd = () => {
  resetForm()
  dialogTitle.value = '新增产品'
  dialogVisible.value = true
}

const handleEdit = async (row: any) => {
  resetForm()
  dialogTitle.value = '编辑产品'
  try {
    const res = await import('@/api/product').then(m => m.getProductById(row.id))
    const data = res.data
    form.id = data.id
    form.productCode = data.productCode ?? ''
    form.productName = data.productName ?? ''
    form.categoryId = data.categoryId ?? null
    form.unit = data.unit ?? ''
    form.spec = data.spec ?? ''
    form.upperLimit = data.upperLimit ?? 0
    form.lowerLimit = data.lowerLimit ?? 0
    form.defaultLocationId = data.defaultLocationId ?? null
    form.status = data.status ?? 1
  } catch {
    // ignore
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const payload = { ...form }
    if (payload.id) {
      await updateProduct(payload)
      ElMessage.success('更新成功')
    } else {
      await createProduct(payload)
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
  ElMessageBox.confirm(`确定要删除产品「${row.productName}」吗？`, '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    await deleteProduct(row.id)
    ElMessage.success('删除成功')
    fetchList()
  }).catch(() => { /* cancelled */ })
}

// ---- Init ----
onMounted(() => {
  fetchList()
  fetchCategoryTree()
  fetchLocationList()
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
