package com.taxi.service;

import com.taxi.entity.Vehicle;

import java.util.List;

/**
 * 车辆服务接口
 */
public interface VehicleService {

    /** 根据ID查询车辆 */
    Vehicle getById(Long id);

    /** 根据司机ID查询车辆列表 */
    List<Vehicle> getByDriverId(Long driverId);

    /** 根据司机ID查询激活的车辆 */
    Vehicle getActiveByDriverId(Long driverId);

    /** 根据车牌号查询车辆 */
    Vehicle getByPlateNumber(String plateNumber);

    /** 添加车辆 */
    Vehicle addVehicle(Vehicle vehicle);

    /** 更新车辆信息 */
    Vehicle updateVehicle(Vehicle vehicle);

    /** 删除车辆 */
    boolean deleteVehicle(Long id);

    /** 设置激活车辆 */
    boolean setActiveVehicle(Long driverId, Long vehicleId);
}