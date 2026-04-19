import request from '@/utils/request'

export function getUserList(params) {
  return request({
    url: '/system/users',
    method: 'get',
    params
  })
}

export function getUserById(id) {
  return request({
    url: `/system/users/${id}`,
    method: 'get'
  })
}

export function createUser(data) {
  return request({
    url: '/system/users',
    method: 'post',
    data
  })
}

export function updateUser(id, data) {
  return request({
    url: `/system/users/${id}`,
    method: 'put',
    data
  })
}

export function deleteUser(id) {
  return request({
    url: `/system/users/${id}`,
    method: 'delete'
  })
}

export function resetUserPassword(id) {
  return request({
    url: `/system/users/${id}/reset-password`,
    method: 'put'
  })
}

export function getUserRoles(id) {
  return request({
    url: `/system/users/${id}/roles`,
    method: 'get'
  })
}

export function changeOwnPassword(data) {
  return request({
    url: '/system/users/change-password',
    method: 'put',
    data
  })
}
