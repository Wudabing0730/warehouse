<template>
  <div class="inbound-query">
    <el-card class="search-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="单号">
          <el-input v-model="searchForm.orderNo" placeholder="输入单号" clearable />
        </el-form-item>
        <el-form-item label="供应商">
          <el-select v-model="searchForm.supplierId" placeholder="选择供应商" filterable clearable>
            <el-option v-for="s in supplierList" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="选择状态" clearable>
            <el-option label="待审核" :value="0" />
            <el-option label="已审核" :value="1" />
            <el-option label="已拒绝" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="订单时间">
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
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column prop="orderNo" label="单号" width="180" />
        <el-table-column label="供应商" min-width="150">
          <template #default="{ row }">{{ row.supplierName ?? '--' }}</template>
        </el-table-column>
        <el-table-column label="操作人" width="120">
          <template #default="{ row }">{{ row.operatorName ?? row.operator ?? '--' }}</template>
        </el-table-column>
        <el-table-column label="审核人" width="120">
          <template #default="{ row }">{{ row.auditorName ?? row.auditor ?? '--' }}</template>
        </el-table-column>
        <el-table-column prop="orderTime" label="订单时间" width="180" />
        <el-table-column label="审核时间" width="180">
          <template #default="{ row }">{{ row.auditTime ?? '--' }}</template>
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
            <el-button type="primary" size="small" link @click="openDetail(row)">
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="fetchData"
          @size-change="fetchData"
        />
      </div>
    </el-card>

    <!-- Detail Dialog (read-only) -->
    <el-dialog v-model="dialogVisible" title="入库单详情" width="900px" destroy-on-close>
      <div v-if="currentRow" class="detail-content">
        <el-descriptions :column="3" border>
          <el-descriptions-item label="单号">{{ currentRow.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="供应商">{{ currentRow.supplierName ?? '--' }}</el-descriptions-item>
          <el-descriptions-item label="订单时间">{{ currentRow.orderTime }}</el-descriptions-item>
          <el-descriptions-item label="操作人">{{ currentRow.operatorName ?? currentRow.operator ?? '--' }}</el-descriptions-item>
          <el-descriptions-item label="审核人">{{ currentRow.auditorName ?? currentRow.auditor ?? '--' }}</el-descriptions-item>
          <el-descriptions-item label="审核时间">{{ currentRow.auditTime ?? '--' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTagType(currentRow.status)" size="small">
              {{ statusLabel(currentRow.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="审核备注">{{ currentRow.auditRemark ?? '--' }}</el-descriptions-item>
          <el-descriptions-item label="备注">{{ currentRow.remark || '--' }}</el-descriptions-item>
        </el-descriptions>

        <h4 style="margin: 16px 0 8px">明细列表</h4>
        <el-table :data="currentRow.details ?? []" border stripe size="small">
          <el-table-column label="产品" min-width="150">
            <template #default="{ row: d }">{{ d.productName ?? '--' }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="100" />
          <el-table-column prop="unitPrice" label="单价" width="100" />
          <el-table-column label="目标库位" min-width="150">
            <template #default="{ row: d }">{{ d.locationName ?? d.locationId ?? '--' }}</template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import { getInboundList, getInboundById } from '@/api/inbound'
import { getSupplierList } from '@/api/supplier'

const loading = ref(false)
const dialogVisible = ref(false)
const currentRow = ref<any>(null)

const supplierList = ref<any[]>([])

const searchForm = reactive({
  orderNo: '',
  supplierId: null as number | null,
  status: null as number | null,
  dateRange: null as [string, string] | null,
})

const pagination = reactive({ page: 1, pageSize: 10, total: 0 })
const tableData = ref<any[]>([])

function statusTagType(status: number): 'warning' | 'success' | 'danger' | 'info' {
  if (status === 0) return 'warning'
  if (status === 1) return 'success'
  if (status === 2) return 'danger'
  return 'info'
}

function statusLabel(status: number): string {
  if (status === 0) return '待审核'
  if (status === 1) return '已审核'
  if (status === 2) return '已拒绝'
  return '未知'
}

onMounted(() => {
  loadSuppliers()
  fetchData()
})

async function loadSuppliers() {
  try {
    const res = await getSupplierList({ pageSize: 999 })
    supplierList.value = res.data?.records ?? res.data ?? []
  } catch {
    // handled by interceptor
  }
}

async function fetchData() {
  loading.value = true
  try {
    const params: any = {
      page: pagination.page,
      pageSize: pagination.pageSize,
    }
    if (searchForm.orderNo) params.orderNo = searchForm.orderNo
    if (searchForm.supplierId) params.supplierId = searchForm.supplierId
    if (searchForm.status !== null && searchForm.status !== '') params.status = searchForm.status
    if (searchForm.dateRange) {
      params.startTime = searchForm.dateRange[0]
      params.endTime = searchForm.dateRange[1]
    }
    const res = await getInboundList(params)
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
  searchForm.orderNo = ''
  searchForm.supplierId = null
  searchForm.status = null
  searchForm.dateRange = null
  pagination.page = 1
  fetchData()
}

async function openDetail(row: any) {
  loading.value = true
  try {
    const res = await getInboundById(row.id)
    currentRow.value = res.data
    dialogVisible.value = true
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.inbound-query {
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
