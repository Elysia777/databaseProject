package com.taxi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    
    /**
     * 订单等待队列 - 所有待分配的订单都在这里排队
     */
    @Bean
    public Queue orderWaitingQueue() {
        return QueueBuilder.durable("order_waiting_queue").build();
    }
    
    /**
     * 司机通知队列 - 用于异步通知司机有新订单
     */
    @Bean
    public Queue driverNotificationQueue() {
        return QueueBuilder.durable("driver_notification_queue").build();
    }
    
    /**
     * 订单状态更新队列 - 用于处理订单状态变更
     */
    @Bean
    public Queue orderStatusQueue() {
        return QueueBuilder.durable("order_status_queue").build();
    }
    
    /**
     * 司机状态更新队列 - 用于处理司机状态变更
     */
    @Bean
    public Queue driverStatusQueue() {
        return QueueBuilder.durable("driver_status_queue").build();
    }
    
    /**
     * 延迟重试队列 - 用于处理订单分配失败后的重试
     */
    @Bean
    public Queue orderRetryQueue() {
        return QueueBuilder.durable("order_retry_queue")
                .withArgument("x-message-ttl", 30000) // 30秒延迟
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", "order_waiting_queue")
                .build();
    }
    
    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(objectMapper);
    }
    
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
    
    /**
     * 配置RabbitListener容器工厂，使用JSON消息转换器
     */
    @Bean
    public org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory factory = 
            new org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        
        // 设置错误处理器
        factory.setErrorHandler(new org.springframework.amqp.rabbit.listener.ConditionalRejectingErrorHandler());
        
        // 设置并发消费者数量
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(5);
        
        return factory;
    }
} 