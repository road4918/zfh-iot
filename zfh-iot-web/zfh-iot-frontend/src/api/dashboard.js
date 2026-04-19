import request from '@/utils/request'

export function getDashboardStatistics(tenantId) {
  return request({
    url: '/dashboard/statistics',
    method: 'get',
    params: tenantId ? { tenantId } : {}
  })
}

export function getDeviceTrend(days = 7, tenantId) {
  return request({
    url: '/dashboard/device-trend',
    method: 'get',
    params: { days, ...(tenantId ? { tenantId } : {}) }
  })
}

export function getOnlineRate(days = 7, tenantId) {
  return request({
    url: '/dashboard/online-rate',
    method: 'get',
    params: { days, ...(tenantId ? { tenantId } : {}) }
  })
}

export function getDeviceDataStat(days = 7, tenantId) {
  return request({
    url: '/dashboard/device-data-stat',
    method: 'get',
    params: { days, ...(tenantId ? { tenantId } : {}) }
  })
}

export function getPushStat(days = 7, tenantId) {
  return request({
    url: '/dashboard/push-stat',
    method: 'get',
    params: { days, ...(tenantId ? { tenantId } : {}) }
  })
}

export function getCommandStatus(days = 7, tenantId) {
  return request({
    url: '/dashboard/command-status',
    method: 'get',
    params: { days, ...(tenantId ? { tenantId } : {}) }
  })
}
