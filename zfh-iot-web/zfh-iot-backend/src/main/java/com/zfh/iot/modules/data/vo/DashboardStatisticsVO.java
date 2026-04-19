package com.zfh.iot.modules.data.vo;

import lombok.Data;

@Data
public class DashboardStatisticsVO {
    private DeviceStat devices;
    private MessageCommandStat messages;

    @Data
    public static class DeviceStat {
        private long total;
        private long online;
        private long offline;
        private long abnormal;
        private long inactive;
    }

    @Data
    public static class MessageCommandStat {
        private long messageCount;
        private long commandCount;
    }
}
