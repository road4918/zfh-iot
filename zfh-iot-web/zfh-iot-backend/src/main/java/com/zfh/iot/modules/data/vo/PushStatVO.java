package com.zfh.iot.modules.data.vo;

import lombok.Data;

@Data
public class PushStatVO {
    private String date;
    private Long total;
    private Long success;
    private Long fail;
}
