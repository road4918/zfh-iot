import request from '../utils/request'

export const getGatewayList = (params) => {
  return request.get('/gateways', { params })
}

export const getGatewayById = (id) => {
  return request.get(`/gateways/${id}`)
}

export const createGateway = (data) => {
  return request.post('/gateways', data)
}

export const updateGateway = (id, data) => {
  return request.put(`/gateways/${id}`, data)
}

export const deleteGateway = (id) => {
  return request.delete(`/gateways/${id}`)
}

export const startGateway = (id) => {
  return request.post(`/gateways/${id}/start`)
}

export const stopGateway = (id) => {
  return request.post(`/gateways/${id}/stop`)
}
