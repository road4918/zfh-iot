package com.zfh.iot.modules.archive.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zfh.iot.common.result.PageResult;
import com.zfh.iot.common.result.Result;
import com.zfh.iot.common.utils.PageUtils;
import com.zfh.iot.modules.archive.entity.IotManufacturer;
import com.zfh.iot.modules.archive.service.IotManufacturerService;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/archive/manufacturers")
@RequiredArgsConstructor
public class ManufacturerController {

    private final IotManufacturerService manufacturerService;

    @GetMapping
    @RequiresPermissions("manufacturer:list")
    public Result<PageResult<IotManufacturer>> list(
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "20") Long size,
            @RequestParam(required = false) String keyword) {
        
        LambdaQueryWrapper<IotManufacturer> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(IotManufacturer::getManufacturerName, keyword);
        }
        
        Page<IotManufacturer> result = manufacturerService.page(PageUtils.getPage(page, size), wrapper);
        return Result.success(PageUtils.convert(result));
    }

    @GetMapping("/{id}")
    @RequiresPermissions("manufacturer:list")
    public Result<IotManufacturer> getById(@PathVariable Long id) {
        return Result.success(manufacturerService.getById(id));
    }

    @PostMapping
    @RequiresPermissions("manufacturer:create")
    public Result<Void> create(@RequestBody IotManufacturer manufacturer) {
        manufacturerService.save(manufacturer);
        return Result.success();
    }

    @PutMapping("/{id}")
    @RequiresPermissions("manufacturer:update")
    public Result<Void> update(@PathVariable Long id, @RequestBody IotManufacturer manufacturer) {
        manufacturer.setId(id);
        manufacturerService.updateById(manufacturer);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequiresPermissions("manufacturer:delete")
    public Result<Void> delete(@PathVariable Long id) {
        manufacturerService.removeById(id);
        return Result.success();
    }
}
