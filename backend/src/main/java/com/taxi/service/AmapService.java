package com.taxi.service;

import java.util.List;
import java.util.Map;

/**
 * 高德地图服务接口
 */
public interface AmapService {
    
    /**
     * 搜索附近的地点
     * @param keywords 搜索关键词
     * @param latitude 中心点纬度
     * @param longitude 中心点经度
     * @param radius 搜索半径（米）
     * @return 搜索结果列表
     */
    List<Map<String, Object>> searchNearbyPlaces(String keywords, Double latitude, Double longitude, Integer radius);
    
    /**
     * 地理编码（地址转坐标）
     * @param address 地址
     * @return 坐标信息
     */
    Map<String, Object> geocode(String address);
    
    /**
     * 逆地理编码（坐标转地址）
     * @param latitude 纬度
     * @param longitude 经度
     * @return 地址信息
     */
    Map<String, Object> reverseGeocode(Double latitude, Double longitude);
    
    /**
     * 路径规划
     * @param origin 起点坐标
     * @param destination 终点坐标
     * @return 路径信息
     */
    Map<String, Object> routePlanning(String origin, String destination);
} 