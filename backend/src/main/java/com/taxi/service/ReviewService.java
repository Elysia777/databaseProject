package com.taxi.service;

import com.taxi.entity.Review;

import java.util.List;
import java.util.Map;

/**
 * 评价服务接口
 */
public interface ReviewService {

    /** 创建评价 */
    Review createReview(Review review);

    /** 根据ID获取评价 */
    Review getById(Long id);

    /** 根据订单ID获取评价 */
    Review getByOrderId(Long orderId);

    /** 获取乘客的评价列表 */
    List<Review> getByPassengerId(Long passengerId, int page, int size);

    /** 获取司机的评价列表 */
    List<Review> getByDriverId(Long driverId, int page, int size);

    /** 获取司机评分统计 */
    Map<String, Object> getDriverRatingStats(Long driverId);

    /** 检查订单是否已评价 */
    boolean isOrderReviewed(Long orderId);

    /** 更新司机评分 */
    void updateDriverRating(Long driverId);

    /** 获取所有评价（带用户名称） */
    List<Map<String, Object>> getAllReviewsWithNames();

    /** 删除评价 */
    void deleteReview(Long reviewId);
}