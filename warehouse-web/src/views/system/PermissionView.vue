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
        :data="flatList"
        border
        stripe
        v-loading="loading"
        row-key="id"
        style="width: 100%"
        default-expand-all
      >
        <el-table-column prop="name" label="名称" width="240">
          <template #default="{ row }">
            <span :style="{ paddingLeft: (row._level ?? 0) * 24 + 'px' }">{{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="code" label="权限编码" width="220" />
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

    <!-- Also show as tree for visual hierarchy -->
    <el-card class="tree-card" shadow="never" style="margin-top: 16px">
      <template #header>
        <span>权限树</span>
      </template>
      <el-tree
        :data="treeData"
        :props="{ label: 'name', children: 'children' }"
        default-expand-all
        node-key="id"
        highlight-current
      >
        <template #default="{ data }">
          <div class="tree-node">
            <span class="tree-node-name">{{ data.name }}</span>
            <el-tag size="small" type="primary" class="tree-node-code">{{ data.code }}</el-tag>
            <el-tag v-if="data.resourceType" size="small" type="info" class="tree-node-badge">
              {{ data.resourceType }}
            </el-tag>
          </div>
        </template>
      </el-tree>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getPermissionTree } from '@/api/permission'

interface PermissionNode {
  id: number
  name: string
  code: string
  resourceType?: string
  path?: string
  type?: number
  sort?: number
  children?: PermissionNode[]
}

interface FlatPermission extends PermissionNode {
  _level?: number
}

const treeData = ref<PermissionNode[]>([])
const flatList = ref<FlatPermission[]>([])
const loading = ref(false)

function flattenTree(nodes: PermissionNode[], level = 0): FlatPermission[] {
  const result: FlatPermission[] = []
  for (const node of nodes) {
    result.push({ ...node, _level: level })
    if (node.children && node.children.length > 0) {
      result.push(...flattenTree(node.children, level + 1))
    }
  }
  return result
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getPermissionTree()
    const data = res.data ?? res
    treeData.value = data ?? []
    flatList.value = flattenTree(treeData.value)
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

function expandAll() {
  const treeEl = document.querySelector('.tree-card:last-child .el-tree') as HTMLElement
  if (treeEl) {
    const nodes = treeEl.querySelectorAll('.el-tree-node')
    nodes.forEach((n) => n.classList.remove('is-collapsed'))
    n.classList.add('is-expanded')
  }
}

function collapseAll() {
  const treeEl = document.querySelector('.tree-card:last-child .el-tree') as HTMLElement
  if (treeEl) {
    const nodes = treeEl.querySelectorAll('.el-tree-node.is-expanded')
    nodes.forEach((n) => n.classList.remove('is-expanded'))
    n.classList.add('is-collapsed')
  }
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
.tree-node {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.tree-node-name {
  font-weight: 500;
}
.tree-node-code {
  font-family: monospace;
}
.tree-node-badge {
  font-size: 11px;
}
</style>
