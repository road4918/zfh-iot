package com.zfh.iot.modules.archive.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("iot_group_meter")
public class IotGroupMeter {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long groupId;
    private Long meterId;
}
