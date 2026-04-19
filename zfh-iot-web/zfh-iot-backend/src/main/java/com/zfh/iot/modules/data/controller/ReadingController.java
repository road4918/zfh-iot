package com.zfh.iot.modules.data.controller;

import com.zfh.iot.common.result.PageResult;
import com.zfh.iot.common.result.Result;
import com.zfh.iot.modules.data.entity.MeterReading;
import com.zfh.iot.modules.data.service.ReadingDataService;
import com.zfh.iot.modules.data.vo.CurrentReadingVO;
import com.zfh.iot.modules.data.vo.ReadingStatisticsVO;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/iot/reading")
@RequiredArgsConstructor
public class ReadingController {

    private final ReadingDataService readingDataService;

    @GetMapping("/current")
    @RequiresPermissions("reading:current")
    public Result<PageResult<CurrentReadingVO>> getCurrentData(
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "20") Long size,
            @RequestParam(required = false) Integer meterType,
            @RequestParam(required = false) Long gatewayId,
            @RequestParam(required = false) Long groupId) {
        
        return Result.success(readingDataService.getCurrentReadings(
                page, size, meterType, gatewayId, groupId));
    }

    @GetMapping("/{meterId}/current-data")
    @RequiresPermissions("meter:list")
    public Result<MeterReading> getMeterCurrentData(@PathVariable Long meterId) {
        return Result.success(readingDataService.getMeterCurrentData(meterId));
    }

    @GetMapping("/{meterId}/history-data")
    @RequiresPermissions("reading:history")
    public Result<PageResult<MeterReading>> getHistoryData(
            @PathVariable Long meterId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "100") Long size) {
        
        return Result.success(readingDataService.getHistoryReadings(
                meterId, startTime, endTime, page, size));
    }

    @GetMapping("/statistics")
    @RequiresPermissions("reading:history")
    public Result<ReadingStatisticsVO> getStatistics(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        
        return Result.success(readingDataService.getReadingStatistics(startDate, endDate));
    }

    @GetMapping("/export")
    @RequiresPermissions("reading:history")
    public void exportData(
            @RequestParam List<Long> meterIds,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(defaultValue = "excel") String format,
            HttpServletResponse response) throws IOException {
        
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=reading_data.xlsx");
        response.getWriter().write("Export not implemented yet");
    }
}
