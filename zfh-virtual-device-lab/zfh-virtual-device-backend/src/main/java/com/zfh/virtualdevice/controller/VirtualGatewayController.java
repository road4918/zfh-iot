package com.zfh.virtualdevice.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zfh.virtualdevice.dto.Result;
import com.zfh.virtualdevice.entity.VirtualGateway;
import com.zfh.virtualdevice.service.VirtualGatewayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/gateways")
public class VirtualGatewayController {

    @Autowired
    private com.zfh.virtualdevice.device.manager.DeviceLifecycleManager lifecycleManager;

    
    @Autowired
    private VirtualGatewayService gatewayService;
    
    @GetMapping
    public Result<Page<VirtualGateway>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<VirtualGateway> page = new Page<>(current, size);
        return Result.success(gatewayService.page(page));
    }
    
    @GetMapping("/{id}")
    public Result<VirtualGateway> getById(@PathVariable Long id) {
        return Result.success(gatewayService.getById(id));
    }
    
    @PostMapping
    public Result<Void> save(@RequestBody VirtualGateway gateway) {
        if (gatewayService.isCommunicationAddressExists(gateway.getCommunicationAddress())) {
            return Result.error("Communication address already exists");
        }
        
        gateway.setStatus(com.zfh.virtualdevice.enums.DeviceStatus.OFFLINE);
        gatewayService.save(gateway);
        return Result.success();
    }
    
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody VirtualGateway gateway) {
        VirtualGateway existing = gatewayService.getById(id);
        if (existing != null && !existing.getCommunicationAddress().equals(gateway.getCommunicationAddress())) {
            if (gatewayService.isCommunicationAddressExists(gateway.getCommunicationAddress())) {
                return Result.error("Communication address already exists");
            }
        }
        
        gateway.setId(id);
        gatewayService.updateById(gateway);
        return Result.success();
    }
    
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        gatewayService.removeById(id);
        return Result.success();
    }
    
    @PostMapping("/{id}/start")
    public Result<Void> start(@PathVariable Long id) {
        lifecycleManager.startGateway(id);
        return Result.success();
    }
    
    @PostMapping("/{id}/stop")
    public Result<Void> stop(@PathVariable Long id) {
        lifecycleManager.stopGateway(id);
        return Result.success();
    }
}
