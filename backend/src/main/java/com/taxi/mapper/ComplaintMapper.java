package com.taxi.mapper;

import com.taxi.entity.Complaint;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 投诉Mapper接口
 */
@Mapper
public interface ComplaintMapper {

    /** 根据ID查询投诉 */
    Complaint selectById(@Param("id") Long id);

    /** 插入投诉 */
    int insert(Complaint complaint);

    /** 更新投诉 */
    int updateById(Complaint complaint);

    /** 删除投诉 */
    int deleteById(@Param("id") Long id);

    /** 查询所有投诉（管理员用） */
    List<Map<String, Object>> selectAllWithDetails(@Param("offset") int offset, 
                                                   @Param("size") int size,
                                                   @Param("status") String status,
                                                   @Param("complaintType") String complaintType);

    /** 统计投诉总数 */
    int countAll(@Param("status") String status, @Param("complaintType") String complaintType);

    /** 根据投诉人ID查询投诉 */
    List<Complaint> selectByComplainantId(@Param("complainantId") Long complainantId,
                                         @Param("offset") int offset,
                                         @Param("size") int size);

    /** 根据被投诉人ID查询投诉 */
    List<Complaint> selectByDefendantId(@Param("defendantId") Long defendantId,
                                       @Param("offset") int offset,
                                       @Param("size") int size);

    /** 根据订单ID查询投诉 */
    List<Complaint> selectByOrderId(@Param("orderId") Long orderId);

    /** 根据状态查询投诉 */
    List<Complaint> selectByStatus(@Param("status") String status,
                                  @Param("offset") int offset,
                                  @Param("size") int size);

    /** 查询投诉详情（包含关联信息） */
    Map<String, Object> selectComplaintWithDetails(@Param("id") Long id);

    /** 统计投诉数据 */
    Map<String, Object> selectComplaintStats();
}