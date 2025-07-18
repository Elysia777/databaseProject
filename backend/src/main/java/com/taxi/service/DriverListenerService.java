package com.taxi.service;

import com.taxi.entity.Order;
import com.taxi.entity.Driver;
import com.taxi.mapper.DriverMapper;
import com.taxi.mapper.OrderMapper;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 司机动态监听服务
 * 管理司机对订单队列的监听
 */
@Service
public class DriverListenerService {

    @Autowired
    private ConnectionFactory connectionFactory;
    
    @Autowired
    private MessageConverter messageConverter;
    
    @Autowired
    private OrderMapper orderMapper;
    
    @Autowired
    private DriverMapper driverMapper;
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    // 存储每个司机的监听容器
    private final Map<Long, SimpleMessageListenerContainer> driverListeners = new ConcurrentHashMap<>();
    
    /**
     * 司机上线，开始监听订单队列
     */
    public void startListening(Long driverId) {
        // 如果司机已经在监听，先停止
        stopListening(driverId);
        
        System.out.println("司机 " + driverId + " 开始监听订单队列");
        
        // 创建新的监听容器
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames("order_waiting_queue");
        container.setConcurrentConsumers(1); // 一个司机只处理一个订单
        container.setMaxConcurrentConsumers(1);
        
        // 设置消息监听器
        container.setMessageListener((ChannelAwareMessageListener) (message, channel) -> {
            try {
                System.out.println("司机 " + driverId + " 收到订单消息");
                
                // 转换消息
                Object convertedMessage = messageConverter.fromMessage(message);
                Long orderId = null;
                
                if (convertedMessage instanceof Long) {
                    orderId = (Long) convertedMessage;
                } else {
                    System.out.println("不支持的消息类型: " + convertedMessage.getClass());
                    channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                    return;
                }
                
                // 尝试分配订单给这个司机
                boolean assigned = tryAssignOrder(driverId, orderId);
                
                if (assigned) {
                    // 分配成功，确认消息
                    channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                    System.out.println("订单 " + orderId + " 成功分配给司机 " + driverId);
                    
                    // 司机接到订单后停止监听（一个司机同时只能处理一个订单）
                    stopListening(driverId);
                } else {
                    // 分配失败，拒绝消息并重新入队
                    channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
                    System.out.println("司机 " + driverId + " 无法接受订单 " + orderId + "，消息重新入队");
                }
                
            } catch (Exception e) {
                System.err.println("司机 " + driverId + " 处理订单消息时出错: " + e.getMessage());
                e.printStackTrace();
                try {
                    // 出错时拒绝消息并重新入队
                    channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
                } catch (Exception ex) {
                    System.err.println("拒绝消息时出错: " + ex.getMessage());
                }
            }
        });
        
        // 启动监听容器
        container.start();
        
        // 保存到映射中
        driverListeners.put(driverId, container);
        
        System.out.println("司机 " + driverId + " 监听器启动成功");
    }
    
    /**
     * 司机下线，停止监听订单队列
     */
    public void stopListening(Long driverId) {
        SimpleMessageListenerContainer container = driverListeners.remove(driverId);
        if (container != null) {
            container.stop();
            System.out.println("司机 " + driverId + " 停止监听订单队列");
        }
    }
    
    /**
     * 尝试将订单分配给指定司机
     */
    private boolean tryAssignOrder(Long driverId, Long orderId) {
        try {
            // 查询司机信息
            Driver driver = driverMapper.selectById(driverId);
            if (driver == null || !driver.getIsOnline()) {
                System.out.println("司机 " + driverId + " 不存在或不在线");
                return false;
            }
            
            // 查询订单信息
            Order order = orderMapper.selectById(orderId);
            if (order == null) {
                System.out.println("订单 " + orderId + " 不存在");
                return false;
            }
            
            // 检查订单状态
            if (!"PENDING".equals(order.getStatus())) {
                System.out.println("订单 " + orderId + " 状态不是PENDING，当前状态: " + order.getStatus());
                return false;
            }
            
            // 检查司机是否已有其他进行中的订单
            if (hasActiveOrder(driverId)) {
                System.out.println("司机 " + driverId + " 已有进行中的订单");
                return false;
            }
            
            // 检查距离是否合理（可选）
            if (!isDistanceAcceptable(driver, order)) {
                System.out.println("司机 " + driverId + " 距离订单 " + orderId + " 太远");
                return false;
            }
            
            // 分配订单
            order.setDriverId(driverId);
            order.setStatus("ASSIGNED");
            order.setUpdatedAt(LocalDateTime.now());
            orderMapper.updateById(order);
            
            System.out.println("订单分配成功 - 订单: " + order.getOrderNumber() + ", 司机: " + driverId);
            
            // 通知司机有新订单（可以通过WebSocket或其他方式）
            notifyDriverNewOrder(driverId, order);
            
            return true;
            
        } catch (Exception e) {
            System.err.println("分配订单时出错: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 检查司机是否有进行中的订单
     */
    private boolean hasActiveOrder(Long driverId) {
        // 这里可以查询数据库检查司机是否有ASSIGNED、PICKUP、IN_PROGRESS状态的订单
        // 简化处理，假设如果司机在监听就说明没有活跃订单
        return false;
    }
    
    /**
     * 检查距离是否可接受
     */
    private boolean isDistanceAcceptable(Driver driver, Order order) {
        // 检查司机和订单位置信息
        if (driver.getCurrentLatitude() == null || driver.getCurrentLongitude() == null ||
            order.getPickupLatitude() == null || order.getPickupLongitude() == null) {
            return true; // 如果没有位置信息，默认接受
        }
        
        // 计算距离
        double distance = calcDistance(
            driver.getCurrentLatitude().doubleValue(),
            driver.getCurrentLongitude().doubleValue(),
            order.getPickupLatitude().doubleValue(),
            order.getPickupLongitude().doubleValue()
        );
        
        // 设置最大接受距离为5公里
        double maxDistance = 5000; // 5公里
        return distance <= maxDistance;
    }
    
    /**
     * 通知司机有新订单
     */
    private void notifyDriverNewOrder(Long driverId, Order order) {
        // 这里可以通过WebSocket、推送通知等方式通知司机
        System.out.println("通知司机 " + driverId + " 有新订单: " + order.getOrderNumber());
        
        // 也可以发送到司机通知队列
        try {
            rabbitTemplate.convertAndSend("driver_notification_queue", 
                "司机" + driverId + "，您有新订单：" + order.getOrderNumber());
        } catch (Exception e) {
            System.err.println("发送司机通知失败: " + e.getMessage());
        }
    }
    
    /**
     * 计算两点间距离
     */
    private double calcDistance(double lat1, double lng1, double lat2, double lng2) {
        double radLat1 = Math.toRadians(lat1);
        double radLat2 = Math.toRadians(lat2);
        double a = radLat1 - radLat2;
        double b = Math.toRadians(lng1) - Math.toRadians(lng2);
        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a/2),2) +
                Math.cos(radLat1)*Math.cos(radLat2)*Math.pow(Math.sin(b/2),2)));
        s = s * 6378137.0; // 地球半径
        return s;
    }
    
    /**
     * 获取当前监听的司机数量
     */
    public int getActiveListenerCount() {
        return driverListeners.size();
    }
    
    /**
     * 检查司机是否在监听
     */
    public boolean isDriverListening(Long driverId) {
        return driverListeners.containsKey(driverId);
    }
}