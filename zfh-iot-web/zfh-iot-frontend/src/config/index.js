const config = {
  title: '智慧物联网平台',
  defaultPageSize: 20,
  maxPageSize: 100,
  meterTypes: [
    { value: 1, label: '电表', color: '#409eff' },
    { value: 2, label: '水表', color: '#67c23a' },
    { value: 3, label: '气表', color: '#e6a23c' },
    { value: 4, label: '热表', color: '#f56c6c' }
  ],
  deviceStatus: [
    { value: 0, label: '离线', type: 'info' },
    { value: 1, label: '在线', type: 'success' },
    { value: 2, label: '未激活', type: 'warning' },
    { value: 3, label: '异常', type: 'danger' }
  ]
}

export default config
