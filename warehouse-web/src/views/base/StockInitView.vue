<template>
  <div class="base-page">
    <!-- Search / Filter -->
    <el-card class="search-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="产品">
          <el-input v-model="searchForm.productKeyword" placeholder="产品编码/名称" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="库位">
          <el-select v-model="searchForm.locationId" placeholder="选择库位" clearable style="width: 200px">
            <el-option
              v-for="loc in locationList"
              :key="loc.id"
              :label="loc.locationName"
              :value="loc.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Table -->
    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>库存管理</span>
          <el-button type="warning" @click="handleInitDialog">库存初始化</el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" border stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="productCode" label="产品编码" width="130" />
        <el-table-column prop="productName" label="产品名称" width="150" />
        <el-table-column prop="productUnit" label="产品单位" width="90" />
        <el-table-column prop="locationCode" label="库位编码" width="120" />
        <el-table-column prop="locationName" label="库位名称" width="130" />
        <el-table-column prop="quantity" label="当前库存" width="110">
          <template #default="{ row }">
            <span :style="{ color: getStockColor(row), fontWeight: 'bold' }">
              {{ row.quantity }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="upperLimit" label="上限" width="90" />
        <el-table-column prop="lowerLimit" label="下限" width="90" />
        <el-table-column prop="updateTime" label="更新时间" width="180" />
      </el-table>

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :page-sizes="[10, 20, 50, 100]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        class="pagination"
        @size-change="handleSearch"
        @current-change="handleSearch"
      />
    </el-card>

    <!-- Init Dialog -->
    <el-dialog
      v-model="initDialogVisible"
      title="库存初始化"
      width="500px"
      :close-on-click-modal="false"
      @closed="resetInitForm"
    >
      <el-form ref="initFormRef" :model="initForm" :rules="initRules" label-width="100px">
        <el-form-item label="产品" prop="productId">
          <el-select v-model="initForm.productId" placeholder="选择产品" filterable style="width: 100%">
            <el-option
              v-for="p in productList"
              :key="p.id"
              :label="`${p.productCode} - ${p.productName}`"
              :value="p.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="库位" prop="locationId">
          <el-select v-model="initForm.locationId" placeholder="选择库位" style="width: 100%">
            <el-option
              v-for="loc in locationList"
              :key="loc.id"
              :label="`${loc.locationCode} - ${loc.locationName}`"
              :value="loc.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="库存数量" prop="quantity">
          <el-input-number v-model="initForm.quantity" :min="0" :precision="2" controls-position="right" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="initDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="initLoading" @click="handleInitSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { getStockList, initStock } from '@/api/stock'
import { getLocationList, type LocationItem } from '@/api/location'
import { getProductList } from '@/api/product'

// ---- Search ----
const searchForm = reactive({
  productKeyword: '',
  locationId: null as number | null,
})

// ---- Table ----
const loading = ref(false)
const tableData = ref<any[]>([])
const pagination = reactive({ page: 1, size: 10, total: 0 })

const fetchList = async () => {
  loading.value = true
  try {
    const params: Record<string, any> = {
      page: pagination.page,
      size: pagination.size,
    }
    if (searchForm.productKeyword) params.productKeyword = searchForm.productKeyword
    if (searchForm.locationId != null) params.locationId = searchForm.locationId

    const res = await getStockList(params)
    tableData.value = res.data.records ?? res.data.list ?? []
    pagination.total = res.data.total ?? 0
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  fetchList()
}

const handleReset = () => {
  searchForm.productKeyword = ''
  searchForm.locationId = null
  handleSearch()
}

// ---- Color coding for stock levels ----
const getStockColor = (row: any): string => {
  const qty = Number(row.quantity)
  const lower = Number(row.lowerLimit)
  const upper = Number(row.upperLimit)

  if (lower != null && qty < lower) return '#f56c6c'   // red: below lower limit
  if (upper != null && qty > upper) return '#e6a23c'   // orange: above upper limit
  return '#303133' // default
}

// ---- Location list ----
const locationList = ref<LocationItem[]>([])
const fetchLocationList = async () => {
  try {
    const res = await getLocationList({ page: 1, size: 999 })
    locationList.value = res.data.records ?? res.data.list ?? []
  } catch { /* ignore */ }
}

// ---- Product list (for init dialog) ----
const productList = ref<any[]>([])
const fetchProductList = async () => {
  try {
    const res = await getProductList({ page: 1, size: 999 })
    productList.value = res.data.records ?? res.data.list ?? []
  } catch { /* ignore */ }
}

// ---- Init Dialog ----
const initDialogVisible = ref(false)
const initLoading = ref(false)
const initFormRef = ref<FormInstance>()
const initForm = reactive({
  productId: null as number | null,
  locationId: null as number | null,
  quantity: 0,
})

const initRules: FormRules = {
  productId: [{ required: true, message: '请选择产品', trigger: 'change' }],
  locationId: [{ required: true, message: '请选择库位', trigger: 'change' }],
  quantity: [{ required: true, message: '请输入库存数量', trigger: 'blur' }],
}

const resetInitForm = () => {
  initFormRef.value?.resetFields()
  initForm.productId = null
  initForm.locationId = null
  initForm.quantity = 0
}

const handleInitDialog = () => {
  resetInitForm()
  initDialogVisible.value = true
}

const handleInitSubmit = async () => {
  const valid = await initFormRef.value?.validate().catch(() => false)
  if (!valid) return

  initLoading.value = true
  try {
    await initStock({
      productId: initForm.productId,
      locationId: initForm.locationId,
      quantity: initForm.quantity,
    })
    ElMessage.success('库存初始化成功')
    initDialogVisible.value = false
    fetchList()
  } catch {
    // handled by interceptor
  } finally {
    initLoading.value = false
  }
}

// ---- Init ----
onMounted(() => {
  fetchList()
  fetchLocationList()
  fetchProductList()
})
</script>

<style scoped>
.base-page {
  padding: 16px;
}

.search-card {
  margin-bottom: 16px;
}

.table-card {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
