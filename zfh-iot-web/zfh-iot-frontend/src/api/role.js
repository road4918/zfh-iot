import request from '@/utils/request'

export function getRoleList(params) {
  return request({
    url: '/system/roles',
    method: 'get',
    params
  })
}

export function getRoleById(id) {
  return request({
    url: `/system/roles/${id}`,
    method: 'get'
  })
}

export function createRole(data) {
  return request({
    url: '/system/roles',
    method: 'post',
    data
  })
}

export function updateRole(id, data) {
  return request({
    url: `/system/roles/${id}`,
    method: 'put',
    data
  })
}

export function deleteRole(id) {
  return request({
    url: `/system/roles/${id}`,
    method: 'delete'
  })
}

export function getRolePermissions(id) {
  return request({
    url: `/system/roles/${id}/permissions`,
    method: 'get'
  })
}

export function assignRolePermissions(id, permIds) {
  return request({
    url: `/system/roles/${id}/permissions`,
    method: 'put',
    data: permIds
  })
}

export function getPermissionTree() {
  return request({
    url: '/system/permissions/tree',
    method: 'get'
  })
}
