 package com.taxi.mapper;

import com.taxi.entity.Vehicle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 车辆Mapper接口
 */
@Mapper
public interface VehicleMapper {

    /** 根据ID查询车辆 */
    Vehicle selectById(@Param("id") Long id);

    /** 根据司机ID查询车辆 */
    Vehicle selectByDriverId(@Param("driverId") Long driverId);

    /** 根据车牌号查询车辆 */
    Vehicle selectByPlateNumber(@Param("plateNumber") String plateNumber);

    /** 插入车辆 */
    int insert(Vehicle vehicle);

    /** 更新车辆 */
    int updateById(Vehicle vehicle);

    /** 删除车辆 */
    int deleteById(@Param("id") Long id);

    /** 查询所有车辆 */
    List<Vehicle> selectAll();

    /** 查询正常状态的车辆 */
    List<Vehicle> selectActiveVehicles();
}