package com.zfh.iot.modules.data.vo;

import lombok.Data;

@Data
public class TrendDataVO {
    private String date;
    private Long total;
    private Long online;
}
