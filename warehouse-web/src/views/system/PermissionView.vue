<template>
  <div class="permission-view">
    <el-card class="tree-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>权限列表</span>
          <div class="header-actions">
            <el-button @click="expandAll">展开全部</el-button>
            <el-button @click="collapseAll">折叠全部</el-button>
          </div>
        </div>
      </template>
      <el-table
        ref="tableRef"
        :data="treeData"
        border
        stripe
        v-loading="loading"
        row-key="permissionId"
        :tree-props="{ children: 'children' }"
        style="width: 100%"
      >
        <el-table-column prop="permissionName" label="名称" min-width="200" />
        <el-table-column prop="permissionCode" label="权限编码" width="220" />
        <el-table-column label="资源类型" width="120" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.resourceType" size="small" type="info">
              {{ row.resourceType }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="路径" min-width="200" show-overflow-tooltip />
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.type === 1 ? 'primary' : 'warning'">
              {{ row.type === 1 ? '菜单' : '按钮' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" align="center" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getPermissionTree } from '@/api/permission'

interface PermissionNode {
  permissionId: number
  permissionCode?: string
  permissionName: string
  resourceType?: string
  path?: string
  type?: number
  sort?: number
  children?: PermissionNode[]
}

// 必须与后端 PermissionVO 字段一致;permissionId 是 tree-table 的 rowKey,
// children 是子节点字段。若错配为 id/name 会导致 el-table 把所有行视为同一行,
// 点击任何一行的展开箭头都会把所有行一起展开。
const treeData = ref<PermissionNode[]>([])
const tableRef = ref()
const loading = ref(false)

function collectKeys(nodes: PermissionNode[], out: number[] = []): number[] {
  for (const node of nodes) {
    if (node.permissionId !== undefined && node.permissionId !== null) {
      out.push(node.permissionId)
    }
    if (node.children?.length) {
      collectKeys(node.children, out)
    }
  }
  return out
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getPermissionTree()
    const data = (res as { data?: PermissionNode[] }).data ?? (res as PermissionNode[])
    treeData.value = Array.isArray(data) ? data : []
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

function expandAll() {
  const keys = collectKeys(treeData.value)
  keys.forEach((k) => tableRef.value?.toggleRowExpansion(k, true))
}

function collapseAll() {
  const keys = collectKeys(treeData.value)
  keys.forEach((k) => tableRef.value?.toggleRowExpansion(k, false))
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.permission-view {
  padding: 16px;
}
.tree-card {
  min-height: 200px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.header-actions {
  display: flex;
  gap: 8px;
}
</style>