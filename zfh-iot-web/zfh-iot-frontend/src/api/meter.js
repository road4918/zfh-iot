import request from '@/utils/request'

export function getMeterList(params) {
  return request({
    url: '/archive/meters',
    method: 'get',
    params
  })
}

export function getMeterById(id) {
  return request({
    url: `/archive/meters/${id}`,
    method: 'get'
  })
}

export function createMeter(data) {
  return request({
    url: '/archive/meters',
    method: 'post',
    data
  })
}

export function updateMeter(id, data) {
  return request({
    url: `/archive/meters/${id}`,
    method: 'put',
    data
  })
}

export function deleteMeter(id) {
  return request({
    url: `/archive/meters/${id}`,
    method: 'delete'
  })
}

export function bindGateway(id, gatewayId) {
  return request({
    url: `/archive/meters/${id}/bind-gateway`,
    method: 'put',
    params: { gatewayId }
  })
}

export function unbindGateway(id) {
  return request({
    url: `/archive/meters/${id}/unbind-gateway`,
    method: 'put'
  })
}

export function bindGroup(id, groupId) {
  return request({
    url: `/archive/meters/${id}/bind-group`,
    method: 'put',
    params: { groupId }
  })
}

export function unbindGroup(id) {
  return request({
    url: `/archive/meters/${id}/unbind-group`,
    method: 'put'
  })
}
