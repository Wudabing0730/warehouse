<template>
  <div class="stock-report-container">
    <!-- Filter -->
    <el-card class="filter-card" shadow="never">
      <el-form :model="filterForm" inline>
        <el-form-item label="产品">
          <el-select v-model="filterForm.productId" placeholder="请选择产品" clearable filterable>
            <el-option
              v-for="p in productOptions"
              :key="p.id"
              :label="p.name + ' (' + p.code + ')'"
              :value="p.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="库位">
          <el-input v-model="filterForm.locationName" placeholder="请输入库位名称" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleExport">
            <el-icon style="margin-right: 4px"><Download /></el-icon>
            导出
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Summary Cards -->
    <el-row :gutter="16" class="summary-row">
      <el-col :span="8">
        <el-card shadow="hover" class="summary-card">
          <div class="summary-label">产品种类数</div>
          <div class="summary-value">{{ summary.productVariety }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="summary-card">
          <div class="summary-label">库存总数量</div>
          <div class="summary-value">{{ summary.totalQuantity }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="summary-card">
          <div class="summary-label">预警产品数</div>
          <div class="summary-value warning">{{ summary.warningCount }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Table -->
    <el-card class="table-card" shadow="never">
      <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="productCode" label="产品编码" min-width="120" />
        <el-table-column prop="productName" label="产品名称" min-width="140" />
        <el-table-column prop="locationName" label="库位" min-width="120" />
        <el-table-column prop="quantity" label="当前库存" width="100" />
        <el-table-column prop="upperLimit" label="上限" width="80" />
        <el-table-column prop="lowerLimit" label="下限" width="80" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <template v-if="row.upperLimit != null && row.quantity > row.upperLimit">
              <el-tag type="danger">超上限</el-tag>
            </template>
            <template v-else-if="row.lowerLimit != null && row.quantity < row.lowerLimit">
              <el-tag type="warning">低于下限</el-tag>
            </template>
            <template v-else>
              <el-tag type="success">正常</el-tag>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pagination.pageNo"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import { getStockReport } from '@/api/report'
import { getProductList } from '@/api/product'

// ---------- Filter ----------
const filterForm = reactive({
  productId: '' as number | string,
  locationName: '',
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
    const res = await getProductList({ pageNo: 1, pageSize: 9999 })
    productOptions.value = (res.data?.records ?? res.data ?? []) as ProductOption[]
  } catch {
    // ignore
  }
}

// ---------- Summary ----------
const summary = reactive({
  productVariety: 0,
  totalQuantity: 0,
  warningCount: 0,
})

// ---------- Table ----------
interface StockRecord {
  productCode: string
  productName: string
  locationName: string
  quantity: number
  upperLimit: number | null
  lowerLimit: number | null
}

const tableData = ref<StockRecord[]>([])
const loading = ref(false)
const pagination = reactive({ pageNo: 1, pageSize: 10, total: 0 })

function buildParams(): Record<string, unknown> {
  const params: Record<string, unknown> = {
    pageNo: pagination.pageNo,
    pageSize: pagination.pageSize,
  }
  if (filterForm.productId) params.productId = filterForm.productId
  if (filterForm.locationName) params.locationName = filterForm.locationName
  return params
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getStockReport(buildParams())
    const data = res.data
    tableData.value = (data.records ?? data ?? []) as StockRecord[]

    const records = tableData.value

    // Product variety: distinct product names/codes
    const distinctProducts = new Set(records.map((r) => r.productCode))
    summary.productVariety = distinctProducts.size

    // Total quantity
    summary.totalQuantity = records.reduce((acc, r) => acc + (r.quantity ?? 0), 0)

    // Warning count: over upper or below lower
    summary.warningCount = records.filter(
      (r) =>
        (r.upperLimit != null && r.quantity > r.upperLimit) ||
        (r.lowerLimit != null && r.quantity < r.lowerLimit)
    ).length

    pagination.total = (data.total ?? records.length) as number
  } catch {
    // ignore
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.pageNo = 1
  fetchData()
}

function handleExport() {
  ElMessage.info('导出功能开发中')
}

// ---------- Lifecycle ----------
onMounted(() => {
  fetchProducts()
  fetchData()
})
</script>

<style scoped>
.stock-report-container {
  padding: 16px;
}

.filter-card {
  margin-bottom: 12px;
}

.summary-row {
  margin-bottom: 12px;
}

.summary-card {
  text-align: center;
}

.summary-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.summary-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
}

.summary-value.warning {
  color: #e6a23c;
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
