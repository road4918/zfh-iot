import request from '@/utils/request'

export function getProtocolList(params) {
  return request({
    url: '/archive/protocols',
    method: 'get',
    params
  })
}

export function getProtocolByCode(code) {
  return request({
    url: `/archive/protocols/${code}`,
    method: 'get'
  })
}

export function createProtocol(data) {
  return request({
    url: '/archive/protocols',
    method: 'post',
    data
  })
}

export function updateProtocol(code, data) {
  return request({
    url: `/archive/protocols/${code}`,
    method: 'put',
    data
  })
}

export function deleteProtocol(code) {
  return request({
    url: `/archive/protocols/${code}`,
    method: 'delete'
  })
}
