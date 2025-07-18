 package com.taxi.controller;

import com.taxi.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试控制器
 */
@RestController
@RequestMapping("/test")
public class TestController {

    /**
     * 测试接口
     */
    @GetMapping("/hello")
    public Result<Map<String, Object>> hello() {
        Map<String, Object> data = new HashMap<>();
        data.put("message", "后端服务运行正常");
        data.put("timestamp", System.currentTimeMillis());
        return Result.success(data);
    }

    /**
     * 测试地图API
     */
    @GetMapping("/map")
    public Result<Map<String, Object>> testMap() {
        Map<String, Object> data = new HashMap<>();
        data.put("message", "地图API测试");
        data.put("status", "available");
        return Result.success(data);
    }
}