import request from '@/utils/request'

export function getGatewayList(params) {
  return request({
    url: '/archive/gateways',
    method: 'get',
    params
  })
}

export function getGatewayById(id) {
  return request({
    url: `/archive/gateways/${id}`,
    method: 'get'
  })
}

export function createGateway(data) {
  return request({
    url: '/archive/gateways',
    method: 'post',
    data
  })
}

export function updateGateway(id, data) {
  return request({
    url: `/archive/gateways/${id}`,
    method: 'put',
    data
  })
}

export function deleteGateway(id) {
  return request({
    url: `/archive/gateways/${id}`,
    method: 'delete'
  })
}
