package com.zfh.virtualdevice.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zfh.virtualdevice.entity.CommunicationLog;
import com.zfh.virtualdevice.mapper.CommunicationLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CommunicationLogService extends ServiceImpl<CommunicationLogMapper, CommunicationLog> {
    
    @Async
    public void logAsync(CommunicationLog commLog) {
        try {
            save(commLog);
        } catch (Exception e) {
            log.error("Failed to save communication log", e);
        }
    }
}
