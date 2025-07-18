 package com.taxi.service.impl;

import com.taxi.dto.LoginRequest;
import com.taxi.dto.RegisterRequest;
import com.taxi.dto.UserInfo;
import com.taxi.entity.User;
import com.taxi.entity.Passenger;
import com.taxi.entity.Driver;
import com.taxi.mapper.UserMapper;
import com.taxi.mapper.PassengerMapper;
import com.taxi.mapper.DriverMapper;
import com.taxi.service.UserService;
import com.taxi.util.JwtUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PassengerMapper passengerMapper;

    @Autowired
    private DriverMapper driverMapper;

    @Override
    public UserInfo register(RegisterRequest request) {
        // 检查用户名是否已存在
        User existingUser = userMapper.selectByUsername(request.getUsername());
        if (existingUser != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 检查手机号是否已存在
        existingUser = userMapper.selectByPhone(request.getPhone());
        if (existingUser != null) {
            throw new RuntimeException("手机号已存在");
        }

        // 创建新用户
        User user = new User();
        BeanUtils.copyProperties(request, user);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        userMapper.insert(user);

        // 根据用户类型创建对应的乘客或司机记录
        if ("PASSENGER".equals(user.getUserType())) {
            Passenger passenger = new Passenger();
            passenger.setUserId(user.getId());
            passenger.setDefaultPaymentMethod("WECHAT");
            passenger.setRating(new BigDecimal("5.00"));
            passenger.setTotalOrders(0);
            passenger.setTotalSpent(new BigDecimal("0.00"));
            passenger.setCreatedAt(LocalDateTime.now());
            passenger.setUpdatedAt(LocalDateTime.now());
            passengerMapper.insert(passenger);
        } else if ("DRIVER".equals(user.getUserType())) {
            Driver driver = new Driver();
            driver.setUserId(user.getId());
            
            // 使用用户提供的驾驶证号，如果没有则生成默认值
            if (request.getDriverLicense() != null && !request.getDriverLicense().trim().isEmpty()) {
                driver.setDriverLicense(request.getDriverLicense());
            } else {
                driver.setDriverLicense("DL" + System.currentTimeMillis());
            }
            
            // 使用用户提供的驾龄，如果没有则默认为0
            driver.setDrivingYears(request.getDrivingYears() != null ? request.getDrivingYears() : 0);
            
            // 使用用户提供的从业资格证号
            if (request.getProfessionalLicense() != null && !request.getProfessionalLicense().trim().isEmpty()) {
                driver.setProfessionalLicense(request.getProfessionalLicense());
            }
            
            driver.setTotalMileage(new BigDecimal("0.00"));
            driver.setRating(new BigDecimal("5.00"));
            driver.setTotalOrders(0);
            driver.setCompletedOrders(0);
            driver.setCancelledOrders(0);
            driver.setIsOnline(false);
            driver.setCreatedAt(LocalDateTime.now());
            driver.setUpdatedAt(LocalDateTime.now());
            driverMapper.insert(driver);
        }

        // 返回用户信息
        UserInfo userInfo = new UserInfo();
        BeanUtils.copyProperties(user, userInfo);
        userInfo.setToken(jwtUtil.generateToken(user.getId(), user.getUsername(), user.getUserType()));
        
        return userInfo;
    }

    @Override
    public UserInfo login(LoginRequest request) {
        // 根据用户名或手机号查找用户
        User user = userMapper.selectByUsername(request.getUsername());
        if (user == null) {
            user = userMapper.selectByPhone(request.getUsername());
        }

        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        // 检查用户状态
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new RuntimeException("用户已被禁用");
        }

        // 返回用户信息
        UserInfo userInfo = new UserInfo();
        BeanUtils.copyProperties(user, userInfo);
        userInfo.setToken(jwtUtil.generateToken(user.getId(), user.getUsername(), user.getUserType()));
        
        return userInfo;
    }

    @Override
    public UserInfo getUserInfo(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        UserInfo userInfo = new UserInfo();
        BeanUtils.copyProperties(user, userInfo);
        return userInfo;
    }

    @Override
    public UserInfo updateUserInfo(Long id, User user) {
        User existingUser = userMapper.selectById(id);
        if (existingUser == null) {
            throw new RuntimeException("用户不存在");
        }

        user.setId(id);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        UserInfo userInfo = new UserInfo();
        BeanUtils.copyProperties(user, userInfo);
        return userInfo;
    }

    @Override
    public void deleteUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        userMapper.deleteById(id);
    }

    @Override
    public List<UserInfo> getAllUsers() {
        List<User> users = userMapper.selectAll();
        return users.stream().map(user -> {
            UserInfo userInfo = new UserInfo();
            BeanUtils.copyProperties(user, userInfo);
            return userInfo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<UserInfo> getUsersByType(String userType) {
        List<User> users = userMapper.selectByUserType(userType);
        return users.stream().map(user -> {
            UserInfo userInfo = new UserInfo();
            BeanUtils.copyProperties(user, userInfo);
            return userInfo;
        }).collect(Collectors.toList());
    }
}