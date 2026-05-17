import { defineStore } from 'pinia'
import { ref } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useAuthStore } from './auth'

export const useWebSocketStore = defineStore('websocket', () => {
  const client = ref(null)
  const connected = ref(false)
  const deviceStatus = ref([])
  const commLogs = ref([])
  const stats = ref({})

  const connect = () => {
    const authStore = useAuthStore()
    if (!authStore.token) return

    const stompClient = new Client({
      webSocketFactory: () => new SockJS(`/zfh-virtual-device-backend/ws/virtual-device?token=${authStore.token}`),
      reconnectDelay: 5000,
      onConnect: () => {
        connected.value = true
        console.log('WebSocket connected')

        // Subscribe to device status
        stompClient.subscribe('/topic/device-status', (message) => {
          deviceStatus.value.push(JSON.parse(message.body))
        })

        // Subscribe to communication logs
        stompClient.subscribe('/topic/comm-logs', (message) => {
          commLogs.value.unshift(JSON.parse(message.body))
          // Keep last 100 logs
          if (commLogs.value.length > 100) {
            commLogs.value = commLogs.value.slice(0, 100)
          }
        })

        // Subscribe to stats
        stompClient.subscribe('/topic/stats', (message) => {
          stats.value = JSON.parse(message.body)
        })
      },
      onDisconnect: () => {
        connected.value = false
        console.log('WebSocket disconnected')
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame)
      }
    })

    stompClient.activate()
    client.value = stompClient
  }

  const disconnect = () => {
    client.value?.deactivate()
    connected.value = false
  }

  return {
    client,
    connected,
    deviceStatus,
    commLogs,
    stats,
    connect,
    disconnect
  }
})
