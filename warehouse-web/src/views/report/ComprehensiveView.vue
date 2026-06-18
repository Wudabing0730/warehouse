<template>
  <div class="comprehensive-container">
    <!-- Filter -->
    <el-card class="filter-card" shadow="never">
      <el-form :model="filterForm" inline>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="filterForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Summary Cards -->
    <el-row :gutter="16" class="summary-row">
      <el-col :span="6">
        <el-card shadow="hover" class="summary-card inbound">
          <div class="summary-label">入库总量</div>
          <div class="summary-value">{{ summary.inboundTotal }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="summary-card outbound">
          <div class="summary-label">出库总量</div>
          <div class="summary-value">{{ summary.outboundTotal }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="summary-card stock">
          <div class="summary-label">当前库存</div>
          <div class="summary-value">{{ summary.currentStock }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="summary-card check">
          <div class="summary-label">盘点差异</div>
          <div class="summary-value">{{ summary.checkDiff }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Two-column layout -->
    <el-row :gutter="16" class="content-row">
      <el-col :span="12">
        <el-card class="section-card" shadow="never">
          <template #header>
            <span>最近入库记录</span>
          </template>
          <el-table :data="recentInbound" v-loading="inboundLoading" stripe size="small" style="width: 100%">
            <el-table-column prop="orderNo" label="单号" min-width="140" />
            <el-table-column prop="productName" label="产品名称" min-width="120" />
            <el-table-column prop="quantity" label="数量" width="80" />
            <el-table-column prop="createTime" label="时间" width="160" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="section-card" shadow="never">
          <template #header>
            <span>最近出库记录</span>
          </template>
          <el-table :data="recentOutbound" v-loading="outboundLoading" stripe size="small" style="width: 100%">
            <el-table-column prop="orderNo" label="单号" min-width="140" />
            <el-table-column prop="productName" label="产品名称" min-width="120" />
            <el-table-column prop="quantity" label="数量" width="80" />
            <el-table-column prop="createTime" label="时间" width="160" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- Stock Overview -->
    <el-card class="section-card" shadow="never">
      <template #header>
        <span>库存概览</span>
      </template>
      <el-table :data="stockOverview" v-loading="stockLoading" stripe border style="width: 100%">
        <el-table-column prop="productCode" label="产品编码" min-width="120" />
        <el-table-column prop="productName" label="产品名称" min-width="140" />
        <el-table-column prop="locationName" label="库位" min-width="120" />
        <el-table-column prop="quantity" label="当前库存" width="100" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <template v-if="row.upperLimit != null && row.quantity > row.upperLimit">
              <el-tag type="danger" size="small">超上限</el-tag>
            </template>
            <template v-else-if="row.lowerLimit != null && row.quantity < row.lowerLimit">
              <el-tag type="warning" size="small">低于下限</el-tag>
            </template>
            <template v-else>
              <el-tag type="success" size="small">正常</el-tag>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getInboundReport, getOutboundReport, getStockReport, getComprehensiveReport } from '@/api/report'

// ---------- Filter ----------
const filterForm = reactive({
  dateRange: [] as string[],
})

// ---------- Summary ----------
const summary = reactive({
  inboundTotal: 0,
  outboundTotal: 0,
  currentStock: 0,
  checkDiff: 0,
})

// ---------- Recent Inbound ----------
interface RecentRecord {
  orderNo: string
  productName: string
  quantity: number
  createTime: string
}

const recentInbound = ref<RecentRecord[]>([])
const inboundLoading = ref(false)

// ---------- Recent Outbound ----------
const recentOutbound = ref<RecentRecord[]>([])
const outboundLoading = ref(false)

// ---------- Stock Overview ----------
interface StockRecord {
  productCode: string
  productName: string
  locationName: string
  quantity: number
  upperLimit: number | null
  lowerLimit: number | null
}

const stockOverview = ref<StockRecord[]>([])
const stockLoading = ref(false)

// ---------- Data Fetching ----------
function getDateParams(): Record<string, string> {
  const params: Record<string, string> = {}
  if (filterForm.dateRange && filterForm.dateRange.length === 2) {
    params.startDate = filterForm.dateRange[0]
    params.endDate = filterForm.dateRange[1]
  }
  return params
}

async function fetchAllData() {
  const dateParams = getDateParams()

  // Fetch all data in parallel
  try {
    const comprehensiveRes = await getComprehensiveReport(dateParams)
    const cData = comprehensiveRes.data
    if (cData) {
      summary.inboundTotal = (cData.inboundTotal ?? 0) as number
      summary.outboundTotal = (cData.outboundTotal ?? 0) as number
      summary.currentStock = (cData.currentStock ?? 0) as number
      summary.checkDiff = (cData.checkDiff ?? 0) as number
    }
  } catch {
    // Fallback to individual API calls if comprehensive fails
    summary.inboundTotal = 0
    summary.outboundTotal = 0
    summary.currentStock = 0
    summary.checkDiff = 0
  }

  // Recent inbound
  inboundLoading.value = true
  try {
    const res = await getInboundReport({ ...dateParams, page: 1, size: 5 })
    recentInbound.value = (res.data?.records ?? res.data ?? []) as RecentRecord[]
  } catch {
    recentInbound.value = []
  } finally {
    inboundLoading.value = false
  }

  // Recent outbound
  outboundLoading.value = true
  try {
    const res = await getOutboundReport({ ...dateParams, page: 1, size: 5 })
    recentOutbound.value = (res.data?.records ?? res.data ?? []) as RecentRecord[]
  } catch {
    recentOutbound.value = []
  } finally {
    outboundLoading.value = false
  }

  // Stock overview
  stockLoading.value = true
  try {
    const res = await getStockReport({ page: 1, size: 50 })
    stockOverview.value = (res.data?.records ?? res.data ?? []) as StockRecord[]

    // If summary wasn't set by comprehensive, derive from stock
    if (summary.currentStock === 0) {
      summary.currentStock = stockOverview.value.reduce((acc, r) => acc + (r.quantity ?? 0), 0)
    }
  } catch {
    stockOverview.value = []
  } finally {
    stockLoading.value = false
  }
}

function handleSearch() {
  fetchAllData()
}

// ---------- Lifecycle ----------
onMounted(() => {
  fetchAllData()
})
</script>

<style scoped>
.comprehensive-container {
  padding: 16px;
}

.filter-card {
  margin-bottom: 12px;
}

.summary-row {
  margin-bottom: 16px;
}

.summary-card {
  text-align: center;
  border-top: 3px solid #409eff;
}

.summary-card.inbound {
  border-top-color: #409eff;
}

.summary-card.outbound {
  border-top-color: #e6a23c;
}

.summary-card.stock {
  border-top-color: #67c23a;
}

.summary-card.check {
  border-top-color: #f56c6c;
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

.content-row {
  margin-bottom: 16px;
}

.section-card {
  margin-bottom: 16px;
}
</style>
