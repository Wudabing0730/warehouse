<template>
  <div class="base-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>库存预警</span>
          <div class="header-actions">
            <span v-if="autoRefresh" class="auto-refresh-hint">
              每30秒自动刷新
            </span>
            <el-button type="primary" :loading="loading" @click="fetchAlerts">
              <el-icon><Refresh /></el-icon>
              <span>刷新</span>
            </el-button>
          </div>
        </div>
      </template>

      <div v-if="alerts.length === 0 && !loading" class="empty-container">
        <el-empty description="暂无库存预警" />
      </div>

      <div v-else class="alert-list">
        <el-alert
          v-for="alert in alerts"
          :key="alert.id"
          :title="alert.title ?? '库存预警'"
          :description="alert.description ?? formatAlert(alert)"
          type="warning"
          show-icon
          :closable="false"
          class="alert-item"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { getStockAlerts } from '@/api/stock'

const loading = ref(false)
const alerts = ref<any[]>([])

let autoRefresh = false
let timer: ReturnType<typeof setInterval> | null = null

const formatAlert = (alert: any): string => {
  const parts: string[] = []
  if (alert.productCode) parts.push(`产品: ${alert.productCode}`)
  if (alert.productName) parts.push(alert.productName)
  if (alert.locationCode) parts.push(`库位: ${alert.locationCode}`)
  if (alert.currentStock != null) parts.push(`当前库存: ${alert.currentStock}`)
  if (alert.lowerLimit != null) parts.push(`下限: ${alert.lowerLimit}`)
  if (alert.upperLimit != null) parts.push(`上限: ${alert.upperLimit}`)
  if (alert.message) parts.push(alert.message)
  return parts.join('  |  ')
}

const fetchAlerts = async () => {
  loading.value = true
  try {
    const res = await getStockAlerts()
    alerts.value = res.data ?? []
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

// ---- Auto-refresh ----
const startAutoRefresh = () => {
  autoRefresh = true
  timer = setInterval(() => {
    fetchAlerts()
  }, 30_000)
}

const stopAutoRefresh = () => {
  autoRefresh = false
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

onMounted(() => {
  fetchAlerts()
  startAutoRefresh()
})

onUnmounted(() => {
  stopAutoRefresh()
})
</script>

<style scoped>
.base-page {
  padding: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.auto-refresh-hint {
  font-size: 13px;
  color: #909399;
}

.empty-container {
  padding: 40px 0;
}

.alert-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.alert-item {
  margin: 0;
}
</style>
