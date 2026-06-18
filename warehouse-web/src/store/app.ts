import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  // State
  const sidebarCollapsed = ref<boolean>(false)
  const alertCount = ref<number>(0)

  // Actions
  function toggleSidebar(): void {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function setAlertCount(n: number): void {
    alertCount.value = n
  }

  return {
    // State
    sidebarCollapsed,
    alertCount,
    // Actions
    toggleSidebar,
    setAlertCount,
  }
})
