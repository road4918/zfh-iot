package com.zfh.iot.modules.data.vo;

import lombok.Data;

@Data
public class CommandStatusStatVO {
    private String date;
    private Long delivered;
    private Long failed;
    private Long success;
    private Long overdue;
    private Long timeout;
    private Long cancelled;
    private Long waiting;
    private Long sent;
}
