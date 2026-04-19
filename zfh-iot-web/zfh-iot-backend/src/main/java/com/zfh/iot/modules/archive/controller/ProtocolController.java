package com.zfh.iot.modules.archive.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zfh.iot.common.result.PageResult;
import com.zfh.iot.common.result.Result;
import com.zfh.iot.common.utils.PageUtils;
import com.zfh.iot.modules.archive.entity.IotProtocol;
import com.zfh.iot.modules.archive.service.IotProtocolService;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/archive/protocols")
@RequiredArgsConstructor
public class ProtocolController {

    private final IotProtocolService protocolService;

    @GetMapping
    @RequiresPermissions("protocol:list")
    public Result<PageResult<IotProtocol>> list(
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "20") Long size) {
        
        Page<IotProtocol> result = protocolService.page(PageUtils.getPage(page, size));
        return Result.success(PageUtils.convert(result));
    }

    @GetMapping("/{code}")
    @RequiresPermissions("protocol:list")
    public Result<IotProtocol> getByCode(@PathVariable String code) {
        return Result.success(protocolService.getOne(
            new LambdaQueryWrapper<IotProtocol>()
                .eq(IotProtocol::getProtocolCode, code)
        ));
    }

    @PostMapping
    @RequiresPermissions("protocol:create")
    public Result<Void> create(@RequestBody IotProtocol protocol) {
        protocolService.save(protocol);
        return Result.success();
    }

    @PutMapping("/{code}")
    @RequiresPermissions("protocol:update")
    public Result<Void> update(@PathVariable String code, @RequestBody IotProtocol protocol) {
        protocol.setProtocolCode(code);
        protocolService.update(protocol, 
            new LambdaQueryWrapper<IotProtocol>()
                .eq(IotProtocol::getProtocolCode, code));
        return Result.success();
    }

    @DeleteMapping("/{code}")
    @RequiresPermissions("protocol:delete")
    public Result<Void> delete(@PathVariable String code) {
        protocolService.remove(
            new LambdaQueryWrapper<IotProtocol>()
                .eq(IotProtocol::getProtocolCode, code));
        return Result.success();
    }
}
