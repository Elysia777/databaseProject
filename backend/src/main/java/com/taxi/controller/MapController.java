package com.taxi.controller;

import com.taxi.common.Result;
import com.taxi.service.AmapService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 地图控制器
 */
@RestController
@RequestMapping("/map")
@RequiredArgsConstructor
public class MapController {

    private final AmapService amapService;

    /**
     * 搜索附近的地点
     */
    @GetMapping("/search/nearby")
    public Result<List<Map<String, Object>>> searchNearbyPlaces(
            @RequestParam String keywords,
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(required = false, defaultValue = "3000") Integer radius) {
        
        List<Map<String, Object>> places = amapService.searchNearbyPlaces(keywords, latitude, longitude, radius);
        return Result.success(places);
    }

    /**
     * 地理编码（地址转坐标）
     */
    @GetMapping("/geocode")
    public Result<Map<String, Object>> geocode(@RequestParam String address) {
        Map<String, Object> result = amapService.geocode(address);
        return Result.success(result);
    }

    /**
     * 逆地理编码（坐标转地址）
     */
    @GetMapping("/reverse-geocode")
    public Result<Map<String, Object>> reverseGeocode(
            @RequestParam Double latitude,
            @RequestParam Double longitude) {
        
        Map<String, Object> result = amapService.reverseGeocode(latitude, longitude);
        return Result.success(result);
    }

    /**
     * 路径规划
     */
    @GetMapping("/route")
    public Result<Map<String, Object>> routePlanning(
            @RequestParam String origin,
            @RequestParam String destination) {
        
        Map<String, Object> result = amapService.routePlanning(origin, destination);
        return Result.success(result);
    }
} 