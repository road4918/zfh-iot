package com.zfh.virtualdevice.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zfh.virtualdevice.entity.VirtualGateway;
import com.zfh.virtualdevice.entity.VirtualMeter;
import com.zfh.virtualdevice.mapper.VirtualGatewayMapper;
import com.zfh.virtualdevice.mapper.VirtualMeterMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VirtualGatewayService extends ServiceImpl<VirtualGatewayMapper, VirtualGateway> {
    
    @Autowired
    private VirtualMeterMapper meterMapper;
    
    public boolean isCommunicationAddressExists(String address) {
        VirtualGateway gateway = lambdaQuery()
                .eq(VirtualGateway::getCommunicationAddress, address)
                .one();
        if (gateway != null) {
            return true;
        }
        VirtualMeter meter = meterMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<VirtualMeter>()
                .eq(VirtualMeter::getCommunicationAddress, address)
        );
        return meter != null;
    }
}
