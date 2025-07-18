 package com.taxi.mapper;

import com.taxi.entity.Driver;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 司机Mapper接口
 */
@Mapper
public interface DriverMapper {

    /** 根据ID查询司机 */
    Driver selectById(@Param("id") Long id);

    /** 根据用户ID查询司机 */
    Driver selectByUserId(@Param("userId") Long userId);

    /** 根据驾驶证号查询司机 */
    Driver selectByLicenseNumber(@Param("licenseNumber") String licenseNumber);

    /** 插入司机 */
    int insert(Driver driver);

    /** 更新司机 */
    int updateById(Driver driver);

    /** 删除司机 */
    int deleteById(@Param("id") Long id);

    /** 查询所有司机 */
    List<Driver> selectAll();

    /** 查询在线司机 */
    List<Driver> selectOnlineDrivers();

    /** 查询可用司机 */
    List<Driver> selectAvailableDrivers();

    /** 查询所有在线且空闲的司机 */
    List<Driver> selectOnlineAndFreeDrivers();
}