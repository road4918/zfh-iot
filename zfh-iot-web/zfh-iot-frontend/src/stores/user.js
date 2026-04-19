import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import Cookies from 'js-cookie'
import { login, getUserInfo } from '@/api/auth'
import router from '@/router'

export const useUserStore = defineStore('user', () => {
  const token = ref(Cookies.get('token') || '')
  const userInfo = ref(null)
  const permissions = ref([])
  const roles = ref([])
  const currentTenantId = ref(getInitialTenantId())

  function getInitialTenantId() {
    const stored = localStorage.getItem('currentTenantId')
    if (!stored || stored === '' || stored === '-1') return -1
    return Number(stored)
  }

  const isLoggedIn = computed(() => !!token.value)

  const isPlatformUser = computed(() => {
    return Number(userInfo.value?.userType) === 0
  })

  const effectiveTenantId = computed(() => {
    return currentTenantId.value === -1 ? null : currentTenantId.value
  })

  const hasPermission = (perm) => {
    return permissions.value.includes(perm)
  }

  const hasAnyPermission = (perms) => {
    return perms.some(perm => permissions.value.includes(perm))
  }

  const setCurrentTenantId = (tenantId) => {
    currentTenantId.value = tenantId
    localStorage.setItem('currentTenantId', tenantId != null ? tenantId : -1)
  }

  const setToken = (newToken) => {
    token.value = newToken
    Cookies.set('token', newToken, { expires: 1/12 })
  }

  const clearToken = () => {
    token.value = ''
    userInfo.value = null
    permissions.value = []
    roles.value = []
    currentTenantId.value = -1
    localStorage.removeItem('currentTenantId')
    Cookies.remove('token')
  }

  const loginAction = async (loginData) => {
    const data = await login(loginData)
    setToken(data.token)
    return data
  }

  const getUserInfoAction = async () => {
    const data = await getUserInfo()
    userInfo.value = data
    permissions.value = data.permissions || []
    roles.value = data.roles || []
    if (!isPlatformUser.value && data.tenantId) {
      currentTenantId.value = data.tenantId
      localStorage.setItem('currentTenantId', data.tenantId)
    }
    if (isPlatformUser.value && currentTenantId.value == null) {
      currentTenantId.value = -1
      localStorage.setItem('currentTenantId', -1)
    }
    return data
  }

  const logout = () => {
    clearToken()
    router.push('/login')
  }

  return {
    token,
    userInfo,
    permissions,
    roles,
    currentTenantId,
    isLoggedIn,
    isPlatformUser,
    hasPermission,
    hasAnyPermission,
    effectiveTenantId,
    setCurrentTenantId,
    setToken,
    clearToken,
    loginAction,
    getUserInfoAction,
    logout
  }
})