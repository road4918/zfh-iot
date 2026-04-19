package com.zfh.virtualdevice.device.simulation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zfh.virtualdevice.entity.MeterDataConfig;
import com.zfh.virtualdevice.enums.DataCategory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Random;

public class DataGenerator {
    
    private static final Random random = new Random();
    private static final ObjectMapper mapper = new ObjectMapper();
    
    public static BigDecimal generateNextValue(MeterDataConfig config) {
        switch (config.getDataCategory()) {
            case ACCUMULATING:
                return generateAccumulating(config);
            case FLUCTUATING:
                return generateFluctuating(config);
            case RATIO:
                return generateRatio(config);
            default:
                return config.getCurrentValue();
        }
    }
    
    private static BigDecimal generateAccumulating(MeterDataConfig config) {
        BigDecimal current = config.getCurrentValue();
        BigDecimal min = parseParam(config, "incrementMin", BigDecimal.valueOf(0.01));
        BigDecimal max = parseParam(config, "incrementMax", BigDecimal.valueOf(0.05));
        
        BigDecimal increment = randomBetween(min, max);
        return current.add(increment).setScale(4, RoundingMode.HALF_UP);
    }
    
    private static BigDecimal generateFluctuating(MeterDataConfig config) {
        BigDecimal min = parseParam(config, "minValue", BigDecimal.valueOf(0));
        BigDecimal max = parseParam(config, "maxValue", BigDecimal.valueOf(100));
        
        return randomBetween(min, max).setScale(4, RoundingMode.HALF_UP);
    }
    
    private static BigDecimal generateRatio(MeterDataConfig config) {
        BigDecimal min = parseParam(config, "ratioMin", BigDecimal.ZERO);
        BigDecimal max = parseParam(config, "ratioMax", BigDecimal.ONE);
        
        return randomBetween(min, max).setScale(4, RoundingMode.HALF_UP);
    }
    
    private static BigDecimal randomBetween(BigDecimal min, BigDecimal max) {
        BigDecimal range = max.subtract(min);
        BigDecimal randomValue = range.multiply(BigDecimal.valueOf(random.nextDouble()));
        return min.add(randomValue);
    }
    
    private static BigDecimal parseParam(MeterDataConfig config, String key, BigDecimal defaultValue) {
        try {
            // First check override_params (priority)
            String overrideParams = config.getOverrideParams();
            if (overrideParams != null && !overrideParams.isEmpty()) {
                Map<String, Object> overrideMap = mapper.readValue(overrideParams, new TypeReference<Map<String, Object>>() {});
                Object value = overrideMap.get(key);
                if (value != null) {
                    return new BigDecimal(value.toString());
                }
            }
            
            // Then check config_params
            String params = config.getConfigParams();
            if (params != null && !params.isEmpty()) {
                Map<String, Object> paramMap = mapper.readValue(params, new TypeReference<Map<String, Object>>() {});
                Object value = paramMap.get(key);
                if (value != null) {
                    return new BigDecimal(value.toString());
                }
            }
        } catch (Exception e) {
            // Ignore parsing errors, use default
        }
        return defaultValue;
    }
}
