import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(false)
  const device = ref('desktop')

  const toggleSidebar = () => {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  const closeSidebar = () => {
    sidebarCollapsed.value = true
  }

  const toggleDevice = (newDevice) => {
    device.value = newDevice
  }

  return {
    sidebarCollapsed,
    device,
    toggleSidebar,
    closeSidebar,
    toggleDevice
  }
})
