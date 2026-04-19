<template>
  <div class="dashboard-container">
    <el-row :gutter="20" class="stat-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item">
            <div class="stat-icon" style="background-color: #409eff;">
              <el-icon :size="28"><Monitor /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-title">设备总数</div>
              <div class="stat-value">{{ statistics.devices?.total || 0 }}</div>
            </div>
          </div>
          <div class="stat-detail">
            <span class="detail-item online">在线 {{ statistics.devices?.online || 0 }}</span>
            <span class="detail-item offline">离线 {{ statistics.devices?.offline || 0 }}</span>
            <span class="detail-item abnormal">异常 {{ statistics.devices?.abnormal || 0 }}</span>
            <span class="detail-item inactive">未激活 {{ statistics.devices?.inactive || 0 }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item">
            <div class="stat-icon" style="background-color: #67c23a;">
              <el-icon :size="28"><Connection /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-title">在线设备</div>
              <div class="stat-value">{{ statistics.devices?.online || 0 }}</div>
            </div>
          </div>
          <div class="stat-detail">
            <span>在线率 {{ onlineRate }}%</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item">
            <div class="stat-icon" style="background-color: #e6a23c;">
              <el-icon :size="28"><ChatDotRound /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-title">上报消息数</div>
              <div class="stat-value">{{ statistics.messages?.messageCount || 0 }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item">
            <div class="stat-icon" style="background-color: #f56c6c;">
              <el-icon :size="28"><Position /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-title">下发命令数</div>
              <div class="stat-value">{{ statistics.messages?.commandCount || 0 }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>设备趋势图</span>
              <el-radio-group v-model="trendDays" size="small" @change="fetchDeviceTrend">
                <el-radio-button :label="7">近7天</el-radio-button>
                <el-radio-button :label="30">近30天</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="trendChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>设备在线率</span>
              <el-radio-group v-model="onlineRateDays" size="small" @change="fetchOnlineRate">
                <el-radio-button :label="7">近7天</el-radio-button>
                <el-radio-button :label="30">近30天</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="onlineRateChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>设备数据统计图</span>
              <el-radio-group v-model="deviceDataDays" size="small" @change="fetchDeviceDataStat">
                <el-radio-button :label="7">近7天</el-radio-button>
                <el-radio-button :label="30">近30天</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="deviceDataChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>应用侧推送统计</span>
              <el-radio-group v-model="pushDays" size="small" @change="fetchPushStat">
                <el-radio-button :label="7">近7天</el-radio-button>
                <el-radio-button :label="30">近30天</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="pushChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="24">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>命令状态统计</span>
              <el-radio-group v-model="commandDays" size="small" @change="fetchCommandStatus">
                <el-radio-button :label="7">近7天</el-radio-button>
                <el-radio-button :label="30">近30天</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="commandChartRef" class="chart-container-wide"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, computed, nextTick, watch } from 'vue'
import { Monitor, Connection, ChatDotRound, Position } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { useUserStore } from '@/stores/user'
import {
  getDashboardStatistics,
  getDeviceTrend,
  getOnlineRate,
  getDeviceDataStat,
  getPushStat,
  getCommandStatus
} from '@/api/dashboard'

const userStore = useUserStore()

const statistics = ref({
  devices: { total: 0, online: 0, offline: 0, abnormal: 0, inactive: 0 },
  messages: { messageCount: 0, commandCount: 0 }
})

const onlineRate = computed(() => {
  const d = statistics.value.devices
  if (!d || d.total === 0) return 0
  return ((d.online / d.total) * 100).toFixed(1)
})

const trendDays = ref(7)
const onlineRateDays = ref(7)
const deviceDataDays = ref(7)
const pushDays = ref(7)
const commandDays = ref(7)

const trendChartRef = ref(null)
const onlineRateChartRef = ref(null)
const deviceDataChartRef = ref(null)
const pushChartRef = ref(null)
const commandChartRef = ref(null)

let trendChart = null
let onlineRateChart = null
let deviceDataChart = null
let pushChart = null
let commandChart = null

const initCharts = () => {
  if (trendChartRef.value) trendChart = echarts.init(trendChartRef.value)
  if (onlineRateChartRef.value) onlineRateChart = echarts.init(onlineRateChartRef.value)
  if (deviceDataChartRef.value) deviceDataChart = echarts.init(deviceDataChartRef.value)
  if (pushChartRef.value) pushChart = echarts.init(pushChartRef.value)
  if (commandChartRef.value) commandChart = echarts.init(commandChartRef.value)
}

const handleResize = () => {
  trendChart?.resize()
  onlineRateChart?.resize()
  deviceDataChart?.resize()
  pushChart?.resize()
  commandChart?.resize()
}

const getTenantId = () => userStore.effectiveTenantId

const fetchStatistics = async () => {
  try {
    const data = await getDashboardStatistics(getTenantId())
    statistics.value = data
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

const fetchDeviceTrend = async () => {
  try {
    const data = await getDeviceTrend(trendDays.value, getTenantId())
    const dates = data.map(d => d.date)
    const totals = data.map(d => d.total)
    const onlines = data.map(d => d.online)
    trendChart?.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['设备总数', '在线数'], bottom: 0 },
      grid: { left: '3%', right: '4%', bottom: '12%', top: '8%', containLabel: true },
      xAxis: { type: 'category', boundaryGap: false, data: dates },
      yAxis: { type: 'value' },
      series: [
        {
          name: '设备总数',
          type: 'line',
          smooth: true,
          data: totals,
          lineStyle: { width: 2, color: '#409eff' },
          itemStyle: { color: '#409eff' },
          areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64,158,255,0.3)' },
            { offset: 1, color: 'rgba(64,158,255,0.05)' }
          ])}
        },
        {
          name: '在线数',
          type: 'line',
          smooth: true,
          data: onlines,
          lineStyle: { width: 2, color: '#67c23a' },
          itemStyle: { color: '#67c23a' },
          areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(103,194,58,0.3)' },
            { offset: 1, color: 'rgba(103,194,58,0.05)' }
          ])}
        }
      ]
    })
  } catch (error) {
    console.error('获取设备趋势失败:', error)
  }
}

const fetchOnlineRate = async () => {
  try {
    const data = await getOnlineRate(onlineRateDays.value, getTenantId())
    const dates = data.map(d => d.date)
    const onlineRates = data.map(d => d.onlineRate)
    const offlineRates = data.map(d => d.offlineRate)
    onlineRateChart?.setOption({
      tooltip: { trigger: 'axis', formatter: '{b}<br/>{a0}: {c0}%<br/>{a1}: {c1}%' },
      legend: { data: ['在线率', '离线率'], bottom: 0 },
      grid: { left: '3%', right: '4%', bottom: '12%', top: '8%', containLabel: true },
      xAxis: { type: 'category', boundaryGap: false, data: dates },
      yAxis: { type: 'value', axisLabel: { formatter: '{value}%' }, max: 100 },
      series: [
        {
          name: '在线率',
          type: 'line',
          smooth: true,
          data: onlineRates,
          lineStyle: { width: 2, color: '#67c23a' },
          itemStyle: { color: '#67c23a' },
          areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(103,194,58,0.3)' },
            { offset: 1, color: 'rgba(103,194,58,0.05)' }
          ])}
        },
        {
          name: '离线率',
          type: 'line',
          smooth: true,
          data: offlineRates,
          lineStyle: { width: 2, color: '#f56c6c' },
          itemStyle: { color: '#f56c6c' },
          areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(245,108,108,0.3)' },
            { offset: 1, color: 'rgba(245,108,108,0.05)' }
          ])}
        }
      ]
    })
  } catch (error) {
    console.error('获取在线率失败:', error)
  }
}

const fetchDeviceDataStat = async () => {
  try {
    const data = await getDeviceDataStat(deviceDataDays.value, getTenantId())
    const dates = data.map(d => d.date)
    deviceDataChart?.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['异常', '离线', '未激活'], bottom: 0 },
      grid: { left: '3%', right: '4%', bottom: '12%', top: '8%', containLabel: true },
      xAxis: { type: 'category', boundaryGap: false, data: dates },
      yAxis: { type: 'value' },
      series: [
        {
          name: '异常',
          type: 'line',
          smooth: true,
          data: data.map(d => d.abnormal),
          lineStyle: { width: 2, color: '#f56c6c' },
          itemStyle: { color: '#f56c6c' }
        },
        {
          name: '离线',
          type: 'line',
          smooth: true,
          data: data.map(d => d.offline),
          lineStyle: { width: 2, color: '#909399' },
          itemStyle: { color: '#909399' }
        },
        {
          name: '未激活',
          type: 'line',
          smooth: true,
          data: data.map(d => d.inactive),
          lineStyle: { width: 2, color: '#e6a23c' },
          itemStyle: { color: '#e6a23c' }
        }
      ]
    })
  } catch (error) {
    console.error('获取设备数据统计失败:', error)
  }
}

const fetchPushStat = async () => {
  try {
    const data = await getPushStat(pushDays.value, getTenantId())
    const dates = data.map(d => d.date)
    pushChart?.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['全部', '成功', '失败'], bottom: 0 },
      grid: { left: '3%', right: '4%', bottom: '12%', top: '8%', containLabel: true },
      xAxis: { type: 'category', boundaryGap: false, data: dates },
      yAxis: { type: 'value' },
      series: [
        {
          name: '全部',
          type: 'line',
          smooth: true,
          data: data.map(d => d.total),
          lineStyle: { width: 2, color: '#409eff' },
          itemStyle: { color: '#409eff' }
        },
        {
          name: '成功',
          type: 'line',
          smooth: true,
          data: data.map(d => d.success),
          lineStyle: { width: 2, color: '#67c23a' },
          itemStyle: { color: '#67c23a' }
        },
        {
          name: '失败',
          type: 'line',
          smooth: true,
          data: data.map(d => d.fail),
          lineStyle: { width: 2, color: '#f56c6c' },
          itemStyle: { color: '#f56c6c' }
        }
      ]
    })
  } catch (error) {
    console.error('获取推送统计失败:', error)
  }
}

const fetchCommandStatus = async () => {
  try {
    const data = await getCommandStatus(commandDays.value, getTenantId())
    const dates = data.map(d => d.date)
    const colors = ['#67c23a', '#f56c6c', '#409eff', '#909399', '#e6a23c', '#b37feb', '#ffd666', '#36cfc9']
    const names = ['已送达', '失败', '成功', '超期', '超时', '取消', '等待', '已发送']
    const fields = ['delivered', 'failed', 'success', 'overdue', 'timeout', 'cancelled', 'waiting', 'sent']
    commandChart?.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: names, bottom: 0, type: 'scroll' },
      grid: { left: '3%', right: '4%', bottom: '12%', top: '8%', containLabel: true },
      xAxis: { type: 'category', boundaryGap: false, data: dates },
      yAxis: { type: 'value' },
      series: fields.map((field, i) => ({
        name: names[i],
        type: 'line',
        smooth: true,
        data: data.map(d => d[field]),
        lineStyle: { width: 2, color: colors[i] },
        itemStyle: { color: colors[i] }
      }))
    })
  } catch (error) {
    console.error('获取命令状态失败:', error)
  }
}

const fetchAllData = () => {
  fetchStatistics()
  fetchDeviceTrend()
  fetchOnlineRate()
  fetchDeviceDataStat()
  fetchPushStat()
  fetchCommandStatus()
}

onMounted(async () => {
  await nextTick()
  initCharts()
  window.addEventListener('resize', handleResize)
  fetchAllData()
})

watch(() => userStore.currentTenantId, () => {
  fetchAllData()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  onlineRateChart?.dispose()
  deviceDataChart?.dispose()
  pushChart?.dispose()
  commandChart?.dispose()
})
</script>

<style scoped lang="scss">
.dashboard-container {
  padding: 10px;

  .stat-row {
    .stat-card {
      .stat-item {
        display: flex;
        align-items: center;

        .stat-icon {
          width: 56px;
          height: 56px;
          border-radius: 12px;
          display: flex;
          align-items: center;
          justify-content: center;
          color: #fff;
          flex-shrink: 0;
        }

        .stat-info {
          margin-left: 16px;

          .stat-title {
            font-size: 14px;
            color: #909399;
            margin-bottom: 4px;
          }

          .stat-value {
            font-size: 28px;
            font-weight: bold;
            color: #303133;
          }
        }
      }

      .stat-detail {
        margin-top: 12px;
        padding-top: 12px;
        border-top: 1px solid #f0f0f0;
        font-size: 13px;
        color: #606266;
        display: flex;
        gap: 12px;
        flex-wrap: wrap;

        .detail-item {
          &.online { color: #67c23a; }
          &.offline { color: #909399; }
          &.abnormal { color: #f56c6c; }
          &.inactive { color: #e6a23c; }
        }
      }
    }
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-weight: 600;
  }

  .chart-container {
    height: 320px;
  }

  .chart-container-wide {
    height: 320px;
  }
}
</style>
