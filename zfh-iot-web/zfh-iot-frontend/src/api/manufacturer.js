import request from '@/utils/request'

export function getManufacturerList(params) {
  return request({
    url: '/archive/manufacturers',
    method: 'get',
    params
  })
}

export function getManufacturerById(id) {
  return request({
    url: `/archive/manufacturers/${id}`,
    method: 'get'
  })
}

export function createManufacturer(data) {
  return request({
    url: '/archive/manufacturers',
    method: 'post',
    data
  })
}

export function updateManufacturer(id, data) {
  return request({
    url: `/archive/manufacturers/${id}`,
    method: 'put',
    data
  })
}

export function deleteManufacturer(id) {
  return request({
    url: `/archive/manufacturers/${id}`,
    method: 'delete'
  })
}
