<template>
  <el-menu
    :default-active="route.path"
    :collapse="appStore.sidebarCollapsed"
    :collapse-transition="false"
    router
    background-color="#304156"
    text-color="#bfcbd9"
    active-text-color="#409eff"
    class="sidebar-menu"
  >
    <template v-for="item in menuItems" :key="item.path">
      <!-- 有子菜单 -->
      <el-sub-menu v-if="item.children && item.children.length" :index="item.path">
        <template #title>
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </template>
        <el-menu-item
          v-for="child in item.children"
          :key="child.path"
          :index="child.path"
        >
          <el-icon><component :is="child.icon" /></el-icon>
          <span>{{ child.title }}</span>
        </el-menu-item>
      </el-sub-menu>

      <!-- 无子菜单 -->
      <el-menu-item v-else :index="item.path">
        <el-icon><component :is="item.icon" /></el-icon>
        <span>{{ item.title }}</span>
      </el-menu-item>
    </template>
  </el-menu>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'
import { useAppStore } from '@/store/app'
import {
  HomeFilled,
  Setting,
  Folder,
  Box,
  Upload,
  Download,
  Share,
  Check,
  DataAnalysis,
  User,
  List,
  Tickets,
  Document,
  WarnTriangleFilled,
  Clock,
  Search,
  Plus,
  Select,
  Edit,
  Memo,
  Reading,
  TrendCharts,
  PieChart,
} from '@element-plus/icons-vue'
import type { Component } from 'vue'

interface MenuItem {
  path: string
  title: string
  icon: Component
  children?: MenuItem[]
  permission?: string
}

const route = useRoute()
const appStore = useAppStore()

const menuItems: MenuItem[] = [
  {
    path: '/dashboard',
    title: '仪表盘',
    icon: HomeFilled,
  },
  {
    path: '/system',
    title: '系统管理',
    icon: Setting,
    children: [
      { path: '/system/users', title: '用户管理', icon: User },
      { path: '/system/roles', title: '角色管理', icon: Tickets },
      { path: '/system/permissions', title: '权限管理', icon: Select },
      { path: '/system/logs', title: '操作日志', icon: Document },
    ],
  },
  {
    path: '/base',
    title: '基础资料',
    icon: Folder,
    children: [
      { path: '/base/products', title: '产品管理', icon: Box },
      { path: '/base/categories', title: '产品类别', icon: List },
      { path: '/base/locations', title: '库位管理', icon: Box },
      { path: '/base/suppliers', title: '供应商管理', icon: User },
      { path: '/base/customers', title: '客户管理', icon: User },
      { path: '/base/stocks', title: '库存管理', icon: Box },
      { path: '/base/alerts', title: '库存预警', icon: WarnTriangleFilled },
    ],
  },
  {
    path: '/inbound',
    title: '入库管理',
    icon: Upload,
    children: [
      { path: '/inbound/create', title: '填写入库单', icon: Plus },
      { path: '/inbound/audit', title: '入库审核', icon: Select },
      { path: '/inbound/query', title: '入库查询', icon: Search },
    ],
  },
  {
    path: '/outbound',
    title: '出库管理',
    icon: Download,
    children: [
      { path: '/outbound/create', title: '填写出库单', icon: Plus },
      { path: '/outbound/audit', title: '出库审核', icon: Select },
      { path: '/outbound/query', title: '出库查询', icon: Search },
    ],
  },
  {
    path: '/borrow',
    title: '借还管理',
    icon: Share,
    children: [
      { path: '/borrow/create', title: '借条登记', icon: Edit },
      { path: '/borrow/return', title: '归还登记', icon: Memo },
      { path: '/borrow/query', title: '借还查询', icon: Search },
    ],
  },
  {
    path: '/inventory',
    title: '盘库管理',
    icon: Check,
    children: [
      { path: '/inventory/tasks', title: '盘点任务', icon: List },
      { path: '/inventory/input', title: '盘点录入', icon: Edit },
      { path: '/inventory/results', title: '盘点结果', icon: Reading },
    ],
  },
  {
    path: '/reports',
    title: '查询报表',
    icon: DataAnalysis,
    children: [
      { path: '/reports/inbound', title: '入库报表', icon: TrendCharts },
      { path: '/reports/outbound', title: '出库报表', icon: TrendCharts },
      { path: '/reports/stock', title: '库存报表', icon: PieChart },
      { path: '/reports/comprehensive', title: '综合报表', icon: Document },
    ],
  },
]
</script>

<style scoped>
.sidebar-menu {
  border-right: none;
  height: calc(100vh - 60px);
  overflow-y: auto;
  overflow-x: hidden;
}

.sidebar-menu:not(.el-menu--collapse) {
  width: 220px;
}

.sidebar-menu::-webkit-scrollbar {
  width: 4px;
}

.sidebar-menu::-webkit-scrollbar-thumb {
  background-color: rgba(255, 255, 255, 0.15);
  border-radius: 2px;
}

.sidebar-menu::-webkit-scrollbar-thumb:hover {
  background-color: rgba(255, 255, 255, 0.25);
}
</style>
