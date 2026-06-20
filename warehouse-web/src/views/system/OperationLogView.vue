<template>
  <div class="operation-log-view">
    <!-- Search Bar -->
    <el-card class="search-card" shadow="never">
      <el-form :model="searchForm" inline>
        <el-form-item label="用户ID">
          <el-input
            v-model="searchForm.userId"
            placeholder="请输入用户ID"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="模块">
          <el-select v-model="searchForm.module" placeholder="请选择模块" clearable style="width: 150px">
            <el-option label="入库管理" value="inbound" />
            <el-option label="出库管理" value="outbound" />
            <el-option label="系统管理" value="system" />
            <el-option label="库存管理" value="inventory" />
            <el-option label="报表管理" value="report" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable style="width: 120px">
            <!-- P0-6: 后端 OperationLogQueryDTO.status 是 Integer(1=成功/0=失败),
                 之前发 'success'/'fail' 字符串永远查不到,改为 :value 整型绑定 -->
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作时间">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Data Table -->
    <el-card class="table-card" shadow="never">
      <template #header>
        <span>操作日志</span>
      </template>
      <!-- row-key 必须与后端 OperationLogVO.logId 对齐;OperationLogVO 没有
           @JsonProperty("id") 别名,所以不能用 "id" 当 rowKey,否则所有行
           rowKey=undefined,点击任意一行的展开箭头会让所有行一起展开。 -->
      <el-table
        :data="tableData"
        border
        stripe
        v-loading="loading"
        style="width: 100%"
        row-key="logId"
        @expand-change="handleExpandChange"
      >
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="expand-content">
              <div class="expand-section" v-if="row.requestParams">
                <h4>请求参数</h4>
                <pre class="expand-json">{{ formatJson(row.requestParams) }}</pre>
              </div>
              <div class="expand-section" v-if="row.responseResult">
                <h4>响应结果</h4>
                <pre class="expand-json">{{ formatJson(row.responseResult) }}</pre>
              </div>
              <div v-if="!row.requestParams && !row.responseResult" class="expand-empty">
                暂无详细数据
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="logId" label="ID" width="80" align="center" />
        <el-table-column prop="username" label="用户" width="120" />
        <el-table-column prop="description" label="操作描述" min-width="180" show-overflow-tooltip />
        <el-table-column label="模块" width="120" align="center">
          <template #default="{ row }">
            <span>{{ getModuleLabel(row.module) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="请求方法" width="100" align="center">
          <template #default="{ row }">
            <el-tag
              size="small"
              :type="getMethodTagType(row.requestMethod)"
            >
              {{ row.requestMethod }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="requestUrl" label="请求URL" min-width="200" show-overflow-tooltip />
        <el-table-column prop="executeTime" label="执行耗时(ms)" width="120" align="center" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <!-- P0-6: 后端返回 1/0,前端展示按整型比对 -->
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ip" label="IP地址" width="150" />
        <el-table-column prop="createTime" label="操作时间" width="180" />
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSearch"
          @current-change="handleSearch"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getLogList } from '@/api/log'

interface LogEntry {
  logId: number
  userId?: number
  username: string
  description: string
  module: string
  requestMethod: string
  requestUrl: string
  executeTime: number
  /** P0-6: 后端返回整型 1=成功 / 0=失败 */
  status: number
  ip: string
  createTime: string
  requestParams?: string
  responseResult?: string
}

// ---------- Search ----------
const searchForm = reactive({
  userId: '',
  module: '' as string,
  /** P0-6: 改为 number,与后端 Integer 对齐 */
  status: null as number | null,
  dateRange: [] as string[],
})

// ---------- Table ----------
const tableData = ref<LogEntry[]>([])
const loading = ref(false)
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0,
})

async function fetchData() {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      page: pagination.page,
      size: pagination.size,
    }
    if (searchForm.userId) params.userId = searchForm.userId
    if (searchForm.module) params.module = searchForm.module
    if (searchForm.status) params.status = searchForm.status
    if (searchForm.dateRange && searchForm.dateRange.length === 2) {
      params.startTime = searchForm.dateRange[0]
      params.endTime = searchForm.dateRange[1]
    }

    const res = await getLogList(params)
    const data = res.data || res
    tableData.value = data.records ?? data.list ?? []
    pagination.total = data.total ?? 0
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  fetchData()
}

function handleReset() {
  searchForm.userId = ''
  searchForm.module = ''
  searchForm.status = null
  searchForm.dateRange = []
  handleSearch()
}

// ---------- Expand row ----------
function handleExpandChange(row: LogEntry, expandedRows: LogEntry[]) {
  // expand-change provides the row and the currently expanded rows
}

// ---------- Helpers ----------
const moduleMap: Record<string, string> = {
  inbound: '入库管理',
  outbound: '出库管理',
  system: '系统管理',
  inventory: '库存管理',
  report: '报表管理',
}

function getModuleLabel(module: string): string {
  return moduleMap[module] ?? module ?? '-'
}

function getMethodTagType(method: string): '' | 'success' | 'info' | 'warning' | 'danger' {
  const map: Record<string, '' | 'success' | 'info' | 'warning' | 'danger'> = {
    GET: 'success',
    POST: 'primary',
    PUT: 'warning',
    DELETE: 'danger',
    PATCH: 'info',
  }
  return map[method?.toUpperCase()] ?? 'info'
}

function formatJson(value: string): string {
  if (!value) return '-'
  try {
    const obj = typeof value === 'string' ? JSON.parse(value) : value
    return JSON.stringify(obj, null, 2)
  } catch {
    return value
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.operation-log-view {
  padding: 16px;
}
.search-card {
  margin-bottom: 16px;
}
.table-card {
  min-height: 400px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
.expand-content {
  padding: 12px 24px;
  background: #fafafa;
}
.expand-section {
  margin-bottom: 12px;
}
.expand-section h4 {
  margin: 0 0 8px 0;
  font-size: 14px;
  color: #303133;
}
.expand-json {
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 12px;
  font-family: 'Courier New', Courier, monospace;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
  max-height: 300px;
  overflow-y: auto;
}
.expand-empty {
  color: #909399;
  text-align: center;
  padding: 16px 0;
}
</style>
