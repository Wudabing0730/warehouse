<template>
  <div class="check-task-container">
    <!-- Search Bar -->
    <el-card class="search-card" shadow="never">
      <el-form :model="searchForm" inline>
        <el-form-item label="产品">
          <el-select
            v-model="searchForm.productId"
            placeholder="请选择产品"
            clearable
            filterable
            @change="handleSearch"
          >
            <el-option
              v-for="p in productOptions"
              :key="p.id"
              :label="p.productName + ' (' + p.productCode + ')'"
              :value="p.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable @change="handleSearch">
            <el-option label="待处理" :value="0" />
            <el-option label="已确认" :value="1" />
            <el-option label="已取消" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Toolbar -->
    <div class="toolbar">
      <el-button type="primary" @click="openCreateDialog">新建盘点</el-button>
    </div>

    <!-- Table -->
    <el-card class="table-card" shadow="never">
      <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="checkNo" label="盘点单号" min-width="160" />
        <el-table-column label="产品" min-width="140">
          <template #default="{ row }">
            {{ row.productName }} ({{ row.productCode }})
          </template>
        </el-table-column>
        <el-table-column prop="bookQuantity" label="账面数量" width="100" />
        <el-table-column prop="actualQuantity" label="实盘数量" width="100" />
        <el-table-column label="盈亏数量" width="100">
          <template #default="{ row }">
            <span :style="{ color: row.diffQuantity > 0 ? '#67c23a' : row.diffQuantity < 0 ? '#f56c6c' : '#909399' }">
              {{ row.diffQuantity ?? 0 }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="checkUser" label="盘点人" width="100" />
        <el-table-column prop="checkDate" label="盘点日期" width="120" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.status === 0" type="warning">待处理</el-tag>
            <el-tag v-else-if="row.status === 1" type="success">已确认</el-tag>
            <el-tag v-else type="info">已取消</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" type="primary" size="small" link @click="handleProcess(row)">
              处理
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="fetchList"
          @current-change="fetchList"
        />
      </div>
    </el-card>

    <!-- Create Dialog -->
    <el-dialog v-model="dialogVisible" title="新建盘点单" width="550px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="产品" prop="productId">
          <el-select
            v-model="form.productId"
            placeholder="请选择产品"
            filterable
            style="width: 100%"
            @change="onProductChange"
          >
            <el-option
              v-for="p in productOptions"
              :key="p.id"
              :label="p.productName + ' (' + p.productCode + ')'"
              :value="p.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="账面数量">
          <el-input-number v-model="bookQuantity" :disabled="true" style="width: 100%" />
        </el-form-item>
        <el-form-item label="实盘数量" prop="actualQuantity">
          <el-input-number v-model="form.actualQuantity" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="盘点日期" prop="checkDate">
          <el-date-picker v-model="form.checkDate" type="date" placeholder="选择日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { getCheckList, createCheck } from '@/api/inventory'
import { getProductList } from '@/api/product'
import { getStockList } from '@/api/stock'

// ---------- Search ----------
const searchForm = reactive({ productId: '', status: '' })

// ---------- Product options ----------
interface ProductOption {
  id: number | string
  name: string
  code: string
}
const productOptions = ref<ProductOption[]>([])

async function fetchProducts() {
  try {
    const res = await getProductList({ page: 1, size: 9999 })
    productOptions.value = (res.data?.records ?? res.data ?? []) as ProductOption[]
  } catch {
    // ignore
  }
}

// ---------- Table ----------
interface CheckRecord {
  id: number | string
  checkNo: string
  productId: number | string
  productName: string
  productCode: string
  bookQuantity: number
  actualQuantity: number
  diffQuantity: number
  checkUser: string
  checkDate: string
  status: number
  remark: string
}

const tableData = ref<CheckRecord[]>([])
const loading = ref(false)
const pagination = reactive({ page: 1, size: 10, total: 0 })

async function fetchList() {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      page: pagination.page,
      size: pagination.size,
    }
    if (searchForm.productId) params.productId = searchForm.productId
    if (searchForm.status !== '') params.status = searchForm.status

    const res = await getCheckList(params)
    const data = res.data
    tableData.value = (data.records ?? data ?? []) as CheckRecord[]
    pagination.total = (data.total ?? 0) as number
  } catch {
    // ignore
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  fetchList()
}

function handleReset() {
  searchForm.productId = ''
  searchForm.status = ''
  pagination.page = 1
  fetchList()
}

// ---------- Create Dialog ----------
const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const bookQuantity = ref(0)

const form = reactive({
  productId: '' as number | string,
  actualQuantity: 0,
  checkDate: new Date(),
  remark: '',
})

const rules: FormRules = {
  productId: [{ required: true, message: '请选择产品', trigger: 'change' }],
  actualQuantity: [{ required: true, message: '请输入实盘数量', trigger: 'blur' }],
  checkDate: [{ required: true, message: '请选择盘点日期', trigger: 'change' }],
}

async function onProductChange(productId: number | string) {
  if (!productId) {
    bookQuantity.value = 0
    return
  }
  try {
    const res = await getStockList({ productId, page: 1, size: 1 })
    const records = (res.data?.records ?? res.data ?? []) as any[]
    if (records.length > 0) {
      bookQuantity.value = records[0].quantity ?? 0
    } else {
      bookQuantity.value = 0
    }
  } catch {
    bookQuantity.value = 0
  }
}

function openCreateDialog() {
  form.productId = ''
  form.actualQuantity = 0
  form.checkDate = new Date()
  form.remark = ''
  bookQuantity.value = 0
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await createCheck({
      productId: form.productId,
      actualQuantity: form.actualQuantity,
      checkDate: form.checkDate,
      remark: form.remark,
    })
    ElMessage.success('盘点单创建成功')
    dialogVisible.value = false
    fetchList()
  } catch {
    // error handled by interceptor
  } finally {
    submitting.value = false
  }
}

function handleProcess(row: CheckRecord) {
  // Navigate to check result or handle inline - here just a placeholder
  ElMessage.info('请在盘点结果页面确认调整')
}

// ---------- Lifecycle ----------
onMounted(() => {
  fetchProducts()
  fetchList()
})
</script>

<style scoped>
.check-task-container {
  padding: 16px;
}

.search-card {
  margin-bottom: 12px;
}

.toolbar {
  margin-bottom: 12px;
}

.table-card {
  margin-bottom: 12px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
