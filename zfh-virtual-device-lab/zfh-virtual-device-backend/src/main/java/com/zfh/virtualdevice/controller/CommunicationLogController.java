package com.zfh.virtualdevice.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zfh.virtualdevice.dto.Result;
import com.zfh.virtualdevice.entity.CommunicationLog;
import com.zfh.virtualdevice.service.CommunicationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/logs")
public class CommunicationLogController {
    
    @Autowired
    private CommunicationLogService logService;
    
    @GetMapping
    public Result<Page<CommunicationLog>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String deviceType,
            @RequestParam(required = false) Long deviceId,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) String protocol) {
        
        Page<CommunicationLog> page = new Page<>(current, size);
        com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper<CommunicationLog> query = logService.lambdaQuery();
        
        if (deviceType != null) {
            query.eq(CommunicationLog::getDeviceType, deviceType);
        }
        if (deviceId != null) {
            query.eq(CommunicationLog::getDeviceId, deviceId);
        }
        if (direction != null) {
            query.eq(CommunicationLog::getDirection, direction);
        }
        if (protocol != null) {
            query.eq(CommunicationLog::getProtocol, protocol);
        }
        
        query.orderByDesc(CommunicationLog::getTimestamp);
        return Result.success(logService.page(page, query.getWrapper()));
    }
    
    @DeleteMapping
    public Result<Void> clear() {
        logService.remove(null);
        return Result.success();
    }
}
