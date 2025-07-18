package com.taxi.mapper;

import com.taxi.entity.Passenger;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 乘客Mapper接口
 */
@Mapper
public interface PassengerMapper {

    /** 根据ID查询乘客 */
    Passenger selectById(@Param("id") Long id);

    /** 根据用户ID查询乘客 */
    Passenger selectByUserId(@Param("userId") Long userId);

    /** 插入乘客 */
    int insert(Passenger passenger);

    /** 更新乘客 */
    int updateById(Passenger passenger);

    /** 删除乘客 */
    int deleteById(@Param("id") Long id);

    /** 查询所有乘客 */
    List<Passenger> selectAll();

    /** 根据评分查询乘客 */
    List<Passenger> selectByRating(@Param("minRating") BigDecimal minRating);
} 