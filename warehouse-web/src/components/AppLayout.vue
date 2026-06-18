<template>
  <el-container class="app-layout">
    <el-aside :width="isCollapsed ? '64px' : '220px'" class="app-aside">
      <div class="logo">
        <span v-if="!isCollapsed" class="logo-text">仓库管理系统</span>
        <span v-else class="logo-text">WMS</span>
      </div>
      <MenuSidebar />
    </el-aside>
    <el-container>
      <el-header class="app-header">
        <div class="header-left">
          <el-button :icon="Fold" text @click="toggleSidebar" />
        </div>
        <div class="header-right">
          <el-badge :value="alertCount" :hidden="alertCount === 0">
            <el-button :icon="Bell" text />
          </el-badge>
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-icon class="user-icon"><User /></el-icon>
              {{ userStore.realName || userStore.username }}
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人信息</el-dropdown-item>
                <el-dropdown-item command="password">修改密码</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { Fold, Bell, User } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { useAppStore } from '@/store/app'
import MenuSidebar from './MenuSidebar.vue'

const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()

const isCollapsed = appStore.sidebarCollapsed
const alertCount = appStore.alertCount

function toggleSidebar() {
  appStore.toggleSidebar()
}

function handleCommand(command: string) {
  switch (command) {
    case 'profile':
      ElMessage.info('个人信息功能开发中')
      break
    case 'password':
      ElMessage.info('修改密码功能开发中')
      break
    case 'logout':
      userStore.logout()
      router.push('/login')
      break
  }
}
</script>

<style scoped>
.app-layout {
  height: 100vh;
  width: 100vw;
  overflow: hidden;
}

.app-aside {
  background-color: #304156;
  transition: width 0.3s ease;
  overflow-x: hidden;
  flex-shrink: 0;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  user-select: none;
}

.logo-text {
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  white-space: nowrap;
  letter-spacing: 1px;
}

.app-header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #fff;
  border-bottom: 1px solid #e6e6e6;
  padding: 0 20px;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-info {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #333;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.user-info:hover {
  background-color: #f5f7fa;
}

.user-icon {
  font-size: 16px;
}

.app-main {
  background-color: #f0f2f5;
  overflow-y: auto;
  padding: 20px;
  flex: 1;
}
</style>
