<template>
  <div class="borrow-form">
    <el-card class="header-card">
      <template #header>
        <div class="card-header">
          <el-icon><DocumentAdd /></el-icon>
          <span>借用登记</span>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="120px"
        style="max-width: 700px"
        @submit.prevent="handleSubmit"
      >
        <el-form-item label="产品" prop="productId">
          <el-select
            v-model="formData.productId"
            placeholder="请选择产品"
            filterable
            clearable
            style="width: 100%"
            @change="onProductChange"
            v-loading="productLoading"
          >
            <el-option
              v-for="p in productList"
              :key="p.id"
              :label="p.name"
              :value="p.id"
            />
          </el-select>
          <span v-if="currentStock !== null" class="stock-hint">
            当前库存: {{ currentStock }}
          </span>
        </el-form-item>

        <el-form-item label="借用数量" prop="quantity">
          <el-input-number
            v-model="formData.quantity"
            :min="0.01"
            :precision="2"
            :controls="true"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="借用人" prop="borrower">
          <el-input v-model="formData.borrower" placeholder="请输入借用人" clearable />
        </el-form-item>

        <el-form-item label="借用日期" prop="borrowDate">
          <el-date-picker
            v-model="formData.borrowDate"
            type="datetime"
            placeholder="选择借用日期"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="预计归还日期" prop="expectedReturnDate">
          <el-date-picker
            v-model="formData.expectedReturnDate"
            type="datetime"
            placeholder="选择预计归还日期"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="备注">
          <el-input
            v-model="formData.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注信息（可选）"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="submitting"
            :icon="Check"
            @click="handleSubmit"
          >
            提交借用记录
          </el-button>
          <el-button size="large" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { DocumentAdd, Check } from '@element-plus/icons-vue'
import { createBorrow } from '@/api/borrow'
import { getProductList } from '@/api/product'

interface BorrowForm {
  productId: number | null
  quantity: number
  borrower: string
  borrowDate: string
  expectedReturnDate: string
  remark: string
}

const formRef = ref<FormInstance>()
const submitting = ref(false)
const productLoading = ref(false)
const currentStock = ref<number | null>(null)

const productList = ref<any[]>([])

const formData = reactive<BorrowForm>({
  productId: null,
  quantity: 0,
  borrower: '',
  borrowDate: '',
  expectedReturnDate: '',
  remark: '',
})

const formRules: FormRules = {
  productId: [{ required: true, message: '请选择产品', trigger: 'change' }],
  quantity: [
    { required: true, message: '请输入借用数量', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value === 0 || value === null || value === undefined) {
          callback(new Error('借用数量必须大于0'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
  borrower: [{ required: true, message: '请输入借用人', trigger: 'blur' }],
  borrowDate: [{ required: true, message: '请选择借用日期', trigger: 'change' }],
  expectedReturnDate: [{ required: true, message: '请选择预计归还日期', trigger: 'change' }],
}

const getDefaultTime = (): string => {
  const now = new Date()
  const pad = (n: number) => n.toString().padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
}

onMounted(() => {
  formData.borrowDate = getDefaultTime()
  loadProducts()
})

async function loadProducts() {
  productLoading.value = true
  try {
    const res = await getProductList({ size: 999 })
    productList.value = res.data?.records ?? res.data ?? []
  } finally {
    productLoading.value = false
  }
}

function onProductChange(val: number | null) {
  if (val) {
    const product = productList.value.find((p) => p.id === val)
    currentStock.value = product?.stock ?? product?.currentStock ?? null
  } else {
    currentStock.value = null
  }
}

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const payload = {
      productId: formData.productId,
      quantity: formData.quantity,
      borrower: formData.borrower,
      borrowDate: formData.borrowDate,
      expectedReturnDate: formData.expectedReturnDate,
      remark: formData.remark,
    }
    await createBorrow(payload)
    ElMessage.success('借用记录创建成功')
    handleReset()
  } catch {
    // handled by interceptor
  } finally {
    submitting.value = false
  }
}

function handleReset() {
  formData.productId = null
  formData.quantity = 0
  formData.borrower = ''
  formData.borrowDate = getDefaultTime()
  formData.expectedReturnDate = ''
  formData.remark = ''
  currentStock.value = null
  formRef.value?.clearValidate()
}
</script>

<style scoped>
.borrow-form {
  padding: 16px;
  max-width: 800px;
  margin: 0 auto;
}

.header-card {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
}

.stock-hint {
  margin-left: 8px;
  color: #909399;
  font-size: 13px;
}
</style>
