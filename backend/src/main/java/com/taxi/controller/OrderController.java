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
import com.taxi.service.OrderDispatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;

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

    @Autowired
    private OrderDispatchService orderDispatchService;

    @PostMapping("/create")
    public Result<Long> createOrder(@RequestBody CreateOrderRequest request) {
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
            
            // 返回订单ID而不是订单号，前端需要用ID来进行后续操作
            return Result.success(order.getId());
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

    /** 乘客取消订单 */
    @PostMapping("/{orderId}/cancel")
    public Result<String> cancelOrderByPassenger(@PathVariable Long orderId) {
        try {
            System.out.println("=== 乘客取消订单请求 ===");
            System.out.println("订单ID: " + orderId);
            
            Order order = orderMapper.selectById(orderId);
            if (order == null) {
                System.out.println("❌ 订单不存在: " + orderId);
                return Result.error("订单不存在");
            }
            
            System.out.println("✅ 找到订单: " + order.getOrderNumber() + ", 状态: " + order.getStatus());
            
            // 乘客只能在行程开始前取消订单
            if ("IN_PROGRESS".equals(order.getStatus()) || "COMPLETED".equals(order.getStatus()) || "CANCELLED".equals(order.getStatus())) {
                return Result.error("订单已开始行程或已完成，无法取消");
            }
            
            // 更新订单状态
            order.setStatus("CANCELLED");
            order.setCancelReason("乘客取消");
            order.setUpdatedAt(LocalDateTime.now());
            orderMapper.updateById(order);
            
            // 如果订单已分配给司机，需要释放司机
            if (order.getDriverId() != null) {
                driverRedisService.markDriverFree(order.getDriverId());
                
                // 通知司机订单已被乘客取消
                webSocketNotificationService.notifyDriverOrderCancelled(
                    order.getDriverId(), 
                    orderId, 
                    "乘客已取消订单"
                );
            }
            
            // 推送取消状态给乘客
            if (order.getPassengerId() != null) {
                webSocketNotificationService.notifyPassengerOrderStatusChange(
                    order.getPassengerId(), 
                    orderId, 
                    "CANCELLED", 
                    "订单已取消"
                );
            }
            
            return Result.success("订单取消成功");
        } catch (Exception e) {
            return Result.error("取消订单失败: " + e.getMessage());
        }
    }

    /** 司机取消订单 */
    @PostMapping("/{orderId}/cancel-by-driver")
    public Result<String> cancelOrderByDriver(@PathVariable Long orderId, @RequestParam Long driverId) {
        try {
            Order order = orderMapper.selectById(orderId);
            if (order == null) {
                return Result.error("订单不存在");
            }
            
            // 验证司机权限
            if (!driverId.equals(order.getDriverId())) {
                return Result.error("无权取消此订单");
            }
            
            // 司机只能在行程开始前取消订单
            if ("IN_PROGRESS".equals(order.getStatus()) || "COMPLETED".equals(order.getStatus()) || "CANCELLED".equals(order.getStatus())) {
                return Result.error("订单已开始行程或已完成，无法取消");
            }
            
            // 更新订单状态为待重新分配
            order.setDriverId(null); // 清除司机分配
            order.setStatus("PENDING"); // 重新设为待分配状态
            order.setCancelReason("司机取消，重新分配中");
            order.setUpdatedAt(LocalDateTime.now());
            orderMapper.updateById(order);
            
            // 释放司机状态
            driverRedisService.markDriverFree(driverId);
            
            // 将该司机加入此订单的黑名单，避免重复分配
            driverRedisService.addDriverToOrderBlacklist(orderId, driverId);
            
            // 通知乘客订单正在重新分配
            if (order.getPassengerId() != null) {
                webSocketNotificationService.notifyPassengerOrderStatusChange(
                    order.getPassengerId(), 
                    orderId, 
                    "PENDING", 
                    "司机已取消，正在为您重新寻找司机..."
                );
            }
            
            // 重新进入订单分派队列
            orderDispatchService.dispatchOrder(orderId);
            
            return Result.success("订单已取消并重新分配");
        } catch (Exception e) {
            return Result.error("取消订单失败: " + e.getMessage());
        }
    }

    /** 乘客支付订单 */
    @PostMapping("/{orderId}/pay")
    public Result<String> payOrder(@PathVariable Long orderId, @RequestParam String paymentMethod) {
        try {
            System.out.println("=== 乘客支付订单请求 ===");
            System.out.println("订单ID: " + orderId + ", 支付方式: " + paymentMethod);
            
            Order order = orderMapper.selectById(orderId);
            if (order == null) {
                return Result.error("订单不存在");
            }
            
            if (!"COMPLETED".equals(order.getStatus())) {
                return Result.error("订单未完成，无法支付");
            }
            
            if ("PAID".equals(order.getPaymentStatus())) {
                return Result.error("订单已支付，请勿重复支付");
            }
            
            // 更新支付状态
            order.setPaymentStatus("PAID");
            order.setPaymentMethod(paymentMethod);
            order.setUpdatedAt(LocalDateTime.now());
            orderMapper.updateById(order);
            
            System.out.println("✅ 订单支付成功: " + order.getOrderNumber());
            
            return Result.success("支付成功");
        } catch (Exception e) {
            System.err.println("❌ 订单支付失败: " + e.getMessage());
            return Result.error("支付失败: " + e.getMessage());
        }
    }

    /** 获取乘客的历史订单 */
    @GetMapping("/passenger/{passengerId}/history")
    public Result<List<Order>> getPassengerOrderHistory(@PathVariable Long passengerId) {
        try {
            System.out.println("=== 获取乘客历史订单 ===");
            System.out.println("乘客ID: " + passengerId);
            
            List<Order> orders = orderMapper.selectByPassengerId(passengerId);
            
            // 按创建时间倒序排列
            orders.sort((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()));
            
            System.out.println("✅ 找到 " + orders.size() + " 个历史订单");
            
            return Result.success(orders);
        } catch (Exception e) {
            System.err.println("❌ 获取历史订单失败: " + e.getMessage());
            return Result.error("获取历史订单失败: " + e.getMessage());
        }
    }

    /** 检查乘客是否有未支付订单 */
    @GetMapping("/passenger/{passengerId}/unpaid-check")
    public Result<Boolean> checkUnpaidOrders(@PathVariable Long passengerId) {
        try {
            List<Order> orders = orderMapper.selectByPassengerId(passengerId);
            
            boolean hasUnpaid = orders.stream()
                .anyMatch(order -> "COMPLETED".equals(order.getStatus()) && 
                                 !"PAID".equals(order.getPaymentStatus()));
            
            return Result.success(hasUnpaid);
        } catch (Exception e) {
            return Result.error("检查未支付订单失败: " + e.getMessage());
        }
    }

    /** 获取未支付订单列表 */
    @GetMapping("/unpaid")
    public Result<List<Order>> getUnpaidOrders(HttpServletRequest request) {
        try {
            // 从token中获取用户信息
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            // 这里应该从token中解析用户ID，简化处理
            // 实际项目中需要实现JWT解析
            System.out.println("=== 获取未支付订单列表 ===");
            
            // 临时方案：从请求参数或header中获取passengerId
            String passengerIdStr = request.getParameter("passengerId");
            if (passengerIdStr == null) {
                // 如果没有参数，尝试从所有订单中查找未支付的
                // 这里需要根据实际的用户认证机制来获取当前用户ID
                return Result.error("无法获取用户信息");
            }
            
            Long passengerId = Long.parseLong(passengerIdStr);
            List<Order> allOrders = orderMapper.selectByPassengerId(passengerId);
            
            List<Order> unpaidOrders = allOrders.stream()
                .filter(order -> "COMPLETED".equals(order.getStatus()) && 
                               !"PAID".equals(order.getPaymentStatus()))
                .collect(Collectors.toList());
            
            System.out.println("找到 " + unpaidOrders.size() + " 个未支付订单");
            return Result.success(unpaidOrders);
        } catch (Exception e) {
            System.err.println("获取未支付订单失败: " + e.getMessage());
            e.printStackTrace();
            return Result.error("获取未支付订单失败: " + e.getMessage());
        }
    }

    /** 获取乘客当前进行中的订单 */
    @GetMapping("/passenger/{passengerId}/current")
    public Result<Order> getCurrentOrder(@PathVariable Long passengerId) {
        try {
            System.out.println("=== 获取乘客当前进行中的订单 ===");
            System.out.println("乘客ID: " + passengerId);
            
            List<Order> orders = orderMapper.selectByPassengerId(passengerId);
            
            // 查找进行中的订单（状态为PENDING, ASSIGNED, PICKUP, IN_PROGRESS）
            Order currentOrder = orders.stream()
                .filter(order -> "PENDING".equals(order.getStatus()) || 
                               "ASSIGNED".equals(order.getStatus()) ||
                               "PICKUP".equals(order.getStatus()) ||
                               "IN_PROGRESS".equals(order.getStatus()))
                .findFirst()
                .orElse(null);
            
            if (currentOrder != null) {
                System.out.println("找到进行中的订单: " + currentOrder.getId() + ", 状态: " + currentOrder.getStatus());
            } else {
                System.out.println("没有找到进行中的订单");
            }
            
            return Result.success(currentOrder);
        } catch (Exception e) {
            System.err.println("获取当前订单失败: " + e.getMessage());
            e.printStackTrace();
            return Result.error("获取当前订单失败: " + e.getMessage());
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
            
            // 推送订单完成状态给乘客，提示需要支付
            if (order.getPassengerId() != null) {
                webSocketNotificationService.notifyPassengerOrderStatusChange(
                    order.getPassengerId(), 
                    orderId, 
                    "COMPLETED", 
                    "行程已完成，请完成支付"
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