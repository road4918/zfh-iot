package com.zfh.virtualdevice.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zfh.virtualdevice.enums.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("template_data_item")
public class TemplateDataItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long templateId;
    private String dataType;
    
    @EnumValue
    private DataCategory dataCategory;
    
    private BigDecimal initialValue;
    private BigDecimal incrementMin;
    private BigDecimal incrementMax;
    private BigDecimal minValue;
    private BigDecimal maxValue;
    private String fluctuationType;
    private BigDecimal ratioMin;
    private BigDecimal ratioMax;
    private String unit;
    private Integer sortOrder;
}
