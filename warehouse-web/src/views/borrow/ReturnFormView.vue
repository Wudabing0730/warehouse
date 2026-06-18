<template>
  <div class="return-form">
    <el-card class="search-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="记录号">
          <el-input v-model="searchForm.recordNo" placeholder="输入记录号" clearable />
        </el-form-item>
        <el-form-item label="产品">
          <el-select v-model="searchForm.productId" placeholder="选择产品" filterable clearable>
            <el-option v-for="p in productList" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="借用人">
          <el-input v-model="searchForm.borrower" placeholder="输入借用人" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="选择状态" clearable>
            <el-option label="借出中" :value="0" />
            <el-option label="部分归还" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column prop="recordNo" label="记录号" width="180" />
        <el-table-column label="产品" min-width="150">
          <template #default="{ row }">{{ row.productName ?? '--' }}</template>
        </el-table-column>
        <el-table-column prop="borrower" label="借用人" width="120" />
        <el-table-column prop="borrowQuantity" label="借出数量" width="110" />
        <el-table-column label="已还数量" width="110">
          <template #default="{ row }">{{ row.returnedQuantity ?? 0 }}</template>
        </el-table-column>
        <el-table-column prop="borrowDate" label="借出日期" width="180" />
        <el-table-column label="预计归还日期" width="180">
          <template #default="{ row }">{{ row.expectedReturnDate ?? '--' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status !== 1"
              type="primary"
              size="small"
              @click="openReturn(row)"
            >
              归还
            </el-button>
            <span v-else class="text-muted">--</span>
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
          @current-change="fetchData"
          @size-change="fetchData"
        />
      </div>
    </el-card>

    <!-- Return Dialog -->
    <el-dialog v-model="dialogVisible" title="归还登记" width="500px" destroy-on-close>
      <div v-if="currentRow">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="记录号">{{ currentRow.recordNo }}</el-descriptions-item>
          <el-descriptions-item label="产品">{{ currentRow.productName ?? '--' }}</el-descriptions-item>
          <el-descriptions-item label="借用人">{{ currentRow.borrower }}</el-descriptions-item>
          <el-descriptions-item label="借出数量">{{ currentRow.borrowQuantity }}</el-descriptions-item>
          <el-descriptions-item label="已还数量">{{ currentRow.returnedQuantity ?? 0 }}</el-descriptions-item>
          <el-descriptions-item label="剩余数量">
            {{ (currentRow.borrowQuantity ?? 0) - (currentRow.returnedQuantity ?? 0) }}
          </el-descriptions-item>
        </el-descriptions>

        <el-form
          ref="returnFormRef"
          :model="returnForm"
          :rules="returnRules"
          label-width="120px"
          style="margin-top: 16px"
        >
          <el-form-item label="归还数量" prop="returnQuantity">
            <el-input-number
              v-model="returnForm.returnQuantity"
              :min="0.01"
              :max="remaining"
              :precision="2"
              :controls="true"
              style="width: 100%"
            />
            <span class="stock-hint">最大可还: {{ remaining }}</span>
          </el-form-item>
          <el-form-item label="实际归还日期" prop="actualReturnDate">
            <el-date-picker
              v-model="returnForm.actualReturnDate"
              type="datetime"
              placeholder="选择实际归还日期"
              format="YYYY-MM-DD HH:mm:ss"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 100%"
            />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="returning" @click="handleReturn">
          确认归还
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import { getBorrowList, returnBorrow } from '@/api/borrow'
import { getProductList } from '@/api/product'

const loading = ref(false)
const dialogVisible = ref(false)
const returning = ref(false)
const currentRow = ref<any>(null)
const returnFormRef = ref<FormInstance>()

const productList = ref<any[]>([])

const searchForm = reactive({
  recordNo: '',
  productId: null as number | null,
  borrower: '',
  status: null as number | null,
})

const pagination = reactive({ page: 1, size: 10, total: 0 })
const tableData = ref<any[]>([])

const returnForm = reactive({
  returnQuantity: 0,
  actualReturnDate: '',
})

const returnRules: FormRules = {
  returnQuantity: [
    { required: true, message: '请输入归还数量', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value === 0 || value === null || value === undefined) {
          callback(new Error('归还数量必须大于0'))
        } else if (value > remaining.value) {
          callback(new Error(`归还数量不能超过剩余数量 ${remaining.value}`))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
  actualReturnDate: [{ required: true, message: '请选择实际归还日期', trigger: 'change' }],
}

const remaining = computed(() => {
  if (!currentRow.value) return 0
  return (currentRow.value.borrowQuantity ?? 0) - (currentRow.value.returnedQuantity ?? 0)
})

function statusTagType(status: number): 'warning' | 'success' | 'danger' | 'info' {
  if (status === 0) return 'warning'
  if (status === 1) return 'success'
  if (status === 2) return 'info'
  return 'info'
}

function statusLabel(status: number): string {
  if (status === 0) return '借出中'
  if (status === 1) return '已归还'
  if (status === 2) return '部分归还'
  return '未知'
}

onMounted(() => {
  loadProducts()
  fetchData()
})

async function loadProducts() {
  try {
    const res = await getProductList({ size: 999 })
    productList.value = res.data?.records ?? res.data ?? []
  } catch {
    // handled by interceptor
  }
}

async function fetchData() {
  loading.value = true
  try {
    const params: any = {
      page: pagination.page,
      size: pagination.size,
    }
    if (searchForm.recordNo) params.recordNo = searchForm.recordNo
    if (searchForm.productId) params.productId = searchForm.productId
    if (searchForm.borrower) params.borrower = searchForm.borrower
    if (searchForm.status !== null && searchForm.status !== '') params.status = searchForm.status
    const res = await getBorrowList(params)
    tableData.value = res.data?.records ?? res.data ?? []
    pagination.total = res.data?.total ?? 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  fetchData()
}

function handleReset() {
  searchForm.recordNo = ''
  searchForm.productId = null
  searchForm.borrower = ''
  searchForm.status = null
  pagination.page = 1
  fetchData()
}

function openReturn(row: any) {
  currentRow.value = row
  returnForm.returnQuantity = remaining.value
  returnForm.actualReturnDate = getDefaultTime()
  dialogVisible.value = true
  // Reset form validation after dialog opens
  setTimeout(() => returnFormRef.value?.clearValidate(), 0)
}

async function handleReturn() {
  if (!returnFormRef.value || !currentRow.value) return
  const valid = await returnFormRef.value.validate().catch(() => false)
  if (!valid) return

  returning.value = true
  try {
    await returnBorrow(currentRow.value.id, {
      returnQuantity: returnForm.returnQuantity,
      actualReturnDate: returnForm.actualReturnDate,
    })
    ElMessage.success('归还登记成功')
    dialogVisible.value = false
    fetchData()
  } catch {
    // handled by interceptor
  } finally {
    returning.value = false
  }
}

const getDefaultTime = (): string => {
  const now = new Date()
  const pad = (n: number) => n.toString().padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
}
</script>

<style scoped>
.return-form {
  padding: 16px;
}

.search-card {
  margin-bottom: 16px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.text-muted {
  color: #909399;
  font-size: 13px;
}

.stock-hint {
  margin-left: 8px;
  color: #909399;
  font-size: 13px;
}
</style>
