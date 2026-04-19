<template>
  <div class="gateway-list">
    <div class="toolbar">
      <el-button type="primary" @click="handleCreate">
        <el-icon><Plus /></el-icon>新增网关
      </el-button>
    </div>
    
    <el-table :data="gatewayList" v-loading="loading" border>
      <el-table-column prop="name" label="网关名称" />
      <el-table-column prop="communicationAddress" label="通讯地址" />
      <el-table-column prop="protocol" label="协议" width="100" />
      <el-table-column prop="commMode" label="通讯模式" width="100">
        <template #default="{ row }">
          <el-tag>{{ row.commMode === 'SERVER' ? '服务端' : '客户端' }}</el-tag>
        </template>
      </el-table-column>
      
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">
            {{ getStatusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      
      <el-table-column label="操作" width="250" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 'OFFLINE'"
            type="success"
            size="small"
            @click="handleStart(row)"
          >
            启动
          </el-button>
          <el-button
            v-else
            type="warning"
            size="small"
            @click="handleStop(row)"
          >
            停止
          </el-button>
          <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <el-pagination
      v-model:current-page="page.current"
      v-model:page-size="page.size"
      :total="page.total"
      layout="total, prev, pager, next"
      @change="loadData"
    />
    
    <GatewayForm
      v-model:visible="formVisible"
      :data="currentRow"
      @success="loadData"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getGatewayList, startGateway, stopGateway, deleteGateway } from '../../api/gateway'
import GatewayForm from './GatewayForm.vue'

const loading = ref(false)
const gatewayList = ref([])
const formVisible = ref(false)
const currentRow = ref(null)

const page = reactive({
  current: 1,
  size: 10,
  total: 0
})

const getStatusType = (status) => {
  const map = { ONLINE: 'success', OFFLINE: 'info', ERROR: 'danger' }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = { ONLINE: '在线', OFFLINE: '离线', ERROR: '异常' }
  return map[status] || status
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getGatewayList({ current: page.current, size: page.size })
    gatewayList.value = res.data.records
    page.total = res.data.total
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleCreate = () => {
  currentRow.value = null
  formVisible.value = true
}

const handleEdit = (row) => {
  currentRow.value = row
  formVisible.value = true
}

const handleStart = async (row) => {
  try {
    await startGateway(row.id)
    ElMessage.success('启动成功')
    loadData()
  } catch (error) {
    ElMessage.error('启动失败')
  }
}

const handleStop = async (row) => {
  try {
    await stopGateway(row.id)
    ElMessage.success('停止成功')
    loadData()
  } catch (error) {
    ElMessage.error('停止失败')
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确认删除该网关？', '提示', { type: 'warning' })
    await deleteGateway(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(loadData)
</script>

<style scoped>
.gateway-list {
  .toolbar {
    margin-bottom: 20px;
  }
}
</style>
