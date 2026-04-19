package com.zfh.virtualdevice.protocol;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class Command {
    private CommandType type;
    private String dataId;
    private Map<String, Object> params;
    private LocalDateTime timestamp;
    private String rawFrame;
}
