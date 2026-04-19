<template>
  <div class="log-list">
    <div class="toolbar">
      <el-button type="danger" @click="handleClear">
        <el-icon><Delete /></el-icon>清空日志
      </el-button>
      <el-tag :type="wsStore.connected ? 'success' : 'danger'">
        WebSocket: {{ wsStore.connected ? '已连接' : '未连接' }}
      </el-tag>
    </div>
    
    <el-table :data="logList" v-loading="loading" border height="600">
      <el-table-column prop="timestamp" label="时间" width="180" />
      <el-table-column prop="deviceType" label="设备类型" width="100">
        <template #default="{ row }">
          <el-tag>{{ row.deviceType === 'GATEWAY' ? '网关' : '表计' }}</el-tag>
        </template>
      </el-table-column>
      
      <el-table-column prop="deviceId" label="设备ID" width="80" />
      
      <el-table-column prop="direction" label="方向" width="80">
        <template #default="{ row }">
          <el-tag :type="row.direction === 'UP' ? 'success' : 'primary'">
            {{ row.direction === 'UP' ? '↑ 上行' : '↓ 下行' }}
          </el-tag>
        </template>
      </el-table-column>
      
      <el-table-column prop="protocol" label="协议" width="100" />
      
      <el-table-column prop="rawData" label="原始报文" show-overflow-tooltip />
      
      <el-table-column prop="parsedData" label="解析数据" show-overflow-tooltip />
    </el-table>
    
    <el-pagination
      v-model:current-page="page.current"
      v-model:page-size="page.size"
      :total="page.total"
      layout="total, prev, pager, next"
      @change="loadData"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getLogList, clearLogs } from '../../api/log'
import { useWebSocketStore } from '../../stores/websocket'

const loading = ref(false)
const logList = ref([])
const wsStore = useWebSocketStore()

const page = reactive({
  current: 1,
  size: 20,
  total: 0
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await getLogList({
      current: page.current,
      size: page.size
    })
    logList.value = res.data.records
    page.total = res.data.total
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleClear = async () => {
  try {
    await ElMessageBox.confirm('确认清空所有日志？', '提示', { type: 'warning' })
    await clearLogs()
    ElMessage.success('清空成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('清空失败')
    }
  }
}

// Watch for real-time WebSocket logs
watch(() => wsStore.commLogs, (newLogs) => {
  if (newLogs.length > 0 && page.current === 1) {
    // Prepend new logs to the list
    logList.value = [...newLogs.slice(0, 10), ...logList.value].slice(0, page.size)
  }
}, { deep: true })

onMounted(() => {
  loadData()
  wsStore.connect()
})

onUnmounted(() => {
  wsStore.disconnect()
})
</script>

<style scoped>
.log-list {
  .toolbar {
    margin-bottom: 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}
</style>
