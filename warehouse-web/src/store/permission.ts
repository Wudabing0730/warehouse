import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { RouteRecordRaw } from 'vue-router'

export const usePermissionStore = defineStore('permission', () => {
  // State
  const routes = ref<RouteRecordRaw[]>([])

  // Actions
  function generateRoutes(userPermissions: string[]): RouteRecordRaw[] {
    // Placeholder for future dynamic permission-based routing.
    // This would filter available routes based on the user's permissions
    // and add them dynamically via router.addRoute().
    //
    // Example future logic:
    //   const accessibleRoutes = filterAsyncRoutes(constantRoutes, userPermissions)
    //   routes.value = accessibleRoutes
    //   accessibleRoutes.forEach(r => router.addRoute('Layout', r))
    //
    // For now, return an empty array.
    console.log('generateRoutes called with permissions:', userPermissions)
    routes.value = []
    return routes.value
  }

  return {
    // State
    routes,
    // Actions
    generateRoutes,
  }
})
