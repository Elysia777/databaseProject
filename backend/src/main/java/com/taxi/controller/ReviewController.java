package com.taxi.controller;

import com.taxi.common.Result;
import com.taxi.entity.Review;
import com.taxi.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 评价控制器
 */
@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = "*")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    /**
     * 创建评价
     */
    @PostMapping
    public Result<Review> createReview(@RequestBody Review review) {
        try {
            Review createdReview = reviewService.createReview(review);
            return Result.success(createdReview);
        } catch (Exception e) {
            return Result.error("创建评价失败: " + e.getMessage());
        }
    }

    /**
     * 根据订单ID获取评价
     */
    @GetMapping("/order/{orderId}")
    public Result<Review> getReviewByOrderId(@PathVariable Long orderId) {
        try {
            Review review = reviewService.getByOrderId(orderId);
            return Result.success(review);
        } catch (Exception e) {
            return Result.error("获取评价失败: " + e.getMessage());
        }
    }

    /**
     * 检查订单是否已评价
     */
    @GetMapping("/order/{orderId}/exists")
    public Result<Boolean> checkOrderReviewed(@PathVariable Long orderId) {
        try {
            boolean reviewed = reviewService.isOrderReviewed(orderId);
            return Result.success(reviewed);
        } catch (Exception e) {
            return Result.error("检查评价状态失败: " + e.getMessage());
        }
    }

    /**
     * 获取乘客发出的评价列表
     */
    @GetMapping("/passenger/{passengerId}")
    public Result<List<Review>> getPassengerReviews(@PathVariable Long passengerId,
                                                   @RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        try {
            List<Review> reviews = reviewService.getByPassengerId(passengerId, page, size);
            return Result.success(reviews);
        } catch (Exception e) {
            return Result.error("获取乘客评价列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取司机的评价列表
     */
    @GetMapping("/driver/{driverId}")
    public Result<List<Review>> getDriverReviews(@PathVariable Long driverId,
                                                @RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        try {
            List<Review> reviews = reviewService.getByDriverId(driverId, page, size);
            return Result.success(reviews);
        } catch (Exception e) {
            return Result.error("获取司机评价列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取司机评分统计
     */
    @GetMapping("/driver/{driverId}/stats")
    public Result<Map<String, Object>> getDriverRatingStats(@PathVariable Long driverId) {
        try {
            Map<String, Object> stats = reviewService.getDriverRatingStats(driverId);
            return Result.success(stats);
        } catch (Exception e) {
            return Result.error("获取司机评分统计失败: " + e.getMessage());
        }
    }

    /**
     * 管理员获取所有评价（带用户名称）
     */
    @GetMapping("/all")
    public Result<List<Map<String, Object>>> getAllReviewsWithNames() {
        try {
            List<Map<String, Object>> reviews = reviewService.getAllReviewsWithNames();
            System.out.println("测试"+reviews);
            return Result.success(reviews);
        } catch (Exception e) {
            return Result.error("获取评价列表失败: " + e.getMessage());
        }
    }

    /**
     * 删除评价（管理员功能）
     */
    @DeleteMapping("/{reviewId}")
    public Result<Void> deleteReview(@PathVariable Long reviewId) {
        try {
            reviewService.deleteReview(reviewId);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error("删除评价失败: " + e.getMessage());
        }
    }
}