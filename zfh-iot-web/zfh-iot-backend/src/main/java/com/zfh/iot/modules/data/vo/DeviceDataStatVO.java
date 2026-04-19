package com.zfh.iot.modules.data.vo;

import lombok.Data;

@Data
public class DeviceDataStatVO {
    private String date;
    private Long abnormal;
    private Long offline;
    private Long inactive;
}
