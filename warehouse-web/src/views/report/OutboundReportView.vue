<template>
  <div class="outbound-report-container">
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
          <div class="summary-label">出库总次数</div>
          <div class="summary-value">{{ summary.totalCount }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="summary-card">
          <div class="summary-label">出库总数量</div>
          <div class="summary-value">{{ summary.totalQuantity }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="summary-card">
          <div class="summary-label">出库总金额</div>
          <div class="summary-value">¥{{ summary.totalAmount.toFixed(2) }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Table -->
    <el-card class="table-card" shadow="never">
      <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="productCode" label="产品编码" min-width="120" />
        <el-table-column prop="productName" label="产品名称" min-width="140" />
        <el-table-column prop="quantity" label="出库数量" width="120" />
        <el-table-column label="出库金额" width="140">
          <template #default="{ row }">
            ¥{{ (row.amount ?? 0).toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column prop="count" label="出库次数" width="100" />
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
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
import { getOutboundReport } from '@/api/report'

// ---------- Filter ----------
const filterForm = reactive({
  dateRange: [] as string[],
})

// ---------- Summary ----------
const summary = reactive({
  totalCount: 0,
  totalQuantity: 0,
  totalAmount: 0,
})

// ---------- Table ----------
interface OutboundRecord {
  productCode: string
  productName: string
  quantity: number
  amount: number
  count: number
}

const tableData = ref<OutboundRecord[]>([])
const loading = ref(false)
const pagination = reactive({ page: 1, size: 10, total: 0 })

function buildParams(): Record<string, unknown> {
  const params: Record<string, unknown> = {
    page: pagination.page,
    size: pagination.size,
  }
  if (filterForm.dateRange && filterForm.dateRange.length === 2) {
    params.startDate = filterForm.dateRange[0]
    params.endDate = filterForm.dateRange[1]
  }
  return params
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getOutboundReport(buildParams())
    const data = res.data
    tableData.value = (data.records ?? data ?? []) as OutboundRecord[]

    const records = tableData.value
    summary.totalCount = records.reduce((acc, r) => acc + (r.count ?? 0), 0)
    summary.totalQuantity = records.reduce((acc, r) => acc + (r.quantity ?? 0), 0)
    summary.totalAmount = records.reduce((acc, r) => acc + (r.amount ?? 0), 0)

    pagination.total = (data.total ?? records.length) as number
  } catch {
    // ignore
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  fetchData()
}

function handleExport() {
  ElMessage.info('导出功能开发中')
}

// ---------- Lifecycle ----------
onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.outbound-report-container {
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

.table-card {
  margin-bottom: 12px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
