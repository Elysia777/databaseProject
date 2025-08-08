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

        // 确保rater_id和rated_id都是user_id
        // 验证并确保rater_id是有效的用户ID
        if (review.getRaterId() != null) {
            // 检查是否是有效的用户ID，如果不是则尝试转换
            if (!isValidUserId(review.getRaterId())) {
                // 如果传入的可能是passenger_id，尝试转换为user_id
                Long passengerUserId = getPassengerUserIdByPassengerId(review.getRaterId());
                if (passengerUserId != null) {
                    review.setRaterId(passengerUserId);
                } else {
                    throw new RuntimeException("无效的评价人ID: " + review.getRaterId());
                }
            }
        }
        
        // 验证并确保rated_id是有效的用户ID
        if (review.getRatedId() != null) {
            // 检查是否是有效的用户ID，如果不是则尝试转换
            if (!isValidUserId(review.getRatedId())) {
                // 如果传入的可能是driver_id，尝试转换为user_id
                Long driverUserId = getDriverUserIdByDriverId(review.getRatedId());
                if (driverUserId != null) {
                    review.setRatedId(driverUserId);
                } else {
                    throw new RuntimeException("无效的被评价人ID: " + review.getRatedId());
                }
            }
        }

        // 插入评价
        reviewMapper.insert(review);

        // 更新司机评分（这里需要传递driver_id，不是user_id）
        // 需要通过user_id找到对应的driver_id
        Long driverId = getDriverIdByUserId(review.getRatedId());
        if (driverId != null) {
            updateDriverRating(driverId);
        }

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

    /**
     * 通过user_id获取对应的driver_id
     */
    private Long getDriverIdByUserId(Long userId) {
        if (userId == null) return null;
        try {
            // 通过user_id查找对应的driver记录的ID
            return driverMapper.selectDriverIdByUserId(userId);
        } catch (Exception e) {
            System.err.println("获取driver_id失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 检查是否是有效的用户ID
     */
    private boolean isValidUserId(Long userId) {
        if (userId == null) return false;
        try {
            // 通过查询users表验证用户ID是否存在
            return reviewMapper.checkUserExists(userId);
        } catch (Exception e) {
            System.err.println("验证用户ID失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 通过passenger_id获取对应的user_id
     */
    private Long getPassengerUserIdByPassengerId(Long passengerId) {
        if (passengerId == null) return null;
        try {
            return reviewMapper.getPassengerUserIdByPassengerId(passengerId);
        } catch (Exception e) {
            System.err.println("获取乘客用户ID失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 通过driver_id获取对应的user_id
     */
    private Long getDriverUserIdByDriverId(Long driverId) {
        if (driverId == null) return null;
        try {
            return reviewMapper.getDriverUserIdByDriverId(driverId);
        } catch (Exception e) {
            System.err.println("获取司机用户ID失败: " + e.getMessage());
            return null;
        }
    }

    @Override
    public List<Map<String, Object>> getAllReviewsWithNames() {
        return reviewMapper.selectAllWithNames();
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId) {
        Review review = reviewMapper.selectById(reviewId);
        if (review == null) {
            throw new RuntimeException("评价不存在");
        }
        
        reviewMapper.deleteById(reviewId);
        
        // 重新计算司机评分（需要转换user_id为driver_id）
        if (review.getRatedId() != null) {
            Long driverId = getDriverIdByUserId(review.getRatedId());
            if (driverId != null) {
                updateDriverRating(driverId);
            }
        }
    }
}