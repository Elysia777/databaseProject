package com.taxi.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxi.service.AmapService;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 高德地图服务实现类
 */
@Slf4j
@Service
public class AmapServiceImpl implements AmapService {

    @Value("${amap.key}")
    private String amapKey;

    @Value("${amap.base-url}")
    private String baseUrl;

    @Value("${amap.search.radius}")
    private Integer defaultRadius;

    @Value("${amap.search.page-size}")
    private Integer pageSize;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<Map<String, Object>> searchNearbyPlaces(String keywords, Double latitude, Double longitude, Integer radius) {
        try {
            String url = String.format("%s/place/around?key=%s&keywords=%s&location=%s,%s&radius=%d&page_size=%d&output=json",
                    baseUrl,
                    amapKey,
                    URLEncoder.encode(keywords, StandardCharsets.UTF_8),
                    longitude, latitude,
                    radius != null ? radius : defaultRadius,
                    pageSize
            );

            String response = sendGetRequest(url);
            JsonNode rootNode = objectMapper.readTree(response);

            if ("1".equals(rootNode.path("status").asText())) {
                List<Map<String, Object>> results = new ArrayList<>();
                JsonNode pois = rootNode.path("pois");
                
                for (JsonNode poi : pois) {
                    Map<String, Object> place = new HashMap<>();
                    place.put("id", poi.path("id").asText());
                    place.put("name", poi.path("name").asText());
                    place.put("address", poi.path("address").asText());
                    place.put("distance", poi.path("distance").asText());
                    place.put("type", poi.path("type").asText());
                    place.put("location", poi.path("location").asText());
                    results.add(place);
                }
                return results;
            } else {
                log.error("高德地图API调用失败: {}", rootNode.path("info").asText());
                return new ArrayList<>();
            }
        } catch (Exception e) {
            log.error("搜索附近地点失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    public Map<String, Object> geocode(String address) {
        try {
            String url = String.format("%s/geocode/geo?key=%s&address=%s&output=json",
                    baseUrl,
                    amapKey,
                    URLEncoder.encode(address, StandardCharsets.UTF_8)
            );

            String response = sendGetRequest(url);
            JsonNode rootNode = objectMapper.readTree(response);

            if ("1".equals(rootNode.path("status").asText())) {
                JsonNode geocodes = rootNode.path("geocodes");
                if (geocodes.isArray() && geocodes.size() > 0) {
                    JsonNode firstResult = geocodes.get(0);
                    Map<String, Object> result = new HashMap<>();
                    result.put("location", firstResult.path("location").asText());
                    result.put("formatted_address", firstResult.path("formatted_address").asText());
                    result.put("country", firstResult.path("country").asText());
                    result.put("province", firstResult.path("province").asText());
                    result.put("city", firstResult.path("city").asText());
                    result.put("district", firstResult.path("district").asText());
                    return result;
                }
            } else {
                log.error("地理编码失败: {}", rootNode.path("info").asText());
            }
        } catch (Exception e) {
            log.error("地理编码失败", e);
        }
        return new HashMap<>();
    }

    @Override
    public Map<String, Object> reverseGeocode(Double latitude, Double longitude) {
        try {
            String url = String.format("%s/geocode/regeo?key=%s&location=%s,%s&output=json",
                    baseUrl,
                    amapKey,
                    longitude, latitude
            );

            String response = sendGetRequest(url);
            JsonNode rootNode = objectMapper.readTree(response);

            if ("1".equals(rootNode.path("status").asText())) {
                JsonNode regeocode = rootNode.path("regeocode");
                Map<String, Object> result = new HashMap<>();
                result.put("formatted_address", regeocode.path("formatted_address").asText());
                result.put("addressComponent", regeocode.path("addressComponent"));
                return result;
            } else {
                log.error("逆地理编码失败: {}", rootNode.path("info").asText());
            }
        } catch (Exception e) {
            log.error("逆地理编码失败", e);
        }
        return new HashMap<>();
    }

    @Override
    public Map<String, Object> routePlanning(String origin, String destination) {
        try {
            String url = String.format("%s/direction/driving?key=%s&origin=%s&destination=%s&output=json",
                    baseUrl,
                    amapKey,
                    origin,
                    destination
            );

            String response = sendGetRequest(url);
            JsonNode rootNode = objectMapper.readTree(response);

            if ("1".equals(rootNode.path("status").asText())) {
                JsonNode route = rootNode.path("route");
                JsonNode paths = route.path("paths");
                if (paths.isArray() && paths.size() > 0) {
                    JsonNode firstPath = paths.get(0);
                    Map<String, Object> result = new HashMap<>();
                    result.put("distance", firstPath.path("distance").asText());
                    result.put("duration", firstPath.path("duration").asText());
                    result.put("tolls", firstPath.path("tolls").asText());
                    result.put("steps", firstPath.path("steps"));
                    return result;
                }
            } else {
                log.error("路径规划失败: {}", rootNode.path("info").asText());
            }
        } catch (Exception e) {
            log.error("路径规划失败", e);
        }
        return new HashMap<>();
    }

    private String sendGetRequest(String url) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(url);
            return EntityUtils.toString(httpClient.execute(httpGet).getEntity(), StandardCharsets.UTF_8);
        }
    }
} 