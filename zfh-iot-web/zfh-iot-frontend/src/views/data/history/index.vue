<template>
  <div class="app-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <span>历史数据</span>
            <el-select
              v-model="queryParams.meterType"
              placeholder="表计类型"
              style="width: 120px; margin-left: 20px"
              @change="handleMeterTypeChange"
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
            >
              <el-option label="累积量" value="cumulant" />
              <el-option label="瞬时量" value="instant" />
            </el-select>
            <el-select
              v-model="queryParams.gatewayId"
              placeholder="选择网关"
              clearable
              filterable
              style="width: 180px; margin-left: 10px"
              @change="handleGatewayChange"
            >
              <el-option
                v-for="item in gatewayList"
                :key="item.id"
                :label="item.gatewayName || item.gatewayNo"
                :value="item.id"
              />
            </el-select>
            <el-select
              v-model="queryParams.meterId"
              placeholder="选择表计"
              clearable
              filterable
              style="width: 240px; margin-left: 10px"
            >
              <el-option
                v-for="item in meterList"
                :key="item.id"
                :label="`${item.meterNo} - ${item.meterName}`"
                :value="item.id"
              />
            </el-select>
            <el-date-picker
              v-model="timeRange"
              type="datetimerange"
              range-separator="至"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              value-format="YYYY-MM-DD HH:mm:ss"
              :default-time="defaultTime"
              style="width: 380px; margin-left: 10px"
            />
            <el-button type="primary" style="margin-left: 10px" @click="handleSearch">
              <el-icon><Search /></el-icon> 查询
            </el-button>
          </div>
        </div>
      </template>

      <template v-if="queryParams.meterId">
        <div v-if="statistics" class="statistics-banner">
          <span class="statistics-title">统计</span>
          <span class="statistics-item statistics-should">应抄：{{ statistics.totalShouldRead }}</span>
          <span class="statistics-item statistics-actual">实抄：{{ statistics.totalActualRead }}</span>
          <span class="statistics-item statistics-rate">
            成功率：{{ formatRate(statistics.averageRate) }}
          </span>
        </div>

        <el-table :data="historyList" v-loading="loading" stripe>
          <el-table-column type="index" label="序号" width="60" />
          <el-table-column prop="ts" label="数据时间" width="180">
            <template #default="{ row }">
              {{ formatTime(row.ts || row.readingTime) }}
            </template>
          </el-table-column>
          <el-table-column prop="meterNo" label="表计编号" width="130">
            <template #default>
              {{ currentMeter?.meterNo || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="meterName" label="表计名称" min-width="140" show-overflow-tooltip>
            <template #default>
              {{ currentMeter?.meterName || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="meterType" label="类型" width="70">
            <template #default>
              <el-tag :color="getMeterTypeColor(activeMeterType)" size="small" style="color: #fff; border: none">
                {{ getMeterTypeLabel(activeMeterType) }}
              </el-tag>
            </template>
          </el-table-column>

          <template v-if="activeMeterType === 1">
            <template v-if="queryParams.electricDataType === 'cumulant'">
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
            <el-table-column prop="totalEnergy" label="表码" width="120">
              <template #default="{ row }">
                {{ formatNumber(row.totalEnergy) }}
              </template>
            </el-table-column>
            <el-table-column prop="batteryLevel" label="电池电量" width="100">
              <template #default="{ row }">
                {{ row.batteryLevel != null ? `${row.batteryLevel}%` : '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="signalQuality" label="信号质量" width="100">
              <template #default="{ row }">
                {{ row.signalQuality != null ? row.signalQuality : '-' }}
              </template>
            </el-table-column>
          </template>

          <template v-else-if="activeMeterType === 3 || activeMeterType === 4">
            <el-table-column prop="totalEnergy" label="表码" width="120">
              <template #default="{ row }">
                {{ formatNumber(row.totalEnergy) }}
              </template>
            </el-table-column>
            <el-table-column prop="temperature" label="温度" width="100">
              <template #default="{ row }">
                {{ formatNumber(row.temperature) }}
              </template>
            </el-table-column>
            <el-table-column prop="pressure" label="压力" width="100">
              <template #default="{ row }">
                {{ formatNumber(row.pressure) }}
              </template>
            </el-table-column>
            <el-table-column prop="flowRate" label="流量" width="100">
              <template #default="{ row }">
                {{ formatNumber(row.flowRate) }}
              </template>
            </el-table-column>
          </template>
        </el-table>

        <div class="pagination-container">
          <el-pagination
            v-model:current-page="queryParams.page"
            v-model:page-size="queryParams.size"
            :total="total"
            :page-sizes="[20, 50, 100, 200]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </template>

      <el-empty v-else description="请选择表计和时间范围查看历史数据" />
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import config from '@/config'
import { getGatewayList } from '@/api/gateway'
import { getMeterList } from '@/api/meter'
import { getHistoryData, getReadingStatistics } from '@/api/reading'
import { useUserStore } from '@/stores/user'
import dayjs from 'dayjs'

const { meterTypes } = config
const userStore = useUserStore()

const loading = ref(false)
const gatewayList = ref([])
const meterList = ref([])
const historyList = ref([])
const statistics = ref(null)
const total = ref(0)

const defaultTime = [
  new Date(0, 0, 0, 0, 0, 0),
  new Date(0, 0, 0, 23, 59, 59)
]

const timeRange = ref([
  dayjs().subtract(7, 'day').format('YYYY-MM-DD 00:00:00'),
  dayjs().format('YYYY-MM-DD HH:mm:ss')
])

const queryParams = reactive({
  page: 1,
  size: 50,
  meterType: 1,
  electricDataType: 'cumulant',
  gatewayId: undefined,
  meterId: undefined
})

const activeMeterType = computed(() => Number(queryParams.meterType || 1))

const currentMeter = computed(() => {
  return meterList.value.find(item => item.id === queryParams.meterId) || null
})

const getMeterTypeLabel = (type) => {
  const item = meterTypes.find(t => t.value === type)
  return item ? item.label : '未知'
}

const getMeterTypeColor = (type) => {
  const item = meterTypes.find(t => t.value === type)
  return item ? item.color : '#909399'
}

const formatTime = (time) => {
  if (!time) return '-'
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

const formatNumber = (value, digits = 2) => {
  return value != null ? Number(value).toFixed(digits) : '-'
}

const formatRate = (value) => {
  return value != null ? `${Number(value).toFixed(2)}%` : '-'
}

const buildStatisticsParams = () => {
  if (!timeRange.value?.length) {
    return null
  }

  return {
    startDate: dayjs(timeRange.value[0]).format('YYYY-MM-DD'),
    endDate: dayjs(timeRange.value[1]).format('YYYY-MM-DD')
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

const fetchMeters = async () => {
  try {
    const res = await getMeterList({
      page: 1,
      size: 2000,
      tenantId: userStore.effectiveTenantId,
      meterType: activeMeterType.value,
      gatewayId: queryParams.gatewayId
    })
    meterList.value = res.list || []

    if (!meterList.value.some(item => item.id === queryParams.meterId)) {
      queryParams.meterId = undefined
    }
  } catch (error) {
    console.error('Failed to fetch meter list:', error)
  }
}

const fetchHistoryData = async () => {
  if (!queryParams.meterId || !timeRange.value?.length) {
    historyList.value = []
    total.value = 0
    return
  }

  loading.value = true
  try {
    const res = await getHistoryData(queryParams.meterId, {
      startTime: timeRange.value[0],
      endTime: timeRange.value[1],
      page: queryParams.page,
      size: queryParams.size
    })
    historyList.value = res.list || []
    total.value = res.total || 0
  } catch (error) {
    console.error('Failed to fetch history data:', error)
  } finally {
    loading.value = false
  }
}

const fetchStatistics = async () => {
  const params = buildStatisticsParams()
  if (!params) {
    statistics.value = null
    return
  }

  try {
    const res = await getReadingStatistics(params)
    statistics.value = res
  } catch (error) {
    console.error('Failed to fetch statistics:', error)
    statistics.value = null
  }
}

const handleSearch = async () => {
  queryParams.page = 1
  await fetchHistoryData()

  if (queryParams.meterId) {
    await fetchStatistics()
  } else {
    statistics.value = null
  }
}

const handleMeterTypeChange = async () => {
  if (activeMeterType.value !== 1) {
    queryParams.electricDataType = 'cumulant'
  }

  queryParams.page = 1
  await fetchMeters()
  historyList.value = []
  total.value = 0
  statistics.value = null
}

const handleGatewayChange = async () => {
  queryParams.page = 1
  await fetchMeters()
  historyList.value = []
  total.value = 0
  statistics.value = null
}

const handleSizeChange = (val) => {
  queryParams.size = val
  fetchHistoryData()
}

const handleCurrentChange = (val) => {
  queryParams.page = val
  fetchHistoryData()
}

onMounted(async () => {
  await Promise.all([fetchGateways(), fetchMeters()])
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
      flex-wrap: wrap;
      gap: 0;
    }
  }

  .statistics-banner {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 16px;
    margin-bottom: 16px;
    padding: 10px 14px;
    background: #fef6ec;
    border: 1px solid #f5dab1;
    border-radius: 8px;
    color: #7a4b10;
    font-size: 13px;

    .statistics-title {
      font-size: 15px;
      font-weight: 700;
      color: #d46b08;
    }

    .statistics-item {
      font-weight: 500;
    }

    .statistics-should {
      color: #0958d9;
    }

    .statistics-actual {
      color: #389e0d;
    }

    .statistics-rate {
      color: #cf1322;
      font-weight: 700;
    }
  }

  .pagination-container {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
