<template>
  <div class="outbound-form">
    <el-card class="header-card">
      <template #header>
        <div class="card-header">
          <el-icon><DocumentAdd /></el-icon>
          <span>填写出库单</span>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
        @submit.prevent="handleSubmit"
      >
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="客户" prop="customerId">
              <el-select
                v-model="formData.customerId"
                placeholder="请选择客户"
                filterable
                clearable
                style="width: 100%"
              >
                <el-option
                  v-for="c in customerList"
                  :key="c.id"
                  :label="c.customerName"
                  :value="c.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="部门">
              <el-input v-model="formData.department" placeholder="请输入部门" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="申请人">
              <el-input v-model="formData.applicant" placeholder="请输入申请人" clearable />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="出库时间" prop="orderTime">
              <el-date-picker
                v-model="formData.orderTime"
                type="datetime"
                placeholder="选择出库时间"
                format="YYYY-MM-DD HH:mm:ss"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <el-card class="detail-card">
      <template #header>
        <div class="card-header">
          <el-icon><List /></el-icon>
          <span>出库明细</span>
        </div>
      </template>

      <el-table
        :data="formData.details"
        border
        stripe
        style="width: 100%"
        v-loading="productLoading"
      >
        <el-table-column label="产品" min-width="180">
          <template #default="{ row, $index }">
            <el-select
              v-model="row.productId"
              placeholder="选择产品"
              filterable
              clearable
              style="width: 100%"
              @change="(val: number | null) => onProductChange(val, $index)"
            >
              <el-option
                v-for="p in productList"
                :key="p.id"
                :label="p.productName"
                :value="p.id"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="库存提示" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.productId && row.currentStock !== null" type="info" size="small">
              库存: {{ row.currentStock }}
            </el-tag>
            <span v-else class="text-muted">--</span>
          </template>
        </el-table-column>
        <el-table-column label="出库数量" width="140">
          <template #default="{ row }">
            <el-input-number
              v-model="row.quantity"
              :min="0.01"
              :precision="2"
              :controls="true"
              style="width: 100%"
            />
          </template>
        </el-table-column>
        <el-table-column label="单价" width="130">
          <template #default="{ row }">
            <el-input-number
              v-model="row.unitPrice"
              :min="0"
              :precision="2"
              :controls="true"
              style="width: 100%"
            />
          </template>
        </el-table-column>
        <el-table-column label="目标库位" min-width="160">
          <template #default="{ row }">
            <el-select
              v-model="row.locationId"
              placeholder="选择库位"
              filterable
              clearable
              style="width: 100%"
            >
              <el-option
                v-for="loc in locationList"
                :key="loc.locationId"
                :label="loc.locationName"
                :value="loc.locationId"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ $index }">
            <el-button
              type="danger"
              :icon="Delete"
              circle
              size="small"
              :disabled="formData.details.length <= 1"
              @click="removeDetail($index)"
            />
          </template>
        </el-table-column>
      </el-table>

      <div class="add-row">
        <el-button type="primary" :icon="Plus" @click="addDetail">
          添加明细行
        </el-button>
      </div>
    </el-card>

    <el-card class="remark-card">
      <template #header>
        <div class="card-header">
          <el-icon><EditPen /></el-icon>
          <span>备注</span>
        </div>
      </template>
      <el-input
        v-model="formData.remark"
        type="textarea"
        :rows="3"
        placeholder="请输入备注信息（可选）"
        maxlength="500"
        show-word-limit
      />
    </el-card>

    <div class="submit-bar">
      <el-button
        type="primary"
        size="large"
        :loading="submitting"
        :icon="Check"
        @click="handleSubmit"
      >
        提交出库单
      </el-button>
      <el-button size="large" @click="handleReset">重置</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { DocumentAdd, List, EditPen, Plus, Check, Delete } from '@element-plus/icons-vue'
import { createOutbound } from '@/api/outbound'
import { getProductList } from '@/api/product'
import { getCustomerList } from '@/api/customer'
import { getLocationList } from '@/api/location'

interface DetailRow {
  productId: number | null
  currentStock: number | null
  quantity: number
  unitPrice: number
  // 修复:补 locationId,与入库页对齐,confirm 阶段按指定库位扣库存
  locationId: number | null
}

interface OutboundForm {
  customerId: number | null
  department: string
  applicant: string
  orderTime: string
  remark: string
  details: DetailRow[]
}

function createEmptyDetail(): DetailRow {
  return {
    productId: null,
    currentStock: null,
    quantity: 0,
    unitPrice: 0,
    locationId: null,
  }
}

const formRef = ref<FormInstance>()
const submitting = ref(false)
const productLoading = ref(false)

const customerList = ref<any[]>([])
const productList = ref<any[]>([])
const locationList = ref<any[]>([])

const formData = reactive<OutboundForm>({
  customerId: null,
  department: '',
  applicant: '',
  orderTime: '',
  remark: '',
  details: [createEmptyDetail()],
})

const formRules: FormRules = {
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  orderTime: [{ required: true, message: '请选择出库时间', trigger: 'change' }],
}

const getDefaultTime = (): string => {
  const now = new Date()
  const pad = (n: number) => n.toString().padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
}

onMounted(async () => {
  formData.orderTime = getDefaultTime()
  await Promise.all([loadCustomers(), loadProducts(), loadLocations()])
})

async function loadCustomers() {
  try {
    const res = await getCustomerList({ size: 999 })
    customerList.value = res.data?.records ?? res.data ?? []
  } catch {
    // handled by interceptor
  }
}

async function loadProducts() {
  productLoading.value = true
  try {
    const res = await getProductList({ size: 999 })
    productList.value = res.data?.records ?? res.data ?? []
  } finally {
    productLoading.value = false
  }
}

async function loadLocations() {
  try {
    const res = await getLocationList({ size: 999 })
    locationList.value = res.data?.records ?? res.data ?? []
  } catch {
    // handled by interceptor
  }
}

function onProductChange(val: number | null, index: number) {
  const detail = formData.details[index]
  if (val) {
    const product = productList.value.find((p) => p.id === val)
    detail.currentStock = product?.stock ?? product?.currentStock ?? null
  } else {
    detail.currentStock = null
  }
}

function addDetail() {
  formData.details.push(createEmptyDetail())
}

function removeDetail(index: number) {
  if (formData.details.length <= 1) {
    ElMessage.warning('至少保留一条明细')
    return
  }
  formData.details.splice(index, 1)
}

function validateDetails(): boolean {
  if (formData.details.length < 1) {
    ElMessage.warning('请至少添加一条出库明细')
    return false
  }
  for (let i = 0; i < formData.details.length; i++) {
    const d = formData.details[i]
    if (!d.productId) {
      ElMessage.warning(`第 ${i + 1} 行：请选择产品`)
      return false
    }
    if (!d.quantity || d.quantity <= 0) {
      ElMessage.warning(`第 ${i + 1} 行：出库数量必须大于0`)
      return false
    }
    // 修复:库位必填,与入库页一致;避免 confirm 阶段用 default 兜底导致扣错库位
    if (!d.locationId) {
      ElMessage.warning(`第 ${i + 1} 行：请选择目标库位`)
      return false
    }
  }
  return true
}

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (!validateDetails()) return

  submitting.value = true
  try {
    const payload = {
      customerId: formData.customerId,
      department: formData.department,
      applicant: formData.applicant,
      orderTime: formData.orderTime,
      remark: formData.remark,
      details: formData.details.map((d) => ({
        productId: d.productId,
        quantity: d.quantity,
        unitPrice: d.unitPrice,
        locationId: d.locationId,
      })),
    }
    await createOutbound(payload)
    ElMessage.success('出库单创建成功')
    handleReset()
  } catch (e: any) {
    // 修复:把后端 message 透出给用户(以前 catch {} 吞掉所有错误)
    const msg = e?.response?.data?.message ?? e?.message ?? '提交失败'
    ElMessage.error(msg)
  } finally {
    submitting.value = false
  }
}

function handleReset() {
  formData.customerId = null
  formData.department = ''
  formData.applicant = ''
  formData.orderTime = getDefaultTime()
  formData.remark = ''
  formData.details = [createEmptyDetail()]
  formRef.value?.clearValidate()
}
</script>

<style scoped>
.outbound-form {
  padding: 16px;
  max-width: 1400px;
  margin: 0 auto;
}

.header-card,
.detail-card,
.remark-card {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
}

.add-row {
  margin-top: 12px;
  text-align: center;
}

.submit-bar {
  text-align: center;
  padding: 16px 0 32px;
}

.text-muted {
  color: #909399;
  font-size: 13px;
}
</style>
