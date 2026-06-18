<template>
  <div class="check-input-container">
    <!-- Product Selection -->
    <el-card class="select-card" shadow="never">
      <template #header>
        <span>选择盘点产品</span>
      </template>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="产品" prop="productId">
          <el-select
            v-model="form.productId"
            placeholder="请选择产品"
            filterable
            style="width: 100%"
            @change="onProductChange"
          >
            <el-option
              v-for="p in productOptions"
              :key="p.id"
              :label="p.name + ' (' + p.code + ')'"
              :value="p.id"
            />
          </el-select>
        </el-form-item>

        <el-descriptions v-if="selectedProduct" :column="2" border size="small" style="margin-bottom: 16px">
          <el-descriptions-item label="当前库存">{{ stockInfo.quantity ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="库位">{{ stockInfo.locationName ?? '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-form-item label="实盘数量" prop="actualQuantity">
          <el-input-number v-model="form.actualQuantity" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="盘点日期" prop="checkDate">
          <el-date-picker v-model="form.checkDate" type="date" placeholder="选择日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">提交盘点</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Recent Checks -->
    <el-card class="recent-card" shadow="never">
      <template #header>
        <span>最近盘点记录</span>
      </template>
      <el-table :data="recentChecks" stripe border v-loading="recentLoading" style="width: 100%">
        <el-table-column prop="checkNo" label="盘点单号" min-width="160" />
        <el-table-column label="产品" min-width="140">
          <template #default="{ row }">
            {{ row.productName }} ({{ row.productCode }})
          </template>
        </el-table-column>
        <el-table-column prop="bookQuantity" label="账面数量" width="100" />
        <el-table-column prop="actualQuantity" label="实盘数量" width="100" />
        <el-table-column label="盈亏数量" width="100">
          <template #default="{ row }">
            <span :style="{ color: row.diffQuantity > 0 ? '#67c23a' : row.diffQuantity < 0 ? '#f56c6c' : '#909399' }">
              {{ row.diffQuantity ?? 0 }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="checkUser" label="盘点人" width="100" />
        <el-table-column prop="checkDate" label="盘点日期" width="120" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.status === 0" type="warning">待处理</el-tag>
            <el-tag v-else-if="row.status === 1" type="success">已确认</el-tag>
            <el-tag v-else type="info">已取消</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { getCheckList, createCheck } from '@/api/inventory'
import { getProductList } from '@/api/product'
import { getStockList } from '@/api/stock'

// ---------- Product options ----------
interface ProductOption {
  id: number | string
  name: string
  code: string
}
const productOptions = ref<ProductOption[]>([])

async function fetchProducts() {
  try {
    const res = await getProductList({ page: 1, size: 9999 })
    productOptions.value = (res.data?.records ?? res.data ?? []) as ProductOption[]
  } catch {
    // ignore
  }
}

// ---------- Form ----------
const formRef = ref<FormInstance>()
const submitting = ref(false)
const selectedProduct = ref(false)
const stockInfo = reactive({ quantity: null as number | null, locationName: '' })

const form = reactive({
  productId: '' as number | string,
  actualQuantity: 0,
  checkDate: new Date(),
  remark: '',
})

const rules: FormRules = {
  productId: [{ required: true, message: '请选择产品', trigger: 'change' }],
  actualQuantity: [{ required: true, message: '请输入实盘数量', trigger: 'blur' }],
  checkDate: [{ required: true, message: '请选择盘点日期', trigger: 'change' }],
}

async function onProductChange(productId: number | string) {
  if (!productId) {
    selectedProduct.value = false
    stockInfo.quantity = null
    stockInfo.locationName = ''
    return
  }
  try {
    const res = await getStockList({ productId, page: 1, size: 1 })
    const records = (res.data?.records ?? res.data ?? []) as any[]
    if (records.length > 0) {
      stockInfo.quantity = records[0].quantity ?? 0
      stockInfo.locationName = records[0].locationName ?? records[0].location ?? '-'
    } else {
      stockInfo.quantity = 0
      stockInfo.locationName = '-'
    }
    selectedProduct.value = true
  } catch {
    stockInfo.quantity = 0
    stockInfo.locationName = '-'
    selectedProduct.value = true
  }
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await createCheck({
      productId: form.productId,
      actualQuantity: form.actualQuantity,
      checkDate: form.checkDate,
      remark: form.remark,
    })
    ElMessage.success('盘点单提交成功')
    handleReset()
    fetchRecentChecks()
  } catch {
    // error handled by interceptor
  } finally {
    submitting.value = false
  }
}

function handleReset() {
  form.productId = ''
  form.actualQuantity = 0
  form.checkDate = new Date()
  form.remark = ''
  selectedProduct.value = false
  stockInfo.quantity = null
  stockInfo.locationName = ''
}

// ---------- Recent Checks ----------
interface CheckRecord {
  id: number | string
  checkNo: string
  productName: string
  productCode: string
  bookQuantity: number
  actualQuantity: number
  diffQuantity: number
  checkUser: string
  checkDate: string
  status: number
}

const recentChecks = ref<CheckRecord[]>([])
const recentLoading = ref(false)

async function fetchRecentChecks() {
  recentLoading.value = true
  try {
    const res = await getCheckList({ page: 1, size: 10 })
    const data = res.data
    recentChecks.value = ((data.records ?? data ?? []) as CheckRecord[])
  } catch {
    // ignore
  } finally {
    recentLoading.value = false
  }
}

// ---------- Lifecycle ----------
onMounted(() => {
  fetchProducts()
  fetchRecentChecks()
})
</script>

<style scoped>
.check-input-container {
  padding: 16px;
}

.select-card {
  margin-bottom: 16px;
}

.recent-card {
  margin-bottom: 16px;
}
</style>
