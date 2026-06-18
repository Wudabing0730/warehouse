<template>
  <div class="base-page">
    <el-row :gutter="16">
      <!-- Left: Category Tree -->
      <el-col :span="8">
        <el-card class="tree-card">
          <template #header>
            <div class="card-header">
              <span>类别树</span>
              <el-button type="primary" size="small" @click="handleAdd(null)">新增根类别</el-button>
            </div>
          </template>
          <el-tree
            :data="treeData"
            :props="{ label: 'categoryName', children: 'children' }"
            node-key="id"
            default-expand-all
            highlight-current
            :expand-on-click-node="false"
            @node-click="handleNodeClick"
          >
            <template #default="{ node, data }">
              <span class="tree-node">
                <span>{{ data.categoryName }}</span>
                <span class="tree-node-actions">
                  <el-button type="primary" link size="small" @click.stop="handleAdd(data)">新增子级</el-button>
                </span>
              </span>
            </template>
          </el-tree>
        </el-card>
      </el-col>

      <!-- Right: Detail / Form -->
      <el-col :span="16">
        <el-card class="detail-card">
          <template #header>
            <span>{{ selectedNode ? `编辑类别：${selectedNode.categoryName}` : '请从左侧选择一个类别' }}</span>
          </template>

          <div v-if="!selectedNode" class="placeholder">
            <el-empty description="请在左侧点击一个类别节点查看详情" />
          </div>

          <div v-else>
            <el-descriptions :column="1" border style="margin-bottom: 16px">
              <el-descriptions-item label="类别ID">{{ selectedNode.id }}</el-descriptions-item>
              <el-descriptions-item label="类别名称">{{ selectedNode.categoryName }}</el-descriptions-item>
              <el-descriptions-item label="父级ID">{{ selectedNode.parentId ?? '无（根类别）' }}</el-descriptions-item>
              <el-descriptions-item label="排序">{{ selectedNode.sortOrder ?? 0 }}</el-descriptions-item>
              <el-descriptions-item label="子级数量">{{ selectedNode.children?.length ?? 0 }}</el-descriptions-item>
            </el-descriptions>

            <div class="detail-actions">
              <el-button type="primary" @click="handleEdit(selectedNode)">编辑</el-button>
              <el-button type="danger" @click="handleDelete(selectedNode)">删除</el-button>
              <el-button @click="handleAdd(selectedNode)">新增子级</el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Add / Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
      :close-on-click-modal="false"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="父级类别">
          <el-tree-select
            v-model="form.parentId"
            :data="treeData"
            :props="{ label: 'categoryName', value: 'id', children: 'children' }"
            placeholder="不选则为根类别"
            clearable
            check-strictly
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="类别名称" prop="categoryName">
          <el-input v-model="form.categoryName" placeholder="输入类别名称" />
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number v-model="form.sortOrder" :min="0" controls-position="right" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  getCategoryTree,
  createCategory,
  updateCategory,
  deleteCategory,
  type CategoryTreeNode,
} from '@/api/category'

// ---- Tree ----
const treeData = ref<CategoryTreeNode[]>([])
const selectedNode = ref<CategoryTreeNode | null>(null)

const fetchTree = async () => {
  try {
    const res = await getCategoryTree()
    treeData.value = res.data ?? []
  } catch {
    // handled by interceptor
  }
}

const handleNodeClick = (data: CategoryTreeNode) => {
  selectedNode.value = data
}

// ---- Dialog ----
const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  id: null as number | null,
  parentId: null as number | null,
  categoryName: '',
  sortOrder: 0,
})

const rules: FormRules = {
  categoryName: [{ required: true, message: '请输入类别名称', trigger: 'blur' }],
}

const resetForm = () => {
  formRef.value?.resetFields()
  form.id = null
  form.parentId = null
  form.categoryName = ''
  form.sortOrder = 0
}

const handleAdd = (parent: CategoryTreeNode | null) => {
  resetForm()
  dialogTitle.value = parent ? `新增「${parent.categoryName}」的子类别` : '新增根类别'
  form.parentId = parent?.id ?? null
  dialogVisible.value = true
}

const handleEdit = (data: CategoryTreeNode) => {
  resetForm()
  dialogTitle.value = `编辑类别：${data.categoryName}`
  form.id = data.id
  form.parentId = data.parentId ?? null
  form.categoryName = data.categoryName
  form.sortOrder = data.sortOrder ?? 0
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const payload = { ...form }
    if (payload.id) {
      await updateCategory(payload)
      ElMessage.success('更新成功')
    } else {
      await createCategory(payload)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    selectedNode.value = null
    fetchTree()
  } catch {
    // handled by interceptor
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = (data: CategoryTreeNode) => {
  if (data.children && data.children.length > 0) {
    ElMessage.warning('该类别下存在子类别，无法删除')
    return
  }
  ElMessageBox.confirm(`确定要删除类别「${data.categoryName}」吗？`, '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    await deleteCategory(data.id)
    ElMessage.success('删除成功')
    selectedNode.value = null
    fetchTree()
  }).catch(() => { /* cancelled */ })
}

// ---- Init ----
onMounted(() => {
  fetchTree()
})
</script>

<style scoped>
.base-page {
  padding: 16px;
}

.tree-card {
  height: calc(100vh - 140px);
  overflow-y: auto;
}

.detail-card {
  height: calc(100vh - 140px);
  overflow-y: auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tree-node {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-right: 8px;
}

.tree-node-actions {
  opacity: 0;
  transition: opacity 0.2s;
}

.tree-node:hover .tree-node-actions {
  opacity: 1;
}

.placeholder {
  padding: 60px 0;
}

.detail-actions {
  display: flex;
  gap: 8px;
}
</style>
