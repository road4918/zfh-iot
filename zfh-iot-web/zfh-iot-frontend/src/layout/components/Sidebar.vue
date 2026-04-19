<template>
  <div class="sidebar" :class="{ collapsed: appStore.sidebarCollapsed }">
    <div class="logo-section">
      <div class="logo">
        <div class="logo-badge">
          <el-icon :size="20"><DataLine /></el-icon>
        </div>
        <div v-if="!appStore.sidebarCollapsed" class="logo-text">
          <span class="title">智慧物联网平台</span>
          <span class="subtitle">IOT Console</span>
        </div>
      </div>
      <div v-if="!appStore.sidebarCollapsed" class="tenant-info">
        <el-select
          v-if="userStore.isPlatformUser && tenantOptions.length > 0"
          v-model="userStore.currentTenantId"
          placeholder="搜索租户"
          filterable
          remote
          :remote-method="handleSearch"
          :loading="loading"
          @change="handleTenantChange"
          class="tenant-select"
        >
          <el-option
            label="全部"
            :value="-1"
          />
          <el-option
            v-for="tenant in tenantOptions"
            :key="tenant.id"
            :label="tenant.tenantName"
            :value="tenant.id"
          >
            <span :class="{ 'active-tenant': tenant.id === userStore.currentTenantId }">
              {{ tenant.tenantName }}
            </span>
          </el-option>
        </el-select>
        <span v-else class="tenant-name">
          {{ currentTenantName || '-' }}
        </span>
      </div>
    </div>
    <el-menu
      :default-active="activeMenu"
      :collapse="appStore.sidebarCollapsed"
      :collapse-transition="false"
      background-color="transparent"
      text-color="#576b95"
      active-text-color="#07c160"
      router
    >
      <el-menu-item index="/dashboard">
        <el-icon><HomeFilled /></el-icon>
        <template #title>首页</template>
      </el-menu-item>
      
      <el-sub-menu v-if="showSystemMenu" index="/system">
        <template #title>
          <el-icon><Setting /></el-icon>
          <span>系统管理</span>
        </template>
        <el-menu-item v-if="userStore.hasPermission('tenant:list')" index="/system/tenant">租户管理</el-menu-item>
        <el-menu-item v-if="userStore.hasPermission('user:list')" index="/system/user">用户管理</el-menu-item>
        <el-menu-item v-if="userStore.hasPermission('role:list')" index="/system/role">角色管理</el-menu-item>
      </el-sub-menu>
      
      <el-sub-menu v-if="showArchiveMenu" index="/archive">
        <template #title>
          <el-icon><Folder /></el-icon>
          <span>档案管理</span>
        </template>
        <el-menu-item v-if="userStore.hasPermission('gateway:list')" index="/archive/gateway">网关管理</el-menu-item>
        <el-menu-item v-if="userStore.hasPermission('meter:list')" index="/archive/meter">表计管理</el-menu-item>
        <el-menu-item v-if="userStore.hasPermission('group:list')" index="/archive/group">群组管理</el-menu-item>
      </el-sub-menu>
      
      <el-sub-menu v-if="showDataMenu" index="/data">
        <template #title>
          <el-icon><DataLine /></el-icon>
          <span>抄表数据</span>
        </template>
        <el-menu-item v-if="userStore.hasPermission('reading:current')" index="/data/current">当前数据</el-menu-item>
        <el-menu-item v-if="userStore.hasPermission('reading:history')" index="/data/history">历史数据</el-menu-item>
      </el-sub-menu>
    </el-menu>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import { getTenantList } from '@/api/tenant'
const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)

const showSystemMenu = computed(() => {
  return userStore.hasAnyPermission(['tenant:list', 'user:list', 'role:list'])
})

const showArchiveMenu = computed(() => {
  return userStore.hasAnyPermission(['gateway:list', 'meter:list', 'group:list'])
})

const showDataMenu = computed(() => {
  return userStore.hasAnyPermission(['reading:current', 'reading:history'])
})

const tenantOptions = ref([])
const loading = ref(false)
const searchKeyword = ref('')

const currentTenantName = computed(() => {
  if (userStore.isPlatformUser) {
    const tenant = tenantOptions.value.find(t => t.id === userStore.currentTenantId)
    return tenant?.tenantName
  } else {
    return userStore.userInfo?.tenantName
  }
})

const fetchTenants = async (keyword = '') => {
  if (!userStore.isPlatformUser) return
  loading.value = true
  try {
    const res = await getTenantList({ page: 1, size: 1000, status: 1, keyword })
    tenantOptions.value = res.list || []
  } catch (error) {
    console.error('Failed to fetch tenants:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = (keyword) => {
  searchKeyword.value = keyword
  fetchTenants(keyword)
}

const handleTenantChange = (tenantId) => {
  userStore.setCurrentTenantId(tenantId)
  // 刷新当前页面数据 - 触发一个全局事件通知所有组件刷新
  window.location.reload()
}

onMounted(() => {
  fetchTenants()
})

watch(() => userStore.userInfo, () => {
  if (!userStore.isPlatformUser) {
    userStore.setCurrentTenantId(userStore.userInfo?.tenantId)
  }
}, { immediate: true })
</script>

<style scoped lang="scss">
.sidebar {
  width: 224px;
  height: 100%;
  padding: 14px 12px 12px;
  background: linear-gradient(180deg, #f8fcfa 0%, #f3f7f5 100%);
  border-right: 1px solid #e8efe9;
  transition: width 0.3s;
  
  &.collapsed {
    width: 76px;
    
    .logo {
      justify-content: center;
    }
  }
  
  .logo-section {
    margin-bottom: 14px;
    padding: 10px 10px 14px;
    border-radius: 18px;
    background: linear-gradient(180deg, #ffffff 0%, #f8fbf9 100%);
    border: 1px solid #e8efe9;
    box-shadow: 0 8px 20px rgba(7, 193, 96, 0.06);
  }
  
  .logo {
    display: flex;
    align-items: center;
    gap: 12px;
    min-height: 52px;
    
    .logo-badge {
      width: 42px;
      height: 42px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 14px;
      color: #fff;
      background: linear-gradient(135deg, #07c160 0%, #18d06a 100%);
      box-shadow: 0 10px 20px rgba(7, 193, 96, 0.22);
      flex-shrink: 0;
    }
    
    .logo-text {
      display: flex;
      flex-direction: column;
      min-width: 0;
      
      .title {
        color: #191919;
        font-size: 16px;
        font-weight: 600;
        line-height: 1.25;
      }
      
      .subtitle {
        margin-top: 2px;
        color: #8a94a6;
        font-size: 12px;
        line-height: 1.2;
        letter-spacing: 0.04em;
      }
    }
  }
  
  .tenant-info {
    margin-top: 12px;
    
    .tenant-select {
      width: 100%;
      
      :deep(.el-input__wrapper) {
        border-radius: 12px;
        border: 1px solid #e8efe9;
        background: #f7faf8;
        box-shadow: none !important;
        
        &.is-focus {
          border-color: #07c160;
        }
      }
      
      :deep(.el-input__inner) {
        color: #576b95;
        font-size: 12px;
        text-align: center;
        
        &::placeholder {
          color: #8a94a6;
        }
      }
    }
    
    .active-tenant {
      color: #07c160;
      font-weight: 500;
    }
    
    .tenant-name {
      width: 100%;
      min-height: 36px;
      padding: 0 12px;
      border-radius: 12px;
      border: 1px solid #e8efe9;
      background: #f7faf8;
      color: #576b95;
      font-size: 12px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 4px;
      transition: all 0.2s ease;
      
      &.clickable {
        cursor: pointer;
        
        &:hover {
          color: #07c160;
          border-color: #b7e7ca;
          background: #f1fbf5;
        }
      }
    }
  }
  
  :deep(.el-menu) {
    border-right: none;
    background: transparent;
  }
  
  :deep(.el-menu-item),
  :deep(.el-sub-menu__title) {
    height: 46px;
    margin-bottom: 6px;
    border-radius: 14px;
    color: #576b95;
    font-weight: 500;
    transition: all 0.2s ease;
  }
  
  :deep(.el-menu-item .el-icon),
  :deep(.el-sub-menu__title .el-icon) {
    color: #8a94a6;
    font-size: 18px;
  }
  
  :deep(.el-sub-menu .el-menu-item) {
    min-width: auto;
    margin: 4px 0 4px 10px;
    padding-left: 38px !important;
    height: 40px;
    border-radius: 12px;
    font-size: 13px;
  }
  
  :deep(.el-menu-item:hover),
  :deep(.el-sub-menu__title:hover) {
    color: #07c160;
    background: #effaf4;
  }
  
  :deep(.el-menu-item:hover .el-icon),
  :deep(.el-sub-menu__title:hover .el-icon) {
    color: #07c160;
  }
  
  :deep(.el-menu-item.is-active) {
    color: #07c160;
    background: linear-gradient(90deg, rgba(7, 193, 96, 0.12) 0%, rgba(7, 193, 96, 0.04) 100%);
    box-shadow: inset 3px 0 0 #07c160;
  }
  
  :deep(.el-menu-item.is-active .el-icon) {
    color: #07c160;
  }
  
  :deep(.el-sub-menu.is-active > .el-sub-menu__title) {
    color: #191919;
  }
  
  :deep(.el-menu--inline) {
    background: transparent;
  }
  
  :deep(.el-menu--collapse .el-sub-menu__title),
  :deep(.el-menu--collapse .el-menu-item) {
    justify-content: center;
    padding: 0 !important;
  }
}

:deep(.el-dropdown-menu__item.active) {
  color: #07c160;
  background-color: #effaf4;
}
</style>
