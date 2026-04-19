import request from '@/utils/request'

export function getTenantList(params) {
  return request({
    url: '/system/tenants',
    method: 'get',
    params
  })
}

export function getTenantById(id) {
  return request({
    url: `/system/tenants/${id}`,
    method: 'get'
  })
}

export function createTenant(data) {
  return request({
    url: '/system/tenants',
    method: 'post',
    data
  })
}

export function updateTenant(id, data) {
  return request({
    url: `/system/tenants/${id}`,
    method: 'put',
    data
  })
}

export function deleteTenant(id) {
  return request({
    url: `/system/tenants/${id}`,
    method: 'delete'
  })
}

export function updateTenantStatus(id, status) {
  return request({
    url: `/system/tenants/${id}/status`,
    method: 'put',
    params: { status }
  })
}
