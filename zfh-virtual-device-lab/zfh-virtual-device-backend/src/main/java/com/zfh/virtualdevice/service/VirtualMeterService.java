package com.zfh.virtualdevice.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zfh.virtualdevice.entity.VirtualGateway;
import com.zfh.virtualdevice.entity.VirtualMeter;
import com.zfh.virtualdevice.mapper.VirtualGatewayMapper;
import com.zfh.virtualdevice.mapper.VirtualMeterMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VirtualMeterService extends ServiceImpl<VirtualMeterMapper, VirtualMeter> {
    
    @Autowired
    private VirtualGatewayMapper gatewayMapper;
    
    public boolean isCommunicationAddressExists(String address) {
        VirtualMeter meter = lambdaQuery()
                .eq(VirtualMeter::getCommunicationAddress, address)
                .one();
        if (meter != null) {
            return true;
        }
        VirtualGateway gateway = gatewayMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<VirtualGateway>()
                .eq(VirtualGateway::getCommunicationAddress, address)
        );
        return gateway != null;
    }
}
