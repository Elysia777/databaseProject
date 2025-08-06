package com.taxi.service.impl;

import com.taxi.entity.Review;
import com.taxi.entity.Driver;
import com.taxi.mapper.ReviewMapper;
import com.taxi.mapper.DriverMapper;
import com.taxi.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 评价服务实现类
 */
@Service
public class ReviewServiceImpl implements ReviewService {

    @Autowired
    private ReviewMapper reviewMapper;

    @Autowired
    private DriverMapper driverMapper;

    @Override
    @Transactional
    public Review createReview(Review review) {
        // 检查订单是否已评价
        if (isOrderReviewed(review.getOrderId())) {
            throw new RuntimeException("该订单已评价，不能重复评价");
        }

        // 设置评价类型为乘客评价司机
        review.setRatingType("PASSENGER_TO_DRIVER");
        
        // 设置创建时间
        review.setCreatedAt(LocalDateTime.now());

        // 插入评价
        reviewMapper.insert(review);

        // 更新司机评分
        updateDriverRating(review.getRatedId());

        return review;
    }

    @Override
    public Review getById(Long id) {
        return reviewMapper.selectById(id);
    }

    @Override
    public Review getByOrderId(Long orderId) {
        return reviewMapper.selectByOrderId(orderId);
    }

    @Override
    public List<Review> getByPassengerId(Long passengerId, int page, int size) {
        int offset = (page - 1) * size;
        return reviewMapper.selectByPassengerId(passengerId, offset, size);
    }

    @Override
    public List<Review> getByDriverId(Long driverId, int page, int size) {
        int offset = (page - 1) * size;
        return reviewMapper.selectByDriverId(driverId, offset, size);
    }

    @Override
    public Map<String, Object> getDriverRatingStats(Long driverId) {
        return reviewMapper.selectDriverRatingStats(driverId);
    }

    @Override
    public boolean isOrderReviewed(Long orderId) {
        return reviewMapper.countByOrderId(orderId) > 0;
    }

    @Override
    @Transactional
    public void updateDriverRating(Long driverId) {
        try {
            // 获取司机评分统计
            Map<String, Object> stats = reviewMapper.selectDriverRatingStats(driverId);
            
            if (stats != null && stats.get("totalReviews") != null) {
                Long totalReviews = ((Number) stats.get("totalReviews")).longValue();
                BigDecimal averageRating = (BigDecimal) stats.get("averageRating");
                
                if (totalReviews > 0 && averageRating != null) {
                    // 更新司机表中的评分
                    Driver driver = driverMapper.selectById(driverId);
                    if (driver != null) {
                        driver.setRating(averageRating);
                        driver.setUpdatedAt(LocalDateTime.now());
                        driverMapper.updateById(driver);
                        
                        System.out.println("司机 " + driverId + " 评分已更新: " + averageRating + " (基于 " + totalReviews + " 个评价)");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("更新司机评分失败: " + e.getMessage());
            // 不抛出异常，避免影响评价创建
        }
    }
}