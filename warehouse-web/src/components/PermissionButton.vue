<template>
  <el-button v-if="hasPermission(permission)" v-bind="$attrs">
    <slot />
  </el-button>
</template>

<script setup lang="ts">
import { hasPermission as checkPermission } from '@/utils/permission'

const props = defineProps<{
  permission: string
}>()

function hasPermission(perm: string): boolean {
  return checkPermission(perm)
}
</script>
