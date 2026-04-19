import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', hidden: true }
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '首页', icon: 'HomeFilled' }
      }
    ]
  },
  {
    path: '/system',
    name: 'System',
    component: () => import('@/layout/index.vue'),
    meta: { title: '系统管理', icon: 'Setting', permissions: ['tenant:list', 'user:list', 'role:list'] },
    children: [
      {
        path: 'tenant',
        name: 'Tenant',
        component: () => import('@/views/system/tenant/index.vue'),
        meta: { title: '租户管理', permission: 'tenant:list' }
      },
      {
        path: 'user',
        name: 'User',
        component: () => import('@/views/system/user/index.vue'),
        meta: { title: '用户管理', permission: 'user:list' }
      },
      {
        path: 'role',
        name: 'Role',
        component: () => import('@/views/system/role/index.vue'),
        meta: { title: '角色管理', permission: 'role:list' }
      }
    ]
  },
  {
    path: '/archive',
    name: 'Archive',
    component: () => import('@/layout/index.vue'),
    meta: { title: '档案管理', icon: 'Folder', permissions: ['gateway:list', 'meter:list', 'group:list'] },
    children: [
      {
        path: 'gateway',
        name: 'Gateway',
        component: () => import('@/views/archive/gateway/index.vue'),
        meta: { title: '网关管理', permission: 'gateway:list' }
      },
      {
        path: 'meter',
        name: 'Meter',
        component: () => import('@/views/archive/meter/index.vue'),
        meta: { title: '表计管理', permission: 'meter:list' }
      },
      {
        path: 'group',
        name: 'Group',
        component: () => import('@/views/archive/group/index.vue'),
        meta: { title: '群组管理', permission: 'group:list' }
      }
    ]
  },
  {
    path: '/data',
    name: 'Data',
    component: () => import('@/layout/index.vue'),
    meta: { title: '抄表数据', icon: 'DataLine', permissions: ['reading:current', 'reading:history'] },
    children: [
      {
        path: 'current',
        name: 'CurrentData',
        component: () => import('@/views/data/current/index.vue'),
        meta: { title: '当前数据', permission: 'reading:current' }
      },
      {
        path: 'history',
        name: 'HistoryData',
        component: () => import('@/views/data/history/index.vue'),
        meta: { title: '历史数据', permission: 'reading:history' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  
  document.title = to.meta.title ? `${to.meta.title} - 智慧物联网平台` : '智慧物联网平台'
  
  if (to.path === '/login') {
    next()
    return
  }
  
  if (!userStore.isLoggedIn) {
    next('/login')
    return
  }
  
  if (!userStore.userInfo) {
    try {
      await userStore.getUserInfoAction()
    } catch (error) {
      userStore.logout()
      return
    }
  }
  
  // Check route permission
  if (to.meta.permission && !userStore.hasPermission(to.meta.permission)) {
    if (to.meta.permissions && !userStore.hasAnyPermission(to.meta.permissions)) {
      next('/dashboard')
      return
    }
    if (!to.meta.permissions) {
      next('/dashboard')
      return
    }
  }
  
  next()
})

export default router
