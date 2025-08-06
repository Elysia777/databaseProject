package com.taxi.service.impl;

import com.taxi.entity.Vehicle;
import com.taxi.mapper.VehicleMapper;
import com.taxi.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 车辆服务实现类
 */
@Service
public class VehicleServiceImpl implements VehicleService {

    @Autowired
    private VehicleMapper vehicleMapper;

    @Override
    public Vehicle getById(Long id) {
        return vehicleMapper.selectById(id);
    }

    @Override
    public List<Vehicle> getByDriverId(Long driverId) {
        return vehicleMapper.selectByDriverId(driverId);
    }

    @Override
    public Vehicle getActiveByDriverId(Long driverId) {
        return vehicleMapper.selectActiveByDriverId(driverId);
    }

    @Override
    public Vehicle getByPlateNumber(String plateNumber) {
        return vehicleMapper.selectByPlateNumber(plateNumber);
    }

    @Override
    @Transactional
    public Vehicle addVehicle(Vehicle vehicle) {
        // 检查车牌号是否已存在
        Vehicle existing = vehicleMapper.selectByPlateNumber(vehicle.getPlateNumber());
        if (existing != null) {
            throw new RuntimeException("车牌号已存在");
        }

        // 设置创建时间
        vehicle.setCreatedAt(LocalDateTime.now());
        vehicle.setUpdatedAt(LocalDateTime.now());
        
        // 如果是司机的第一辆车，自动设为激活状态
        List<Vehicle> existingVehicles = vehicleMapper.selectByDriverId(vehicle.getDriverId());
        if (existingVehicles.isEmpty()) {
            vehicle.setIsActive(true);
        } else {
            vehicle.setIsActive(false);
        }

        vehicleMapper.insert(vehicle);
        return vehicle;
    }

    @Override
    public Vehicle updateVehicle(Vehicle vehicle) {
        // 检查车牌号是否被其他车辆使用
        Vehicle existing = vehicleMapper.selectByPlateNumber(vehicle.getPlateNumber());
        if (existing != null && !existing.getId().equals(vehicle.getId())) {
            throw new RuntimeException("车牌号已被其他车辆使用");
        }

        vehicle.setUpdatedAt(LocalDateTime.now());
        vehicleMapper.updateById(vehicle);
        return vehicle;
    }

    @Override
    @Transactional
    public boolean deleteVehicle(Long id) {
        Vehicle vehicle = vehicleMapper.selectById(id);
        if (vehicle == null) {
            return false;
        }

        // 如果删除的是激活车辆，需要激活其他车辆
        if (vehicle.getIsActive()) {
            List<Vehicle> otherVehicles = vehicleMapper.selectByDriverId(vehicle.getDriverId());
            otherVehicles.removeIf(v -> v.getId().equals(id));
            
            if (!otherVehicles.isEmpty()) {
                // 激活第一辆其他车辆
                vehicleMapper.setActiveVehicle(vehicle.getDriverId(), otherVehicles.get(0).getId());
            }
        }

        return vehicleMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional
    public boolean setActiveVehicle(Long driverId, Long vehicleId) {
        // 验证车辆是否属于该司机
        Vehicle vehicle = vehicleMapper.selectById(vehicleId);
        if (vehicle == null || !vehicle.getDriverId().equals(driverId)) {
            throw new RuntimeException("车辆不存在或不属于该司机");
        }

        return vehicleMapper.setActiveVehicle(driverId, vehicleId) > 0;
    }
}