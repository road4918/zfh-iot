## Chunk 2: Mapper Layer & Basic CRUD APIs

### Task 3: Create MyBatis Mappers

**Files:**
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/mapper/VirtualGatewayMapper.java`
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/mapper/VirtualMeterMapper.java`
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/mapper/CommunicationLogMapper.java`
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/mapper/MeterDataConfigMapper.java`

- [ ] **Step 1: Create mapper interfaces**

```java
package com.zfh.virtualdevice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zfh.virtualdevice.entity.VirtualGateway;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface VirtualGatewayMapper extends BaseMapper<VirtualGateway> {
}
```

```java
package com.zfh.virtualdevice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zfh.virtualdevice.entity.VirtualMeter;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface VirtualMeterMapper extends BaseMapper<VirtualMeter> {
}
```

```java
package com.zfh.virtualdevice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zfh.virtualdevice.entity.CommunicationLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommunicationLogMapper extends BaseMapper<CommunicationLog> {
}
```

```java
package com.zfh.virtualdevice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zfh.virtualdevice.entity.MeterDataConfig;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MeterDataConfigMapper extends BaseMapper<MeterDataConfig> {
}
```

- [ ] **Step 2: Commit**

```bash
git add zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/mapper/
git commit -m "feat: add MyBatis mappers"
```

---

### Task 4: Gateway CRUD API

**Files:**
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/dto/Result.java`
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/exception/GlobalExceptionHandler.java`
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/controller/VirtualGatewayController.java`
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/service/VirtualGatewayService.java`

- [ ] **Step 1: Create unified response DTO**

```java
package com.zfh.virtualdevice.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Result<T> {
    private Integer code;
    private String message;
    private T data;
    private LocalDateTime timestamp;
    
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("success");
        result.setData(data);
        result.setTimestamp(LocalDateTime.now());
        return result;
    }
    
    public static <T> Result<T> success() {
        return success(null);
    }
    
    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMessage(message);
        result.setTimestamp(LocalDateTime.now());
        return result;
    }
    
    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        result.setTimestamp(LocalDateTime.now());
        return result;
    }
}
```

- [ ] **Step 2: Create global exception handler**

Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/exception/GlobalExceptionHandler.java`

```java
package com.zfh.virtualdevice.exception;

import com.zfh.virtualdevice.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return Result.error("Internal server error: " + e.getMessage());
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgument(IllegalArgumentException e) {
        return Result.error(400, e.getMessage());
    }
}
```

- [ ] **Step 3: Create Gateway Service**

```java
package com.zfh.virtualdevice.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zfh.virtualdevice.entity.VirtualGateway;
import com.zfh.virtualdevice.entity.VirtualMeter;
import com.zfh.virtualdevice.mapper.VirtualGatewayMapper;
import com.zfh.virtualdevice.mapper.VirtualMeterMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VirtualGatewayService extends ServiceImpl<VirtualGatewayMapper, VirtualGateway> {
    
    @Autowired
    private VirtualMeterMapper meterMapper;
    
    public boolean isCommunicationAddressExists(String address) {
        // Check in gateways
        VirtualGateway gateway = lambdaQuery()
                .eq(VirtualGateway::getCommunicationAddress, address)
                .one();
        if (gateway != null) {
            return true;
        }
        // Check in meters
        VirtualMeter meter = meterMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<VirtualMeter>()
                .eq(VirtualMeter::getCommunicationAddress, address)
        );
        return meter != null;
    }
}
```

- [ ] **Step 4: Create Gateway Controller**

```java
package com.zfh.virtualdevice.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zfh.virtualdevice.dto.Result;
import com.zfh.virtualdevice.entity.VirtualGateway;
import com.zfh.virtualdevice.service.VirtualGatewayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/gateways")
public class VirtualGatewayController {
    
    @Autowired
    private VirtualGatewayService gatewayService;
    
    @GetMapping
    public Result<Page<VirtualGateway>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<VirtualGateway> page = new Page<>(current, size);
        return Result.success(gatewayService.page(page));
    }
    
    @GetMapping("/{id}")
    public Result<VirtualGateway> getById(@PathVariable Long id) {
        return Result.success(gatewayService.getById(id));
    }
    
    @PostMapping
    public Result<Void> save(@RequestBody VirtualGateway gateway) {
        // Check duplicate communication address globally (across gateways and meters)
        if (gatewayService.isCommunicationAddressExists(gateway.getCommunicationAddress())) {
            return Result.error("Communication address already exists");
        }
        
        gateway.setStatus(com.zfh.virtualdevice.enums.DeviceStatus.OFFLINE);
        gatewayService.save(gateway);
        return Result.success();
    }
    
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody VirtualGateway gateway) {
        // Check duplicate communication address if changed
        VirtualGateway existing = gatewayService.getById(id);
        if (existing != null && !existing.getCommunicationAddress().equals(gateway.getCommunicationAddress())) {
            if (gatewayService.isCommunicationAddressExists(gateway.getCommunicationAddress())) {
                return Result.error("Communication address already exists");
            }
        }
        
        gateway.setId(id);
        gatewayService.updateById(gateway);
        return Result.success();
    }
    
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        gatewayService.removeById(id);
        return Result.success();
    }
}
```

- [ ] **Step 5: Test Gateway API**

Run application:
```bash
cd zfh-virtual-device-backend
mvn spring-boot:run
```

Test create:
```bash
curl -X POST http://localhost:8080/api/gateways \
  -H "Content-Type: application/json" \
  -d '{
    "name": "测试网关",
    "communicationAddress": "GW001",
    "protocol": "MQTT",
    "commMode": "CLIENT",
    "clientHost": "localhost",
    "clientPort": 1883,
    "mqttBroker": "tcp://localhost:1883",
    "mqttClientId": "gw-001"
  }'
```
Expected: `{"code":200,"message":"success","timestamp":"..."}`

Test list:
```bash
curl http://localhost:8080/api/gateways
```
Expected: List with created gateway

Test getById:
```bash
curl http://localhost:8080/api/gateways/1
```
Expected: Gateway details with status OFFLINE

Test update:
```bash
curl -X PUT http://localhost:8080/api/gateways/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"Updated Gateway","communicationAddress":"GW001"}'
```
Expected: `{"code":200,"message":"success"}`

Test delete:
```bash
curl -X DELETE http://localhost:8080/api/gateways/1
```
Expected: `{"code":200,"message":"success"}`

- [ ] **Step 6: Commit**

```bash
git add zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/dto/
git add zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/controller/
git add zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/service/
git commit -m "feat: add gateway CRUD API"
```

---

### Task 5: Meter CRUD API

**Files:**
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/controller/VirtualMeterController.java`
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/service/VirtualMeterService.java`

- [ ] **Step 1: Create Meter Service**

```java
package com.zfh.virtualdevice.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zfh.virtualdevice.entity.VirtualGateway;
import com.zfh.virtualdevice.entity.VirtualMeter;
import com.zfh.virtualdevice.mapper.VirtualGatewayMapper;
import com.zfh.virtualdevice.mapper.VirtualMeterMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VirtualMeterService extends ServiceImpl<VirtualMeterMapper, VirtualMeter> {
    
    @Autowired
    private VirtualGatewayMapper gatewayMapper;
    
    public boolean isCommunicationAddressExists(String address) {
        // Check in meters
        VirtualMeter meter = lambdaQuery()
                .eq(VirtualMeter::getCommunicationAddress, address)
                .one();
        if (meter != null) {
            return true;
        }
        // Check in gateways
        VirtualGateway gateway = gatewayMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<VirtualGateway>()
                .eq(VirtualGateway::getCommunicationAddress, address)
        );
        return gateway != null;
    }
}
```

- [ ] **Step 2: Create Meter Controller**

```java
package com.zfh.virtualdevice.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zfh.virtualdevice.dto.Result;
import com.zfh.virtualdevice.entity.VirtualMeter;
import com.zfh.virtualdevice.service.VirtualMeterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/meters")
public class VirtualMeterController {
    
    @Autowired
    private VirtualMeterService meterService;
    
    @GetMapping
    public Result<Page<VirtualMeter>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<VirtualMeter> page = new Page<>(current, size);
        return Result.success(meterService.page(page));
    }
    
    @GetMapping("/{id}")
    public Result<VirtualMeter> getById(@PathVariable Long id) {
        return Result.success(meterService.getById(id));
    }
    
    @PostMapping
    public Result<Void> save(@RequestBody VirtualMeter meter) {
        // Check duplicate communication address globally (across gateways and meters)
        if (meterService.isCommunicationAddressExists(meter.getCommunicationAddress())) {
            return Result.error("Communication address already exists");
        }
        
        // MVP: enforce DIRECT mode only
        meter.setConnectionMode("DIRECT");
        meter.setGatewayId(null);
        
        meter.setStatus(com.zfh.virtualdevice.enums.DeviceStatus.OFFLINE);
        meterService.save(meter);
        return Result.success();
    }
    
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody VirtualMeter meter) {
        // Check duplicate communication address if changed
        VirtualMeter existing = meterService.getById(id);
        if (existing != null && !existing.getCommunicationAddress().equals(meter.getCommunicationAddress())) {
            if (meterService.isCommunicationAddressExists(meter.getCommunicationAddress())) {
                return Result.error("Communication address already exists");
            }
        }
        
        // MVP: enforce DIRECT mode only
        meter.setConnectionMode("DIRECT");
        meter.setGatewayId(null);
        
        meter.setId(id);
        meterService.updateById(meter);
        return Result.success();
    }
    
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        meterService.removeById(id);
        return Result.success();
    }
}
```

- [ ] **Step 3: Test Meter API**

Test create:
```bash
curl -X POST http://localhost:8080/api/meters \
  -H "Content-Type: application/json" \
  -d '{
    "name": "测试电表",
    "meterType": "ELECTRIC",
    "communicationAddress": "METER001",
    "protocol": "MQTT",
    "autoReport": true,
    "reportInterval": 30
  }'
```
Expected: `{"code":200,"message":"success","timestamp":"..."}`

Test list:
```bash
curl http://localhost:8080/api/meters
```
Expected: List with created meter, `connectionMode` should be "DIRECT"

Test duplicate address (should fail - same meter type):
```bash
curl -X POST http://localhost:8080/api/meters \
  -H "Content-Type: application/json" \
  -d '{
    "name": "重复地址测试",
    "meterType": "ELECTRIC",
    "communicationAddress": "METER001",
    "protocol": "MQTT"
  }'
```
Expected: `{"code":500,"message":"Communication address already exists"}`

Test cross-entity duplicate address (should fail - gateway address used for meter):
```bash
# First create a gateway
curl -X POST http://localhost:8080/api/gateways \
  -H "Content-Type: application/json" \
  -d '{
    "name": "冲突网关",
    "communicationAddress": "CONFLICT001",
    "protocol": "MQTT",
    "commMode": "CLIENT",
    "mqttBroker": "tcp://localhost:1883",
    "mqttClientId": "conflict-001"
  }'

# Then try to create meter with same address
curl -X POST http://localhost:8080/api/meters \
  -H "Content-Type: application/json" \
  -d '{
    "name": "冲突测试",
    "meterType": "ELECTRIC",
    "communicationAddress": "CONFLICT001",
    "protocol": "MQTT"
  }'
```
Expected: `{"code":500,"message":"Communication address already exists"}`

- [ ] **Step 4: Commit**

```bash
git add zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/controller/VirtualMeterController.java
git add zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/service/VirtualMeterService.java
git commit -m "feat: add meter CRUD API"
```

---

