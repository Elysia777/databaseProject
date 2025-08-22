package com.taxi.controller;

import com.taxi.common.Result;
import com.taxi.dto.CreateComplaintRequest;
import com.taxi.dto.ProcessComplaintRequest;
import com.taxi.dto.ComplaintWithOrderInfo;
import com.taxi.entity.Complaint;
import com.taxi.entity.Order;
import com.taxi.mapper.ComplaintMapper;
import com.taxi.mapper.OrderMapper;
import com.taxi.mapper.UserMapper;
import com.taxi.mapper.DriverMapper;
import com.taxi.mapper.PassengerMapper;
import com.taxi.entity.Driver;
import com.taxi.entity.Passenger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/complaints")
@CrossOrigin(origins = "*")
public class ComplaintController {

    @Autowired
    private ComplaintMapper complaintMapper;
    
    @Autowired
    private OrderMapper orderMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private DriverMapper driverMapper;
    
    @Autowired
    private PassengerMapper passengerMapper;

    /**
     * 创建投诉
     */
    @PostMapping("/create")
    public Result<Long> createComplaint(@RequestBody CreateComplaintRequest request,
                                       @RequestParam Long complainantId) {
        try {
            System.out.println("=== 创建投诉请求 ===");
            System.out.println("投诉人用户ID: " + complainantId);
            System.out.println("请求数据: " + request);

            // 验证订单是否存在，并获取被投诉人信息
            Long defendantUserId = null;
            
            // 如果请求中指定了被投诉人ID，直接使用（应该是用户ID）
            if (request.getDefendantId() != null) {
                defendantUserId = request.getDefendantId();
                System.out.println("使用请求中提供的被投诉人用户ID: " + defendantUserId);
            } else if (request.getOrderId() != null) {
                // 如果没有指定被投诉人，从订单中推断
                var order = orderMapper.selectById(request.getOrderId());
                if (order == null) {
                    return Result.error("订单不存在");
                }
                
                // 根据投诉人身份确定被投诉人
                // 先检查投诉人是否是司机
                boolean isDriverComplainant = false;
                try {
                    Driver complainantDriver = driverMapper.selectByUserId(complainantId);
                    if (complainantDriver != null) {
                        isDriverComplainant = true;
                        System.out.println("投诉人是司机，用户ID: " + complainantId);
                    }
                } catch (Exception e) {
                    System.out.println("投诉人不是司机，检查是否是乘客");
                }
                
                if (isDriverComplainant) {
                    // 司机投诉乘客：从订单中获取乘客ID，然后查找对应的用户ID
                    if (order.getPassengerId() != null) {
                        try {
                            Passenger passenger = passengerMapper.selectById(order.getPassengerId());
                            if (passenger != null && passenger.getUserId() != null) {
                                defendantUserId = passenger.getUserId();
                                System.out.println("司机投诉乘客 - 从订单获取乘客ID: " + order.getPassengerId() + ", 对应用户ID: " + defendantUserId);
                            } else {
                                System.err.println("未找到乘客信息或乘客未关联用户，乘客ID: " + order.getPassengerId());
                            }
                        } catch (Exception e) {
                            System.err.println("获取乘客用户ID失败: " + e.getMessage());
                        }
                    }
                } else {
                    // 乘客投诉司机：从订单中获取司机ID，然后查找对应的用户ID
                    if (order.getDriverId() != null) {
                        try {
                            Driver driver = driverMapper.selectById(order.getDriverId());
                            if (driver != null && driver.getUserId() != null) {
                                defendantUserId = driver.getUserId();
                                System.out.println("乘客投诉司机 - 从订单获取司机ID: " + order.getDriverId() + ", 对应用户ID: " + defendantUserId);
                            } else {
                                System.err.println("未找到司机信息或司机未关联用户，司机ID: " + order.getDriverId());
                            }
                        } catch (Exception e) {
                            System.err.println("获取司机用户ID失败: " + e.getMessage());
                        }
                    }
                }
            }
            
            if (defendantUserId == null) {
                return Result.error("无法确定被投诉人");
            }

            // 创建投诉对象
            Complaint complaint = new Complaint();
            complaint.setOrderId(request.getOrderId());
            complaint.setComplainantId(complainantId); // 投诉人用户ID
            complaint.setDefendantId(defendantUserId); // 被投诉人用户ID
            complaint.setComplaintType(request.getComplaintType());
            complaint.setTitle(request.getTitle());
            complaint.setDescription(request.getDescription());
            complaint.setEvidenceFiles(request.getEvidenceFiles());
            complaint.setStatus("PENDING");
            complaint.setCreatedAt(LocalDateTime.now());
            complaint.setUpdatedAt(LocalDateTime.now());

            // 插入数据库
            int result = complaintMapper.insert(complaint);
            if (result > 0) {
                System.out.println("✅ 投诉创建成功，ID: " + complaint.getId());
                return Result.success(complaint.getId());
            } else {
                return Result.error("创建投诉失败");
            }
        } catch (Exception e) {
            System.err.println("❌ 创建投诉异常: " + e.getMessage());
            e.printStackTrace();
            return Result.error("创建投诉失败: " + e.getMessage());
        }
    }

    /**
     * 获取投诉详情
     */
    @GetMapping("/{complaintId}")
    public Result<Map<String, Object>> getComplaintDetail(@PathVariable Long complaintId) {
        try {
            Map<String, Object> complaint = complaintMapper.selectComplaintWithDetails(complaintId);
            if (complaint == null) {
                return Result.error("投诉不存在");
            }
            return Result.success(complaint);
        } catch (Exception e) {
            System.err.println("获取投诉详情失败: " + e.getMessage());
            return Result.error("获取投诉详情失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户的投诉列表（通过用户ID）
     */
    @GetMapping("/user/{userId}")
    public Result<List<ComplaintWithOrderInfo>> getUserComplaints(@PathVariable Long userId,
                                                    @RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        try {
            System.out.println("=== 获取用户投诉列表 ===");
            System.out.println("用户ID: " + userId);
            
            int offset = (page - 1) * size;
            
            // 通过用户ID查找投诉
            List<Complaint> complaints = complaintMapper.selectByComplainantId(userId, offset, size);
            
            // 转换为包含订单信息的DTO
            List<ComplaintWithOrderInfo> complaintsWithOrderInfo = new java.util.ArrayList<>();
            
            for (Complaint complaint : complaints) {
                ComplaintWithOrderInfo complaintWithOrder = new ComplaintWithOrderInfo(complaint);
                
                // 如果投诉关联了订单，获取订单信息
                if (complaint.getOrderId() != null) {
                    try {
                        var order = orderMapper.selectById(complaint.getOrderId());
                        if (order != null) {
                            complaintWithOrder.setOrderNumber(order.getOrderNumber());
                            complaintWithOrder.setPickupAddress(order.getPickupAddress());
                            complaintWithOrder.setDestinationAddress(order.getDestinationAddress());
                            complaintWithOrder.setActualFare(order.getActualFare());
                            complaintWithOrder.setPaymentStatus(order.getPaymentStatus());
                            
                            // 格式化时间
                            if (order.getPickupTime() != null) {
                                complaintWithOrder.setPickupTime(order.getPickupTime().toString());
                            }
                            if (order.getCompletionTime() != null) {
                                complaintWithOrder.setCompletionTime(order.getCompletionTime().toString());
                            }
                            
                            System.out.println("为投诉 " + complaint.getId() + " 添加订单信息: " + order.getOrderNumber());
                        }
                    } catch (Exception e) {
                        System.err.println("获取投诉 " + complaint.getId() + " 的订单信息失败: " + e.getMessage());
                    }
                }
                
                complaintsWithOrderInfo.add(complaintWithOrder);
            }
            
            System.out.println("查询到 " + complaintsWithOrderInfo.size() + " 条投诉记录");
            
            return Result.success(complaintsWithOrderInfo);
        } catch (Exception e) {
            System.err.println("获取用户投诉列表失败: " + e.getMessage());
            e.printStackTrace();
            return Result.error("获取投诉列表失败: " + e.getMessage());
        }
    }

    /**
     * 管理员获取所有投诉列表
     */
    @GetMapping("/admin/list")
    public Result<Map<String, Object>> getComplaintsForAdmin(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String complaintType) {
        try {
            System.out.println("=== 管理员获取投诉列表 ===");
            System.out.println("页码: " + page + ", 大小: " + size);
            System.out.println("状态筛选: " + status);
            System.out.println("类型筛选: " + complaintType);

            int offset = (page - 1) * size;
            
            // 获取投诉列表
            List<Map<String, Object>> complaints = complaintMapper.selectAllWithDetails(offset, size, status, complaintType);
            
            // 获取总数
            int total = complaintMapper.countAll(status, complaintType);

            Map<String, Object> result = new HashMap<>();
            result.put("complaints", complaints);
            result.put("total", total);
            result.put("page", page);
            result.put("size", size);
            result.put("totalPages", (int) Math.ceil((double) total / size));

            System.out.println("查询到 " + complaints.size() + " 条投诉记录，总计 " + total + " 条");
            
            return Result.success(result);
        } catch (Exception e) {
            System.err.println("获取投诉列表失败: " + e.getMessage());
            e.printStackTrace();
            return Result.error("获取投诉列表失败: " + e.getMessage());
        }
    }

    /**
     * 管理员处理投诉
     */
    @PostMapping("/admin/process/{complaintId}")
    public Result<String> processComplaint(@PathVariable Long complaintId,
                                          @RequestBody ProcessComplaintRequest request,
                                          @RequestParam Long adminId) {
        try {
            System.out.println("=== 管理员处理投诉 ===");
            System.out.println("投诉ID: " + complaintId);
            System.out.println("管理员ID: " + adminId);
            System.out.println("处理请求: " + request);

            // 查询投诉
            Complaint complaint = complaintMapper.selectById(complaintId);
            if (complaint == null) {
                return Result.error("投诉不存在");
            }

            // 更新投诉状态
            complaint.setStatus(request.getStatus());
            complaint.setResolution(request.getResolution());
            complaint.setRefundAmount(request.getRefundAmount()); // 保存退款金额
            complaint.setAdminId(adminId);
            complaint.setResolutionTime(LocalDateTime.now());
            complaint.setUpdatedAt(LocalDateTime.now());

            int result = complaintMapper.updateById(complaint);
            if (result > 0) {
                // 如果需要退款，更新相关订单的支付状态
                if (request.getRefundAmount() != null && request.getRefundAmount() > 0) {
                    System.out.println("处理退款: " + request.getRefundAmount() + " 元");
                    
                    if (complaint.getOrderId() != null) {
                        try {
                            // 检查订单当前状态
                            var order = orderMapper.selectById(complaint.getOrderId());
                            if (order != null) {
                                // 检查订单是否已经退款
                                if ("REFUNDED".equals(order.getPaymentStatus())) {
                                    System.out.println("⚠️ 订单已经退款，不能重复退款，订单ID: " + complaint.getOrderId());
                                    return Result.error("该订单已经退款，不能重复退款");
                                }
                                
                                // 检查是否有其他投诉已经对此订单进行了退款
                                List<Complaint> existingComplaints = complaintMapper.selectByOrderId(complaint.getOrderId());
                                boolean hasExistingRefund = existingComplaints.stream()
                                        .filter(c -> !c.getId().equals(complaintId)) // 排除当前投诉
                                        .anyMatch(c -> c.getRefundAmount() != null && c.getRefundAmount() > 0);
                                
                                if (hasExistingRefund) {
                                    System.out.println("⚠️ 该订单已有其他投诉进行了退款，不能重复退款，订单ID: " + complaint.getOrderId());
                                    return Result.error("该订单已有其他投诉进行了退款，不能重复退款");
                                }
                                order.setPaymentStatus("REFUNDED");
                                order.setUpdatedAt(LocalDateTime.now());
                                
                                int orderUpdateResult = orderMapper.updateById(order);
                                if (orderUpdateResult > 0) {
                                    System.out.println("✅ 订单支付状态已更新为REFUNDED，订单ID: " + complaint.getOrderId());
                                } else {
                                    System.err.println("❌ 更新订单支付状态失败，订单ID: " + complaint.getOrderId());
                                }
                            } else {
                                System.err.println("❌ 未找到相关订单，订单ID: " + complaint.getOrderId());
                            }
                        } catch (Exception e) {
                            System.err.println("❌ 更新订单支付状态异常: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
                
                System.out.println("✅ 投诉处理成功");
                return Result.success("投诉处理成功");
            } else {
                return Result.error("处理投诉失败");
            }
        } catch (Exception e) {
            System.err.println("❌ 处理投诉异常: " + e.getMessage());
            e.printStackTrace();
            return Result.error("处理投诉失败: " + e.getMessage());
        }
    }

    /**
     * 获取投诉统计数据
     */
    @GetMapping("/admin/stats")
    public Result<Map<String, Object>> getComplaintStats() {
        try {
            Map<String, Object> stats = complaintMapper.selectComplaintStats();
            return Result.success(stats);
        } catch (Exception e) {
            System.err.println("获取投诉统计失败: " + e.getMessage());
            return Result.error("获取统计数据失败: " + e.getMessage());
        }
    }

    /**
     * 根据订单ID获取投诉
     */
    @GetMapping("/order/{orderId}")
    public Result<List<Complaint>> getComplaintsByOrderId(@PathVariable Long orderId) {
        try {
            List<Complaint> complaints = complaintMapper.selectByOrderId(orderId);
            return Result.success(complaints);
        } catch (Exception e) {
            System.err.println("获取订单投诉失败: " + e.getMessage());
            return Result.error("获取订单投诉失败: " + e.getMessage());
        }
    }
}