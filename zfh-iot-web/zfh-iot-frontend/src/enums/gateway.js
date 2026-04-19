export const GatewayTypeEnum = {
  TCP_DIRECT: { code: 'TCP_DIRECT', name: 'TCP直连' },
  MQTT: { code: 'MQTT', name: 'MQTT' }
}

export const GatewayTypeList = Object.values(GatewayTypeEnum)

export const getGatewayTypeName = (code) => {
  const item = GatewayTypeList.find(t => t.code === code)
  return item ? item.name : code || '-'
}
