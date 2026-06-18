<template>
  <div class="outbound-audit">
    <el-card class="search-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="单号">
          <el-input v-model="searchForm.orderNo" placeholder="输入单号" clearable />
        </el-form-item>
        <el-form-item label="客户">
          <el-select v-model="searchForm.customerId" placeholder="选择客户" filterable clearable>
            <el-option v-for="c in customerList" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="选择状态" clearable>
            <el-option label="待审核" :value="0" />
            <el-option label="已审核" :value="1" />
            <el-option label="已拒绝" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="出库时间">
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
      <el-table
        :data="tableData"
        border
        stripe
        v-loading="loading"
        @row-click="handleRowClick"
        style="cursor: pointer"
      >
        <el-table-column prop="orderNo" label="单号" width="180" />
        <el-table-column label="客户" min-width="150">
          <template #default="{ row }">{{ row.customerName ?? '--' }}</template>
        </el-table-column>
        <el-table-column label="操作人" width="120">
          <template #default="{ row }">{{ row.operatorName ?? row.operator ?? '--' }}</template>
        </el-table-column>
        <el-table-column prop="orderTime" label="出库时间" width="180" />
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
              v-if="row.status === 0"
              type="primary"
              size="small"
              link
              @click.stop="openAudit(row)"
            >
              审核
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

    <!-- Audit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogReadonly ? '出库单详情' : '审核出库单'"
      width="900px"
      destroy-on-close
    >
      <div v-if="currentRow" class="audit-detail">
        <el-descriptions :column="3" border>
          <el-descriptions-item label="单号">{{ currentRow.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="客户">{{ currentRow.customerName ?? '--' }}</el-descriptions-item>
          <el-descriptions-item label="出库时间">{{ currentRow.orderTime }}</el-descriptions-item>
          <el-descriptions-item label="操作人">{{ currentRow.operatorName ?? currentRow.operator ?? '--' }}</el-descriptions-item>
          <el-descriptions-item label="部门">{{ currentRow.department || '--' }}</el-descriptions-item>
          <el-descriptions-item label="申请人">{{ currentRow.applicant || '--' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTagType(currentRow.status)" size="small">
              {{ statusLabel(currentRow.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="备注">{{ currentRow.remark || '--' }}</el-descriptions-item>
        </el-descriptions>

        <h4 style="margin: 16px 0 8px">明细列表</h4>
        <el-table :data="currentRow.details ?? []" border stripe size="small">
          <el-table-column label="产品" min-width="150">
            <template #default="{ row: d }">{{ d.productName ?? '--' }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="100" />
          <el-table-column prop="unitPrice" label="单价" width="100" />
        </el-table>

        <div v-if="!dialogReadonly" class="audit-actions">
          <el-divider />
          <el-form :model="auditForm" label-width="80px">
            <el-form-item label="审核备注">
              <el-input
                v-model="auditForm.remark"
                type="textarea"
                :rows="2"
                placeholder="请输入审核备注"
              />
            </el-form-item>
          </el-form>
          <div class="audit-buttons">
            <el-button type="success" :loading="auditing" @click="handleApprove(true)">
              通过
            </el-button>
            <el-button type="danger" :loading="auditing" @click="handleApprove(false)">
              拒绝
            </el-button>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import { getOutboundList, getOutboundById, confirmOutbound } from '@/api/outbound'
import { getCustomerList } from '@/api/customer'

const loading = ref(false)
const dialogVisible = ref(false)
const dialogReadonly = ref(false)
const auditing = ref(false)
const currentRow = ref<any>(null)

const customerList = ref<any[]>([])

const searchForm = reactive({
  orderNo: '',
  customerId: null as number | null,
  status: null as number | null,
  dateRange: null as [string, string] | null,
})

const pagination = reactive({ page: 1, size: 10, total: 0 })
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
  loadCustomers()
  fetchData()
})

async function loadCustomers() {
  try {
    const res = await getCustomerList({ size: 999 })
    customerList.value = res.data?.records ?? res.data ?? []
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
    if (searchForm.orderNo) params.orderNo = searchForm.orderNo
    if (searchForm.customerId) params.customerId = searchForm.customerId
    if (searchForm.status !== null && searchForm.status !== '') params.status = searchForm.status
    if (searchForm.dateRange) {
      params.startTime = searchForm.dateRange[0]
      params.endTime = searchForm.dateRange[1]
    }
    const res = await getOutboundList(params)
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
  searchForm.customerId = null
  searchForm.status = null
  searchForm.dateRange = null
  pagination.page = 1
  fetchData()
}

function handleRowClick(row: any) {
  openDetail(row)
}

async function openDetail(row: any) {
  loading.value = true
  try {
    const res = await getOutboundById(row.id)
    currentRow.value = res.data
    dialogReadonly.value = true
    dialogVisible.value = true
  } finally {
    loading.value = false
  }
}

function openAudit(row: any) {
  openDetailForAudit(row)
}

async function openDetailForAudit(row: any) {
  loading.value = true
  try {
    const res = await getOutboundById(row.id)
    currentRow.value = res.data
    dialogReadonly.value = false
    dialogVisible.value = true
  } finally {
    loading.value = false
  }
}

const auditForm = reactive({ remark: '' })

async function handleApprove(approved: boolean) {
  if (!currentRow.value) return
  const action = approved ? '通过' : '拒绝'
  try {
    await ElMessageBox.confirm(`确认${action}该出库单？`, '提示', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }

  auditing.value = true
  try {
    await confirmOutbound(currentRow.value.id, {
      approved,
      remark: auditForm.remark,
    })
    ElMessage.success(`出库单已${action}`)
    dialogVisible.value = false
    auditForm.remark = ''
    fetchData()
  } catch {
    // handled by interceptor
  } finally {
    auditing.value = false
  }
}
</script>

<style scoped>
.outbound-audit {
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

.audit-actions {
  margin-top: 8px;
}

.audit-buttons {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 16px;
}
</style>
