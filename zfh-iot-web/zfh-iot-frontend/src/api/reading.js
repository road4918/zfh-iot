import request from '@/utils/request'

export function getCurrentData(params) {
  return request({
    url: '/iot/reading/current',
    method: 'get',
    params
  })
}

export function getMeterCurrentData(meterId) {
  return request({
    url: `/iot/reading/${meterId}/current-data`,
    method: 'get'
  })
}

export function getHistoryData(meterId, params) {
  return request({
    url: `/iot/reading/${meterId}/history-data`,
    method: 'get',
    params
  })
}

export function getReadingStatistics(params) {
  return request({
    url: '/iot/reading/statistics',
    method: 'get',
    params
  })
}
