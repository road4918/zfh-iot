package com.zfh.virtualdevice.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zfh.virtualdevice.dto.Result;
import com.zfh.virtualdevice.entity.VirtualMeter;
import com.zfh.virtualdevice.service.VirtualMeterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/meters")
public class VirtualMeterController {

    @Autowired
    private com.zfh.virtualdevice.device.manager.DeviceLifecycleManager lifecycleManager;

    
    @Autowired
    private VirtualMeterService meterService;
    
    @GetMapping
    public Result<Page<VirtualMeter>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<VirtualMeter> page = new Page<>(current, size);
        return Result.success(meterService.page(page));
    }
    
    @GetMapping("/{id}")
    public Result<VirtualMeter> getById(@PathVariable Long id) {
        return Result.success(meterService.getById(id));
    }
    
    @PostMapping
    public Result<Void> save(@RequestBody VirtualMeter meter) {
        if (meterService.isCommunicationAddressExists(meter.getCommunicationAddress())) {
            return Result.error("Communication address already exists");
        }
        
        meter.setConnectionMode("DIRECT");
        meter.setGatewayId(null);
        meter.setStatus(com.zfh.virtualdevice.enums.DeviceStatus.OFFLINE);
        meterService.save(meter);
        return Result.success();
    }
    
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody VirtualMeter meter) {
        VirtualMeter existing = meterService.getById(id);
        if (existing != null && !existing.getCommunicationAddress().equals(meter.getCommunicationAddress())) {
            if (meterService.isCommunicationAddressExists(meter.getCommunicationAddress())) {
                return Result.error("Communication address already exists");
            }
        }
        
        meter.setConnectionMode("DIRECT");
        meter.setGatewayId(null);
        meter.setId(id);
        meterService.updateById(meter);
        return Result.success();
    }
    
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        meterService.removeById(id);
        return Result.success();
    }
    
    @PostMapping("/{id}/start")
    public Result<Void> start(@PathVariable Long id) {
        lifecycleManager.startMeter(id);
        return Result.success();
    }
    
    @PostMapping("/{id}/stop")
    public Result<Void> stop(@PathVariable Long id) {
        lifecycleManager.stopMeter(id);
        return Result.success();
    }
}
