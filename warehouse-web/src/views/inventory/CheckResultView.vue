<template>
  <div class="check-result-container">
    <!-- Search Bar -->
    <el-card class="search-card" shadow="never">
      <el-form :model="searchForm" inline>
        <el-form-item label="盘点单号">
          <el-input v-model="searchForm.checkNo" placeholder="请输入盘点单号" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="产品">
          <el-select v-model="searchForm.productId" placeholder="请选择产品" clearable filterable>
            <el-option
              v-for="p in productOptions"
              :key="p.id"
              :label="p.productName + ' (' + p.productCode + ')'"
              :value="p.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
            <el-option label="待处理" :value="0" />
            <el-option label="已确认" :value="1" />
            <el-option label="已取消" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Table -->
    <el-card class="table-card" shadow="never">
      <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%" @row-click="showDetail">
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
            <span
              :style="{
                color: row.diffQuantity > 0 ? '#67c23a' : row.diffQuantity < 0 ? '#f56c6c' : '#909399',
                fontWeight: 'bold',
              }"
            >
              {{ (row.diffQuantity ?? 0) > 0 ? '+' : '' }}{{ row.diffQuantity ?? 0 }}
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
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 0">
              <el-button type="success" size="small" link @click.stop="handleConfirm(row)">确认调整</el-button>
              <el-button type="danger" size="small" link @click.stop="handleCancel(row)">取消</el-button>
            </template>
            <span v-else>-</span>
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

    <!-- Detail Dialog -->
    <el-dialog v-model="detailVisible" title="盘点详情" width="500px">
      <el-descriptions v-if="detailRow" :column="2" border>
        <el-descriptions-item label="盘点单号">{{ detailRow.checkNo }}</el-descriptions-item>
        <el-descriptions-item label="产品">{{ detailRow.productName }} ({{ detailRow.productCode }})</el-descriptions-item>
        <el-descriptions-item label="账面数量">{{ detailRow.bookQuantity }}</el-descriptions-item>
        <el-descriptions-item label="实盘数量">{{ detailRow.actualQuantity }}</el-descriptions-item>
        <el-descriptions-item label="盈亏数量">
          <span
            :style="{
              color: detailRow.diffQuantity > 0 ? '#67c23a' : detailRow.diffQuantity < 0 ? '#f56c6c' : '#909399',
              fontWeight: 'bold',
            }"
          >
            {{ (detailRow.diffQuantity ?? 0) > 0 ? '+' : '' }}{{ detailRow.diffQuantity ?? 0 }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="盘点人">{{ detailRow.checkUser }}</el-descriptions-item>
        <el-descriptions-item label="盘点日期">{{ detailRow.checkDate }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag v-if="detailRow.status === 0" type="warning">待处理</el-tag>
          <el-tag v-else-if="detailRow.status === 1" type="success">已确认</el-tag>
          <el-tag v-else type="info">已取消</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="备注">{{ detailRow.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCheckList, confirmCheck } from '@/api/inventory'
import { getProductList } from '@/api/product'

// ---------- Search ----------
const searchForm = reactive({
  checkNo: '',
  productId: '' as number | string,
  status: '' as number | string,
  dateRange: [] as string[],
})

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

function buildParams(): Record<string, unknown> {
  const params: Record<string, unknown> = {
    page: pagination.page,
    size: pagination.size,
  }
  if (searchForm.checkNo) params.checkNo = searchForm.checkNo
  if (searchForm.productId) params.productId = searchForm.productId
  if (searchForm.status !== '') params.status = searchForm.status
  if (searchForm.dateRange && searchForm.dateRange.length === 2) {
    params.startDate = searchForm.dateRange[0]
    params.endDate = searchForm.dateRange[1]
  }
  return params
}

async function fetchList() {
  loading.value = true
  try {
    const res = await getCheckList(buildParams())
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
  searchForm.checkNo = ''
  searchForm.productId = ''
  searchForm.status = ''
  searchForm.dateRange = []
  pagination.page = 1
  fetchList()
}

// ---------- Confirm / Cancel ----------
async function handleConfirm(row: CheckRecord) {
  try {
    await ElMessageBox.confirm(
      `确认将盘点单 ${row.checkNo} 的差异调整应用到库存？`,
      '确认调整',
      { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' }
    )
    await confirmCheck(row.id, { approved: true })
    ElMessage.success('调整已确认')
    fetchList()
  } catch {
    // user cancelled or error
  }
}

async function handleCancel(row: CheckRecord) {
  try {
    await ElMessageBox.confirm(
      `确定要取消盘点单 ${row.checkNo} 吗？`,
      '取消盘点',
      { confirmButtonText: '确定', cancelButtonText: '返回', type: 'warning' }
    )
    await confirmCheck(row.id, { approved: false })
    ElMessage.success('盘点单已取消')
    fetchList()
  } catch {
    // user cancelled or error
  }
}

// ---------- Detail ----------
const detailVisible = ref(false)
const detailRow = ref<CheckRecord | null>(null)

function showDetail(row: CheckRecord) {
  detailRow.value = { ...row }
  detailVisible.value = true
}

// ---------- Lifecycle ----------
onMounted(() => {
  fetchProducts()
  fetchList()
})
</script>

<style scoped>
.check-result-container {
  padding: 16px;
}

.search-card {
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
