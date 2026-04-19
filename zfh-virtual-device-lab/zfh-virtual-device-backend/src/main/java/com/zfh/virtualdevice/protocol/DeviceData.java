package com.zfh.virtualdevice.protocol;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class DeviceData {
    private Long deviceId;
    private String deviceAddress;
    private com.zfh.virtualdevice.enums.DeviceType deviceType;
    private LocalDateTime timestamp;
    private Map<String, Object> data;
    private Map<String, String> units;
}
