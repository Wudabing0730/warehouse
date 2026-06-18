<template>
  <div class="pagination-wrapper">
    <el-pagination
      v-model:current-page="current"
      v-model:page-size="size"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      background
      @size-change="handleSizeChange"
      @current-change="handlePageChange"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

const props = withDefaults(
  defineProps<{
    total: number
    defaultPage?: number
    defaultSize?: number
  }>(),
  {
    defaultPage: 1,
    defaultSize: 10,
  }
)

const emit = defineEmits<{
  (e: 'change', page: number, size: number): void
}>()

const current = ref(props.defaultPage)
const size = ref(props.defaultSize)

watch(
  () => props.defaultPage,
  (val) => {
    current.value = val
  }
)

watch(
  () => props.defaultSize,
  (val) => {
    size.value = val
  }
)

function handleSizeChange(val: number) {
  size.value = val
  emit('change', current.value, val)
}

function handlePageChange(val: number) {
  current.value = val
  emit('change', val, size.value)
}
</script>

<style scoped>
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: 16px 0;
}
</style>
