import request from '@/utils/request'

export function getGroupTree(params) {
  return request({
    url: '/archive/groups/tree',
    method: 'get',
    params
  })
}

export function getGroupList(params) {
  return request({
    url: '/archive/groups',
    method: 'get',
    params
  })
}

export function createGroup(data) {
  return request({
    url: '/archive/groups',
    method: 'post',
    data
  })
}

export function updateGroup(id, data) {
  return request({
    url: `/archive/groups/${id}`,
    method: 'put',
    data
  })
}

export function deleteGroup(id) {
  return request({
    url: `/archive/groups/${id}`,
    method: 'delete'
  })
}

export function addGroupMeters(groupId, meterIds) {
  return request({
    url: `/archive/groups/${groupId}/meters`,
    method: 'post',
    data: meterIds
  })
}

export function removeGroupMeter(groupId, meterId) {
  return request({
    url: `/archive/groups/${groupId}/meters/${meterId}`,
    method: 'delete'
  })
}

export function getGroupMeters(groupId) {
  return request({
    url: `/archive/groups/${groupId}/meters`,
    method: 'get'
  })
}
