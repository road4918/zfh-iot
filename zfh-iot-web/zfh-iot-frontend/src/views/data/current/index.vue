<template>
  <div class="app-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <span>当前数据</span>
            <el-select
              v-model="queryParams.meterType"
              placeholder="表计类型"
              style="width: 120px; margin-left: 20px"
              @change="handleSearch"
            >
              <el-option
                v-for="item in meterTypes"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
            <el-select
              v-if="activeMeterType === 1"
              v-model="queryParams.electricDataType"
              placeholder="数据类型"
              style="width: 120px; margin-left: 10px"
              @change="handleSearch"
            >
              <el-option label="累积量" value="cumulant" />
              <el-option label="瞬时量" value="instant" />
            </el-select>
            <el-select
              v-model="queryParams.gatewayId"
              placeholder="选择网关"
              clearable
              style="width: 180px; margin-left: 10px"
              @change="handleSearch"
            >
              <el-option
                v-for="item in gatewayList"
                :key="item.id"
                :label="item.gatewayName || item.gatewayNo"
                :value="item.id"
              />
            </el-select>
            <el-button style="margin-left: 10px" @click="handleSearch">
              <el-icon><Search /></el-icon> 查询
            </el-button>
          </div>
        </div>
      </template>

      <el-table :data="dataList" v-loading="loading" stripe>
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="meterNo" label="表计编号" width="130" />
        <el-table-column prop="meterName" label="表计名称" min-width="130" show-overflow-tooltip />
        <el-table-column prop="meterType" label="类型" width="70">
          <template #default="{ row }">
            <el-tag :color="getMeterTypeColor(row.meterType)" size="small" style="color: #fff; border: none">
              {{ getMeterTypeLabel(row.meterType) }}
            </el-tag>
          </template>
        </el-table-column>
        <template v-if="activeMeterType === 1">
          <template v-if="queryParams.electricDataType === 'cumulant'">
            <el-table-column prop="cumulantDataTime" label="累积量数据时间" width="170">
              <template #default="{ row }">
                {{ formatTime(row.cumulantDataTime) }}
              </template>
            </el-table-column>
            <el-table-column prop="cumulantReportTime" label="累积量上报时间" width="170">
              <template #default="{ row }">
                {{ formatTime(row.cumulantReportTime) }}
              </template>
            </el-table-column>
            <el-table-column prop="forwardActive" label="正向有功" width="120">
              <template #default="{ row }">
                {{ formatNumber(row.forwardActive) }}
              </template>
            </el-table-column>
            <el-table-column prop="reverseActive" label="反向有功" width="120">
              <template #default="{ row }">
                {{ formatNumber(row.reverseActive) }}
              </template>
            </el-table-column>
            <el-table-column prop="forwardReactive" label="正向无功" width="120">
              <template #default="{ row }">
                {{ formatNumber(row.forwardReactive) }}
              </template>
            </el-table-column>
            <el-table-column prop="reverseReactive" label="反向无功" width="120">
              <template #default="{ row }">
                {{ formatNumber(row.reverseReactive) }}
              </template>
            </el-table-column>
          </template>

          <template v-else>
            <el-table-column prop="instantDataTime" label="瞬时量数据时间" width="170">
              <template #default="{ row }">
                {{ formatTime(row.instantDataTime) }}
              </template>
            </el-table-column>
            <el-table-column prop="instantReportTime" label="瞬时量上报时间" width="170">
              <template #default="{ row }">
                {{ formatTime(row.instantReportTime) }}
              </template>
            </el-table-column>
            <el-table-column prop="voltageA" label="A相电压(V)" width="120">
              <template #default="{ row }">
                {{ formatNumber(row.voltageA, 1) }}
              </template>
            </el-table-column>
            <el-table-column prop="voltageB" label="B相电压(V)" width="120">
              <template #default="{ row }">
                {{ formatNumber(row.voltageB, 1) }}
              </template>
            </el-table-column>
            <el-table-column prop="voltageC" label="C相电压(V)" width="120">
              <template #default="{ row }">
                {{ formatNumber(row.voltageC, 1) }}
              </template>
            </el-table-column>
            <el-table-column prop="currentA" label="A相电流(A)" width="120">
              <template #default="{ row }">
                {{ formatNumber(row.currentA) }}
              </template>
            </el-table-column>
            <el-table-column prop="currentB" label="B相电流(A)" width="120">
              <template #default="{ row }">
                {{ formatNumber(row.currentB) }}
              </template>
            </el-table-column>
            <el-table-column prop="currentC" label="C相电流(A)" width="120">
              <template #default="{ row }">
                {{ formatNumber(row.currentC) }}
              </template>
            </el-table-column>
            <el-table-column prop="powerActive" label="有功功率(kW)" width="130">
              <template #default="{ row }">
                {{ formatNumber(row.powerActive) }}
              </template>
            </el-table-column>
            <el-table-column prop="powerReactive" label="无功功率(kvar)" width="140">
              <template #default="{ row }">
                {{ formatNumber(row.powerReactive) }}
              </template>
            </el-table-column>
            <el-table-column prop="powerFactor" label="功率因数" width="110">
              <template #default="{ row }">
                {{ formatNumber(row.powerFactor) }}
              </template>
            </el-table-column>
            <el-table-column prop="frequency" label="频率(Hz)" width="110">
              <template #default="{ row }">
                {{ formatNumber(row.frequency) }}
              </template>
            </el-table-column>
          </template>
        </template>

        <template v-else-if="activeMeterType === 2">
          <el-table-column prop="dataTime" label="数据时间" width="170">
            <template #default="{ row }">
              {{ formatTime(row.dataTime || row.readTime) }}
            </template>
          </el-table-column>
          <el-table-column prop="reportTime" label="上报时间" width="170">
            <template #default="{ row }">
              {{ formatTime(row.reportTime) }}
            </template>
          </el-table-column>
          <el-table-column prop="totalEnergy" label="表码" width="120">
            <template #default="{ row }">
              {{ formatNumber(row.totalEnergy) }}
            </template>
          </el-table-column>
          <el-table-column prop="batteryStatus" label="电池状态" width="110">
            <template #default="{ row }">
              {{ getBatteryStatusLabel(row.batteryStatus) }}
            </template>
          </el-table-column>
          <el-table-column prop="valveStatus" label="阀门状态" width="110">
            <template #default="{ row }">
              {{ row.valveStatus || '-' }}
            </template>
          </el-table-column>
        </template>

        <template v-else-if="activeMeterType === 3 || activeMeterType === 4">
          <el-table-column prop="dataTime" label="数据时间" width="170">
            <template #default="{ row }">
              {{ formatTime(row.dataTime || row.readTime) }}
            </template>
          </el-table-column>
          <el-table-column prop="reportTime" label="上报时间" width="170">
            <template #default="{ row }">
              {{ formatTime(row.reportTime) }}
            </template>
          </el-table-column>
          <el-table-column prop="totalEnergy" label="表码" width="120">
            <template #default="{ row }">
              {{ formatNumber(row.totalEnergy) }}
            </template>
          </el-table-column>
        </template>

        <el-table-column prop="status" label="状态" width="70">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="queryParams.page"
          v-model:page-size="queryParams.size"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, ref, reactive, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import config from '@/config'
import { getCurrentData } from '@/api/reading'
import { getGatewayList } from '@/api/gateway'
import { useUserStore } from '@/stores/user'
import dayjs from 'dayjs'

const { meterTypes, deviceStatus } = config
const userStore = useUserStore()

const loading = ref(false)
const dataList = ref([])
const gatewayList = ref([])
const total = ref(0)

const queryParams = reactive({
  page: 1,
  size: 20,
  meterType: 1,
  electricDataType: 'cumulant',
  gatewayId: undefined
})

const normalizeMeterType = (value) => {
  if (value == null || value === '') {
    return null
  }
  return Number(value)
}

const activeMeterType = computed(() => normalizeMeterType(queryParams.meterType))

const getMeterTypeLabel = (type) => {
  const item = meterTypes.find(t => t.value === type)
  return item ? item.label : '未知'
}

const getMeterTypeColor = (type) => {
  const item = meterTypes.find(t => t.value === type)
  return item ? item.color : '#909399'
}

const getStatusType = (status) => {
  const item = deviceStatus.find(s => s.value === status)
  return item ? item.type : 'info'
}

const getStatusLabel = (status) => {
  const item = deviceStatus.find(s => s.value === status)
  return item ? item.label : '未知'
}

const formatTime = (time) => {
  if (!time) return '-'
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

const formatNumber = (value, digits = 2) => {
  return value != null ? Number(value).toFixed(digits) : '-'
}

const getBatteryStatusLabel = (status) => {
  if (status == null) return '-'
  if (status === 0) return '正常'
  if (status === 1) return '欠压'
  return String(status)
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getCurrentData(queryParams)
    dataList.value = res.list || []
    total.value = res.total || 0
  } catch (error) {
    console.error('Failed to fetch current data:', error)
  } finally {
    loading.value = false
  }
}

const fetchGateways = async () => {
  try {
    const res = await getGatewayList({ page: 1, size: 500, tenantId: userStore.effectiveTenantId })
    gatewayList.value = res.list || []
  } catch (error) {
    console.error('Failed to fetch gateway list:', error)
  }
}

const handleSearch = () => {
  if (activeMeterType.value === 1 && !queryParams.electricDataType) {
    queryParams.electricDataType = 'cumulant'
  }
  if (activeMeterType.value !== 1) {
    queryParams.electricDataType = 'cumulant'
  }
  queryParams.page = 1
  fetchData()
}

const handleSizeChange = (val) => {
  queryParams.size = val
  fetchData()
}

const handleCurrentChange = (val) => {
  queryParams.page = val
  fetchData()
}

onMounted(() => {
  fetchData()
  fetchGateways()
})
</script>

<style scoped lang="scss">
.app-container {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .header-left {
      display: flex;
      align-items: center;
    }
  }

  .pagination-container {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
