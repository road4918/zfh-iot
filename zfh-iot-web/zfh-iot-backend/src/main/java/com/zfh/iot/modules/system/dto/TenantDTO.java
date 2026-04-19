package com.zfh.iot.modules.system.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TenantDTO {
    private Long id;
    
    @NotBlank(message = "Tenant code cannot be empty")
    private String tenantCode;
    
    @NotBlank(message = "Tenant name cannot be empty")
    private String tenantName;
    
    private String contactName;
    private String contactPhone;
    
    @NotNull(message = "Max devices cannot be empty")
    private Integer maxDevices;
    
    @NotNull(message = "Max gateways cannot be empty")
    private Integer maxGateways;
    
    private Integer storageDays;
    private Integer status;
}
