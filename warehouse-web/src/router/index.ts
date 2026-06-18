import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { getToken } from '@/utils/auth'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { title: '登录' },
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/components/AppLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/dashboard/DashboardView.vue'), meta: { title: '仪表盘' } },
      { path: 'system/users', name: 'Users', component: () => import('@/views/system/UserView.vue'), meta: { title: '用户管理' } },
      { path: 'system/roles', name: 'Roles', component: () => import('@/views/system/RoleView.vue'), meta: { title: '角色管理' } },
      { path: 'system/permissions', name: 'Permissions', component: () => import('@/views/system/PermissionView.vue'), meta: { title: '权限管理' } },
      { path: 'system/logs', name: 'OperationLogs', component: () => import('@/views/system/OperationLogView.vue'), meta: { title: '操作日志' } },
      { path: 'base/products', name: 'Products', component: () => import('@/views/base/ProductView.vue'), meta: { title: '产品管理' } },
      { path: 'base/categories', name: 'Categories', component: () => import('@/views/base/CategoryView.vue'), meta: { title: '产品类别' } },
      { path: 'base/locations', name: 'Locations', component: () => import('@/views/base/LocationView.vue'), meta: { title: '库位管理' } },
      { path: 'base/suppliers', name: 'Suppliers', component: () => import('@/views/base/SupplierView.vue'), meta: { title: '供应商管理' } },
      { path: 'base/customers', name: 'Customers', component: () => import('@/views/base/CustomerView.vue'), meta: { title: '客户管理' } },
      { path: 'base/stocks', name: 'Stocks', component: () => import('@/views/base/StockInitView.vue'), meta: { title: '库存管理' } },
      { path: 'base/alerts', name: 'Alerts', component: () => import('@/views/base/AlertSettingView.vue'), meta: { title: '库存预警' } },
      { path: 'inbound/create', name: 'InboundCreate', component: () => import('@/views/inbound/InboundFormView.vue'), meta: { title: '填写入库单' } },
      { path: 'inbound/audit', name: 'InboundAudit', component: () => import('@/views/inbound/InboundAuditView.vue'), meta: { title: '入库审核' } },
      { path: 'inbound/query', name: 'InboundQuery', component: () => import('@/views/inbound/InboundQueryView.vue'), meta: { title: '入库查询' } },
      { path: 'outbound/create', name: 'OutboundCreate', component: () => import('@/views/outbound/OutboundFormView.vue'), meta: { title: '填写出库单' } },
      { path: 'outbound/audit', name: 'OutboundAudit', component: () => import('@/views/outbound/OutboundAuditView.vue'), meta: { title: '出库审核' } },
      { path: 'outbound/query', name: 'OutboundQuery', component: () => import('@/views/outbound/OutboundQueryView.vue'), meta: { title: '出库查询' } },
      { path: 'borrow/create', name: 'BorrowCreate', component: () => import('@/views/borrow/BorrowFormView.vue'), meta: { title: '借条登记' } },
      { path: 'borrow/return', name: 'BorrowReturn', component: () => import('@/views/borrow/ReturnFormView.vue'), meta: { title: '归还登记' } },
      { path: 'borrow/query', name: 'BorrowQuery', component: () => import('@/views/borrow/BorrowQueryView.vue'), meta: { title: '借还查询' } },
      { path: 'inventory/tasks', name: 'InventoryTasks', component: () => import('@/views/inventory/CheckTaskView.vue'), meta: { title: '盘点任务' } },
      { path: 'inventory/input', name: 'InventoryInput', component: () => import('@/views/inventory/CheckInputView.vue'), meta: { title: '盘点录入' } },
      { path: 'inventory/results', name: 'InventoryResults', component: () => import('@/views/inventory/CheckResultView.vue'), meta: { title: '盘点结果' } },
      { path: 'reports/inbound', name: 'InboundReport', component: () => import('@/views/report/InboundReportView.vue'), meta: { title: '入库报表' } },
      { path: 'reports/outbound', name: 'OutboundReport', component: () => import('@/views/report/OutboundReportView.vue'), meta: { title: '出库报表' } },
      { path: 'reports/stock', name: 'StockReport', component: () => import('@/views/report/StockReportView.vue'), meta: { title: '库存报表' } },
      { path: 'reports/comprehensive', name: 'ComprehensiveReport', component: () => import('@/views/report/ComprehensiveView.vue'), meta: { title: '综合报表' } },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, _from, next) => {
  document.title = (to.meta.title as string) || '仓库管理系统'
  if (to.path !== '/login' && !getToken()) {
    next('/login')
  } else {
    next()
  }
})

export default router
