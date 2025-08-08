package com.taxi.controller;

import com.taxi.common.Result;
import com.taxi.entity.Vehicle;
import com.taxi.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 车辆控制器
 */
@RestController
@RequestMapping("/api/vehicles")
@CrossOrigin(origins = "*")
public class VehicleController {

    @Autowired
    private VehicleService vehicleService;

    /**
     * 根据司机ID获取车辆列表
     */
    @GetMapping("/driver/{driverId}")
    public Result<List<Vehicle>> getVehiclesByDriverId(@PathVariable Long driverId) {
        try {
            List<Vehicle> vehicles = vehicleService.getByDriverId(driverId);
            return Result.success(vehicles);
        } catch (Exception e) {
            return Result.error("获取车辆列表失败: " + e.getMessage());
        }
    }

    /**
     * 根据司机ID获取激活的车辆
     */
    @GetMapping("/driver/{driverId}/active")
    public Result<Vehicle> getActiveVehicleByDriverId(@PathVariable Long driverId) {
        try {
            Vehicle vehicle = vehicleService.getActiveByDriverId(driverId);
            return Result.success(vehicle);
        } catch (Exception e) {
            return Result.error("获取激活车辆失败: " + e.getMessage());
        }
    }

    /**
     * 添加车辆
     */
    @PostMapping
    public Result<Vehicle> addVehicle(@RequestBody Vehicle vehicle) {
        try {
            Vehicle savedVehicle = vehicleService.addVehicle(vehicle);
            return Result.success(savedVehicle);
        } catch (Exception e) {
            return Result.error("添加车辆失败: " + e.getMessage());
        }
    }

    /**
     * 更新车辆信息
     */
    @PutMapping("/{id}")
    public Result<Vehicle> updateVehicle(@PathVariable Long id, @RequestBody Vehicle vehicle) {
        try {
            vehicle.setId(id);
            Vehicle updatedVehicle = vehicleService.updateVehicle(vehicle);
            return Result.success(updatedVehicle);
        } catch (Exception e) {
            return Result.error("更新车辆失败: " + e.getMessage());
        }
    }

    /**
     * 删除车辆
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteVehicle(@PathVariable Long id) {
        try {
            boolean success = vehicleService.deleteVehicle(id);
            if (success) {
                return Result.success(null);
            } else {
                return Result.error("删除车辆失败");
            }
        } catch (Exception e) {
            return Result.error("删除车辆失败: " + e.getMessage());
        }
    }

    /**
     * 设置激活车辆
     */
    @PostMapping("/driver/{driverId}/active/{vehicleId}")
    public Result<Void> setActiveVehicle(@PathVariable Long driverId, @PathVariable Long vehicleId) {
        try {
            boolean success = vehicleService.setActiveVehicle(driverId, vehicleId);
            if (success) {
                return Result.success(null);
            } else {
                return Result.error("设置激活车辆失败");
            }
        } catch (Exception e) {
            return Result.error("设置激活车辆失败: " + e.getMessage());
        }
    }

    /**
     * 管理员获取所有车辆（包含司机信息）
     */
    @GetMapping("/all")
    public Result<List<java.util.Map<String, Object>>> getAllVehiclesWithDriverInfo() {
        try {
            System.out.println("开始获取所有车辆信息...");
            List<java.util.Map<String, Object>> vehicles = vehicleService.getAllVehiclesWithDriverInfo();
            System.out.println("获取到车辆数量: " + (vehicles != null ? vehicles.size() : "null"));
            if (vehicles != null && !vehicles.isEmpty()) {
                System.out.println("第一辆车信息: " + vehicles.get(0));
            }
            return Result.success(vehicles);
        } catch (Exception e) {
            System.err.println("获取车辆列表失败: " + e.getMessage());
            e.printStackTrace();
            return Result.error("获取车辆列表失败: " + e.getMessage());
        }
    }

    /**
     * 审核车辆
     */
    @PostMapping("/{id}/approve")
    public Result<Void> approveVehicle(@PathVariable Long id) {
        try {
            boolean success = vehicleService.approveVehicle(id);
            if (success) {
                return Result.success(null);
            } else {
                return Result.error("审核车辆失败");
            }
        } catch (Exception e) {
            return Result.error("审核车辆失败: " + e.getMessage());
        }
    }

    /**
     * 拒绝车辆
     */
    @PostMapping("/{id}/reject")
    public Result<Void> rejectVehicle(@PathVariable Long id) {
        try {
            boolean success = vehicleService.rejectVehicle(id);
            if (success) {
                return Result.success(null);
            } else {
                return Result.error("拒绝车辆失败");
            }
        } catch (Exception e) {
            return Result.error("拒绝车辆失败: " + e.getMessage());
        }
    }

    /**
     * 停用车辆
     */
    @PostMapping("/{id}/deactivate")
    public Result<Void> deactivateVehicle(@PathVariable Long id) {
        try {
            boolean success = vehicleService.deactivateVehicle(id);
            if (success) {
                return Result.success(null);
            } else {
                return Result.error("停用车辆失败");
            }
        } catch (Exception e) {
            return Result.error("停用车辆失败: " + e.getMessage());
        }
    }
}