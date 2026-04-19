import { useUserStore } from '@/stores/user'

function isAdmin(userStore) {
  const roles = userStore.roles || []
  return roles.includes('admin')
}

export function setupPermission(app) {
  app.directive('permission', {
    mounted(el, binding) {
      const { value } = binding
      const userStore = useUserStore()
      
      if (!value) {
        return
      }
      
      if (isAdmin(userStore)) {
        return
      }
      
      const permissions = userStore.permissions || []
      const hasPermission = permissions.includes(value)
      
      if (!hasPermission) {
        el.parentNode && el.parentNode.removeChild(el)
      }
    }
  })
  
  app.directive('permission-any', {
    mounted(el, binding) {
      const { value } = binding
      const userStore = useUserStore()
      
      if (!value || !Array.isArray(value)) {
        return
      }
      
      if (isAdmin(userStore)) {
        return
      }
      
      const permissions = userStore.permissions || []
      const hasPermission = value.some(perm => permissions.includes(perm))
      
      if (!hasPermission) {
        el.parentNode && el.parentNode.removeChild(el)
      }
    }
  })
  
  app.directive('permission-all', {
    mounted(el, binding) {
      const { value } = binding
      const userStore = useUserStore()
      
      if (!value || !Array.isArray(value)) {
        return
      }
      
      if (isAdmin(userStore)) {
        return
      }
      
      const permissions = userStore.permissions || []
      const hasPermission = value.every(perm => permissions.includes(perm))
      
      if (!hasPermission) {
        el.parentNode && el.parentNode.removeChild(el)
      }
    }
  })
}
