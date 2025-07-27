package com.taxi.controller;

import com.taxi.common.Result;
import com.taxi.dto.CreateOrderRequest;
import com.taxi.entity.Order;
import com.taxi.entity.Driver;
import com.taxi.mapper.OrderMapper;
import com.taxi.mapper.DriverMapper;
import com.taxi.service.OrderService;
import com.taxi.service.DriverRedisService;
import com.taxi.service.WebSocketNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private DriverMapper driverMapper;

    @Autowired
    private DriverRedisService driverRedisService;

    @Autowired
    private WebSocketNotificationService webSocketNotificationService;

    @PostMapping("/create")
    public Result<String> createOrder(@RequestBody CreateOrderRequest request) {
        System.out.println("=== 订单创建请求到达控制器 ===");
        System.out.println("请求参数: " + request);
        
        try {
            Order order = new Order();
            order.setOrderNumber(generateOrderNumber());
            order.setPassengerId(request.getPassengerId());
            order.setPickupAddress(request.getPickupAddress());
            order.setPickupLatitude(request.getPickupLatitude());
            order.setPickupLongitude(request.getPickupLongitude());
            
            order.setDestinationAddress(request.getDestinationAddress());
            order.setDestinationLatitude(request.getDestinationLatitude());
            order.setDestinationLongitude(request.getDestinationLongitude());
            
            order.setOrderType("REAL_TIME");
            order.setCreatedAt(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());

            System.out.println("准备调用 orderService.createOrder");
            orderService.createOrder(order);
            System.out.println("orderService.createOrder 调用成功");
            
            return Result.success(order.getOrderNumber());
        } catch (Exception e) {
            System.err.println("订单创建异常: " + e.getMessage());
            e.printStackTrace();
            return Result.error("创建订单失败: " + e.getMessage());
        }
    }

    /** 司机接单 */
    @PostMapping("/{orderId}/accept")
    public Result<String> acceptOrder(@PathVariable Long orderId, @RequestParam Long driverId) {
        try {
            // 查询订单
            Order order = orderMapper.selectById(orderId);
            if (order == null) {
                return Result.error("订单不存在");
            }
            
            if (!"PENDING".equals(order.getStatus())) {
                return Result.error("订单状态不允许接单，当前状态: " + order.getStatus());
            }
            
            // 查询司机
            Driver driver = driverMapper.selectById(driverId);
            if (driver == null) {
                return Result.error("司机不存在");
            }
            
            if (!driver.getIsOnline()) {
                return Result.error("司机不在线");
            }
            
            // 检查司机是否已有进行中的订单
            List<Order> driverOrders = orderMapper.selectByDriverId(driverId);
            boolean hasActiveOrder = driverOrders.stream()
                .anyMatch(o -> "ASSIGNED".equals(o.getStatus()) || "PICKUP".equals(o.getStatus()) || "IN_PROGRESS".equals(o.getStatus()));
            
            if (hasActiveOrder) {
                return Result.error("司机正在处理其他订单");
            }
            
            // 分配订单给司机
            order.setDriverId(driverId);
            order.setStatus("ASSIGNED");
            orderMapper.updateById(order);
            
            return Result.success("接单成功");
        } catch (Exception e) {
            return Result.error("接单失败: " + e.getMessage());
        }
    }

    /** 获取所有订单列表 */
    @GetMapping
    public Result<List<Order>> getAllOrders() {
        try {
            List<Order> orders = orderMapper.selectAll();
            return Result.success(orders);
        } catch (Exception e) {
            return Result.error("获取订单列表失败: " + e.getMessage());
        }
    }

    /** 获取所有订单列表 - 兼容 /all 路径 */
    @GetMapping("/all")
    public Result<List<Order>> getAllOrdersCompat() {
        return getAllOrders();
    }

    /** 根据ID获取订单详情 */
    @GetMapping("/{orderId}")
    public Result<Order> getOrderById(@PathVariable Long orderId) {
        try {
            Order order = orderMapper.selectById(orderId);
            if (order == null) {
                return Result.error("订单不存在");
            }
            return Result.success(order);
        } catch (Exception e) {
            return Result.error("获取订单详情失败: " + e.getMessage());
        }
    }

    /** 取消订单 */
    @PostMapping("/{orderId}/cancel")
    public Result<String> cancelOrder(@PathVariable Long orderId) {
        try {
            Order order = orderMapper.selectById(orderId);
            if (order == null) {
                return Result.error("订单不存在");
            }
            
            if ("COMPLETED".equals(order.getStatus()) || "CANCELLED".equals(order.getStatus())) {
                return Result.error("订单已完成或已取消，无法取消");
            }
            
            order.setStatus("CANCELLED");
            order.setUpdatedAt(LocalDateTime.now());
            orderMapper.updateById(order);
            
            return Result.success("订单取消成功");
        } catch (Exception e) {
            return Result.error("取消订单失败: " + e.getMessage());
        }
    }

    /** 司机确认到达上车点 */
    @PostMapping("/{orderId}/pickup")
    public Result<String> confirmPickup(@PathVariable Long orderId) {
        try {
            Order order = orderMapper.selectById(orderId);
            if (order == null) {
                return Result.error("订单不存在");
            }
            
            if (!"ASSIGNED".equals(order.getStatus())) {
                return Result.error("订单状态不正确，当前状态: " + order.getStatus());
            }
            
            order.setStatus("PICKUP");
            order.setPickupTime(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());
            orderMapper.updateById(order);
            
            // 推送确认到达状态给乘客
            if (order.getPassengerId() != null) {
                webSocketNotificationService.notifyPassengerOrderStatusChange(
                    order.getPassengerId(), 
                    orderId, 
                    "PICKUP", 
                    "司机已到达上车点，请准备上车"
                );
            }
            
            return Result.success("确认到达成功");
        } catch (Exception e) {
            return Result.error("确认到达失败: " + e.getMessage());
        }
    }

    /** 司机开始行程 */
    @PostMapping("/{orderId}/start")
    public Result<String> startTrip(@PathVariable Long orderId) {
        try {
            Order order = orderMapper.selectById(orderId);
            if (order == null) {
                return Result.error("订单不存在");
            }
            
            if (!"PICKUP".equals(order.getStatus())) {
                return Result.error("订单状态不正确，当前状态: " + order.getStatus());
            }
            
            order.setStatus("IN_PROGRESS");
            order.setUpdatedAt(LocalDateTime.now());
            orderMapper.updateById(order);
            
            // 推送行程开始状态给乘客
            if (order.getPassengerId() != null) {
                webSocketNotificationService.notifyPassengerOrderStatusChange(
                    order.getPassengerId(), 
                    orderId, 
                    "IN_PROGRESS", 
                    "行程已开始，请系好安全带"
                );
            }
            
            return Result.success("行程开始成功");
        } catch (Exception e) {
            return Result.error("开始行程失败: " + e.getMessage());
        }
    }

    /** 司机完成订单 */
    @PostMapping("/{orderId}/complete")
    public Result<String> completeTrip(@PathVariable Long orderId) {
        try {
            Order order = orderMapper.selectById(orderId);
            if (order == null) {
                return Result.error("订单不存在");
            }
            
            if (!"IN_PROGRESS".equals(order.getStatus())) {
                return Result.error("订单状态不正确，当前状态: " + order.getStatus());
            }
            
            order.setStatus("COMPLETED");
            order.setCompletionTime(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());
            
            // 计算实际费用（这里简化处理，使用预估费用）
            if (order.getActualFare() == null) {
                order.setActualFare(order.getEstimatedFare());
            }
            
            orderMapper.updateById(order);
            
            // 标记司机为空闲状态
            if (order.getDriverId() != null) {
                driverRedisService.markDriverFree(order.getDriverId());
            }
            
            // 推送订单完成状态给乘客
            if (order.getPassengerId() != null) {
                webSocketNotificationService.notifyPassengerOrderStatusChange(
                    order.getPassengerId(), 
                    orderId, 
                    "COMPLETED", 
                    "行程已完成，感谢您的使用"
                );
            }
            
            return Result.success("订单完成成功");
        } catch (Exception e) {
            return Result.error("完成订单失败: " + e.getMessage());
        }
    }

    /** 获取待接单的订单列表 */
    @GetMapping("/pending")
    public Result<List<Order>> getPendingOrders() {
        try {
            List<Order> orders = orderMapper.selectByStatus("PENDING");
            return Result.success(orders);
        } catch (Exception e) {
            return Result.error("获取订单列表失败: " + e.getMessage());
        }
    }

    private String generateOrderNumber() {
        return "ORDER" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8);
    }
} 