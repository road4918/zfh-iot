import request from '../utils/request'

export const getMeterList = (params) => {
  return request.get('/meters', { params })
}

export const getMeterById = (id) => {
  return request.get(`/meters/${id}`)
}

export const createMeter = (data) => {
  return request.post('/meters', data)
}

export const updateMeter = (id, data) => {
  return request.put(`/meters/${id}`, data)
}

export const deleteMeter = (id) => {
  return request.delete(`/meters/${id}`)
}

export const startMeter = (id) => {
  return request.post(`/meters/${id}/start`)
}

export const stopMeter = (id) => {
  return request.post(`/meters/${id}/stop`)
}
