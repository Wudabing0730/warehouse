<template>
  <div class="stock-alert-container">
    <el-alert
      v-for="(msg, index) in messages"
      :key="index"
      :title="msg"
      type="warning"
      show-icon
      :closable="true"
      style="margin-bottom: 8px"
    />
  </div>
</template>

<script setup lang="ts">
defineProps<{
  messages: string[]
}>()
</script>

<style scoped>
.stock-alert-container {
  width: 100%;
}
</style>
