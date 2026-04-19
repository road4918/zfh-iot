package com.zfh.iot.modules.data.vo;

import lombok.Data;

@Data
public class OnlineRateVO {
    private String date;
    private Double onlineRate;
    private Double offlineRate;
}
