<template>
  <div class="borrow-query">
    <el-card class="search-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="记录号">
          <el-input v-model="searchForm.recordNo" placeholder="输入记录号" clearable />
        </el-form-item>
        <el-form-item label="产品">
          <el-select v-model="searchForm.productId" placeholder="选择产品" filterable clearable>
            <el-option v-for="p in productList" :key="p.id" :label="p.productName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="借用人">
          <el-input v-model="searchForm.borrower" placeholder="输入借用人" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="选择状态" clearable>
            <el-option label="借出中" :value="0" />
            <el-option label="已归还" :value="1" />
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
      <el-table
        :data="tableData"
        border
        stripe
        v-loading="loading"
        :row-class-name="tableRowClassName"
      >
        <el-table-column prop="recordNo" label="记录号" width="180" />
        <el-table-column label="产品" min-width="150">
          <template #default="{ row }">{{ row.productName ?? '--' }}</template>
        </el-table-column>
        <el-table-column prop="borrower" label="借用人" width="120" />
        <el-table-column prop="borrowQuantity" label="借出数量" width="110" />
        <el-table-column label="已还数量" width="110">
          <template #default="{ row }">{{ row.returnedQuantity ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="剩余数量" width="110">
          <template #default="{ row }">
            {{ (row.borrowQuantity ?? 0) - (row.returnedQuantity ?? 0) }}
          </template>
        </el-table-column>
        <el-table-column label="借出日期" width="180">
          <template #default="{ row }">{{ row.borrowDate ?? '--' }}</template>
        </el-table-column>
        <el-table-column label="预计归还日期" width="180">
          <template #default="{ row }">{{ row.expectedReturnDate ?? '--' }}</template>
        </el-table-column>
        <el-table-column label="实际归还日期" width="180">
          <template #default="{ row }">{{ row.actualReturnDate ?? '--' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '--' }}</template>
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import { getBorrowList } from '@/api/borrow'
import { getProductList } from '@/api/product'

const loading = ref(false)

const productList = ref<any[]>([])

const searchForm = reactive({
  recordNo: '',
  productId: null as number | null,
  borrower: '',
  status: null as number | null,
})

const pagination = reactive({ page: 1, size: 10, total: 0 })
const tableData = ref<any[]>([])

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

function isOverdue(row: any): boolean {
  if (row.status === 1) return false
  if (!row.expectedReturnDate) return false
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const expected = new Date(row.expectedReturnDate)
  expected.setHours(0, 0, 0, 0)
  return expected < today
}

function tableRowClassName({ row }: { row: any }): string {
  return isOverdue(row) ? 'row-overdue' : ''
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
</script>

<style scoped>
.borrow-query {
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
</style>

<style>
/* Global style for overdue row highlight */
.el-table .row-overdue {
  --el-table-tr-bg-color: var(--el-color-danger-light-9);
  background-color: var(--el-color-danger-light-9);
}
</style>
