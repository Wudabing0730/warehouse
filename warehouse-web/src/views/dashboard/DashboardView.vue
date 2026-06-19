<template>
  <div class="dashboard">
    <!-- Stat Cards Row -->
    <el-row :gutter="20" class="stat-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background:#e6f7ff">
              <el-icon :size="28" color="#1890ff"><Box /></el-icon>
            </div>
            <div class="stat-text">
              <div class="stat-value">{{ stats.productCount }}</div>
              <div class="stat-label">产品总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background:#f6ffed">
              <el-icon :size="28" color="#52c41a"><Coin /></el-icon>
            </div>
            <div class="stat-text">
              <div class="stat-value">{{ stats.totalStock.toLocaleString() }}</div>
              <div class="stat-label">库存总量</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background:#e6fffb">
              <el-icon :size="28" color="#13c2c2"><Upload /></el-icon>
            </div>
            <div class="stat-text">
              <div class="stat-value">{{ stats.todayInbound }}</div>
              <div class="stat-label">今日入库</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background:#fff7e6">
              <el-icon :size="28" color="#fa8c16"><Download /></el-icon>
            </div>
            <div class="stat-text">
              <div class="stat-value">{{ stats.todayOutbound }}</div>
              <div class="stat-label">今日出库</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- P3-2: 7 天出入库趋势 ECharts 折线图 -->
    <el-row :gutter="20" style="margin-top:20px">
      <el-col :span="24">
        <el-card header="最近 7 天出入库趋势">
          <div ref="chartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Bottom Cards Row -->
    <el-row :gutter="20" style="margin-top:20px">
      <el-col :span="12">
        <el-card header="库存预警">
          <el-empty v-if="alerts.length === 0" description="暂无预警" :image-size="60" />
          <div v-else v-for="(a, idx) in alerts" :key="idx" class="alert-item">{{ a }}</div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card header="快速入口">
          <el-row :gutter="12">
            <el-col :span="8" v-for="item in quickActions" :key="item.label" style="margin-bottom:12px">
              <el-button :icon="item.icon" style="width:100%" @click="$router.push(item.path)">
                {{ item.label }}
              </el-button>
            </el-col>
          </el-row>
        </el-card>
      </el-col>
    </el-row>

    <!-- Recent Operations Row -->
    <el-row :gutter="20" style="margin-top:20px">
      <el-col :span="24">
        <el-card header="最近操作">
          <el-empty v-if="recentOps.length === 0" description="暂无操作记录" :image-size="60" />
          <div v-else v-for="(op, idx) in recentOps" :key="idx" class="op-item">
            <span class="op-time">{{ op.time }}</span>
            <span class="op-desc">{{ op.description }}</span>
            <span class="op-user">{{ op.user }}</span>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { Box, Coin, Upload, Download, Plus, Search, List } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { getDashboardSummary, type RecentOperation } from '@/api/dashboard'

// P0-8 修复:不再使用硬编码假数据,初始化为零值,onMounted 时拉取真实接口
const stats = reactive({
  productCount: 0,
  totalStock: 0,
  todayInbound: 0,
  todayOutbound: 0,
})

const alerts = ref<string[]>([])
const recentOps = ref<RecentOperation[]>([])

// P3-2: 7 天趋势数据 + ECharts 实例
const trendDates = ref<string[]>([])
const inboundTrend = ref<number[]>([])
const outboundTrend = ref<number[]>([])
const chartRef = ref<HTMLDivElement | null>(null)
let chartInstance: echarts.ECharts | null = null

function buildChartOption(): echarts.EChartsOption {
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['入库单数', '出库单数'], top: 0 },
    grid: { left: 40, right: 20, top: 40, bottom: 30 },
    xAxis: {
      type: 'category',
      data: trendDates.value,
      axisLabel: { color: '#909399' },
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLabel: { color: '#909399' },
    },
    series: [
      {
        name: '入库单数',
        type: 'line',
        data: inboundTrend.value,
        smooth: true,
        itemStyle: { color: '#13c2c2' },
        areaStyle: { color: 'rgba(19,194,194,0.15)' },
      },
      {
        name: '出库单数',
        type: 'line',
        data: outboundTrend.value,
        smooth: true,
        itemStyle: { color: '#fa8c16' },
        areaStyle: { color: 'rgba(250,140,22,0.15)' },
      },
    ],
  }
}

function renderChart() {
  if (!chartRef.value) return
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }
  chartInstance.setOption(buildChartOption())
}

function disposeChart() {
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
}

async function fetchDashboardData() {
  try {
    const res: any = await getDashboardSummary()
    // 后端返回 Result<T> 结构 { code, data, ... },request 拦截器已自动拆 data
    const summary = res?.data ?? res
    if (!summary) return
    stats.productCount = summary.productCount ?? 0
    stats.totalStock = summary.totalStock ?? 0
    stats.todayInbound = summary.todayInbound ?? 0
    stats.todayOutbound = summary.todayOutbound ?? 0
    alerts.value = summary.alerts ?? []
    recentOps.value = summary.recentOps ?? []
    // P3-2: 7 天趋势
    trendDates.value = summary.trendDates ?? []
    inboundTrend.value = summary.inboundTrend ?? []
    outboundTrend.value = summary.outboundTrend ?? []
    await nextTick()
    renderChart()
  } catch (e) {
    // Bug fix:不能静默吞错 — 必须给用户反馈 + 留 console 排查线索
    console.error('[Dashboard] load failed:', e)
    ElMessage.error('加载仪表盘数据失败,请稍后重试')
  }
}

const quickActions = [
  { label: '入库登记', icon: Plus, path: '/inbound/create' },
  { label: '出库登记', icon: Download, path: '/outbound/create' },
  { label: '库存查询', icon: Search, path: '/base/stocks' },
  { label: '入库查询', icon: List, path: '/inbound/query' },
  { label: '出库查询', icon: List, path: '/outbound/query' },
  { label: '盘点管理', icon: List, path: '/inventory/tasks' },
]

onMounted(() => {
  fetchDashboardData()
  window.addEventListener('resize', renderChart)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', renderChart)
  disposeChart()
})
</script>

<style scoped>
.dashboard {
  padding: 20px;
}
.stat-card {
  border-left: 4px solid transparent;
}
.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}
.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
}
.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}
.alert-item {
  padding: 8px 0;
  border-bottom: 1px solid #ebeef5;
  color: #e6a23c;
  font-size: 13px;
}
.op-item {
  padding: 10px 0;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 13px;
}
.op-time {
  color: #909399;
  min-width: 120px;
}
.op-desc {
  color: #303133;
  flex: 1;
}
.op-user {
  color: #409eff;
}
/* P3-2: ECharts 容器固定高度 */
.chart-container {
  width: 100%;
  height: 320px;
}
</style>

<style scoped>
.dashboard {
  padding: 20px;
}
.stat-card {
  border-left: 4px solid transparent;
}
.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}
.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
}
.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}
.alert-item {
  padding: 8px 0;
  border-bottom: 1px solid #ebeef5;
  color: #e6a23c;
  font-size: 13px;
}
.op-item {
  padding: 10px 0;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 13px;
}
.op-time {
  color: #909399;
  min-width: 120px;
}
.op-desc {
  color: #303133;
  flex: 1;
}
.op-user {
  color: #409eff;
}
</style>