package com.zfh.iot.modules.data.vo;

import lombok.Data;

@Data
public class DailyStatVO {
    private String date;
    private Long shouldRead;
    private Long actualRead;
    private Double rate;
}
