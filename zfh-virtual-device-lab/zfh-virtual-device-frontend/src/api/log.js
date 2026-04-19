import request from '../utils/request'

export const getLogList = (params) => {
  return request.get('/logs', { params })
}

export const clearLogs = () => {
  return request.delete('/logs')
}
