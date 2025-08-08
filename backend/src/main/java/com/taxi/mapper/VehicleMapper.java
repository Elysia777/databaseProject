package com.taxi.mapper;

import com.taxi.entity.Vehicle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 车辆数据访问层
 */
@Mapper
public interface VehicleMapper {

    /** 根据ID查询车辆 */
    Vehicle selectById(Long id);

    /** 根据司机ID查询车辆列表 */
    List<Vehicle> selectByDriverId(Long driverId);

    /** 根据司机ID查询激活的车辆 */
    Vehicle selectActiveByDriverId(Long driverId);

    /** 根据车牌号查询车辆 */
    Vehicle selectByPlateNumber(String plateNumber);

    /** 插入车辆 */
    int insert(Vehicle vehicle);

    /** 根据ID更新车辆 */
    int updateById(Vehicle vehicle);

    /** 根据ID删除车辆 */
    int deleteById(Long id);

    /** 设置司机的激活车辆 */
    int setActiveVehicle(@Param("driverId") Long driverId, @Param("vehicleId") Long vehicleId);

    /** 取消司机所有车辆的激活状态 */
    int deactivateAllByDriverId(Long driverId);

    /** 查询所有车辆（包含司机信息） */
    List<java.util.Map<String, Object>> selectAllWithDriverInfo();
}