import { useUserStore } from '@/store/user'

export function hasPermission(permission: string): boolean {
  const userStore = useUserStore()
  return userStore.permissions.includes(permission)
}

export function hasAnyPermission(permissions: string[]): boolean {
  return permissions.some(p => hasPermission(p))
}

export function hasAllPermissions(permissions: string[]): boolean {
  return permissions.every(p => hasPermission(p))
}
