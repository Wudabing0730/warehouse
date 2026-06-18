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
import { reactive, ref } from 'vue'
import { Box, Coin, Upload, Download, Plus, Search, List } from '@element-plus/icons-vue'

const stats = reactive({
  productCount: 156,
  totalStock: 8420,
  todayInbound: 12,
  todayOutbound: 8,
})

const alerts = ref<string[]>([])

const recentOps = ref([
  { time: '2026-06-18 14:30', description: '入库 - 电子元器件 (批次: B20260618-001)', user: '张三' },
  { time: '2026-06-18 13:15', description: '出库 - 五金配件 (批次: B20260615-003)', user: '李四' },
  { time: '2026-06-18 11:00', description: '盘点 - A仓货架A-01', user: '王五' },
  { time: '2026-06-18 09:45', description: '调拨 - A仓 → B仓 (螺丝M6x20)', user: '赵六' },
  { time: '2026-06-18 08:30', description: '入库 - 包装材料 (批次: B20260618-002)', user: '张三' },
])

const quickActions = [
  { label: '入库登记', icon: Plus, path: '/inbound/create' },
  { label: '出库登记', icon: Download, path: '/outbound/create' },
  { label: '库存查询', icon: Search, path: '/base/stocks' },
  { label: '入库查询', icon: List, path: '/inbound/query' },
  { label: '出库查询', icon: List, path: '/outbound/query' },
  { label: '盘点管理', icon: List, path: '/inventory/tasks' },
]
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
</style>
