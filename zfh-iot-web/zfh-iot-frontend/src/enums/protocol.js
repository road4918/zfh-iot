export const GatewayProtocolEnum = {
  DLT645_2007: { code: 'DLT645_2007', name: 'DL/T645-2007扩展协议' },
  MQTT_ENERGY: { code: 'MQTT_ENERGY', name: 'MQTT能源管理物联协议' }
}

export const GatewayProtocolList = Object.values(GatewayProtocolEnum)

export const getGatewayProtocolName = (code) => {
  const item = GatewayProtocolList.find(p => p.code === code)
  return item ? item.name : code || '-'
}
