package com.taxi.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.taxi.service.UserService;
import com.taxi.common.Result;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
@CrossOrigin(origins = "*")
public class FileUploadController {

    @Autowired
    private UserService userService;

    @Value("${file.upload.path:uploads/}")
    private String uploadPath;

    @PostMapping("/avatar")
    public ResponseEntity<Result<Map<String, String>>> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") Long userId) {
        
        try {
            // 验证文件
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Result.error("文件不能为空"));
            }

            // 验证文件类型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(Result.error("只能上传图片文件"));
            }

            // 验证文件大小 (5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(Result.error("文件大小不能超过5MB"));
            }

            // 创建上传目录
            File uploadDir = new File(uploadPath + "avatars/");
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".") 
                ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                : ".jpg";
            String filename = UUID.randomUUID().toString() + extension;

            // 保存文件
            Path filePath = Paths.get(uploadDir.getAbsolutePath(), filename);
            Files.copy(file.getInputStream(), filePath);

            // 生成访问URL
            String avatarUrl = "/uploads/avatars/" + filename;

            // 更新用户头像URL
            userService.updateUserAvatar(userId, avatarUrl);

            Map<String, String> result = new HashMap<>();
            result.put("avatarUrl", avatarUrl);
            result.put("message", "头像上传成功");

            return ResponseEntity.ok(Result.success(result));

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Result.error("文件上传失败: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Result.error("系统错误: " + e.getMessage()));
        }
    }

    @DeleteMapping("/avatar/{userId}")
    public ResponseEntity<Result<String>> deleteAvatar(@PathVariable Long userId) {
        try {
            // 获取用户当前头像URL
            String currentAvatarUrl = userService.getUserAvatarUrl(userId);
            
            if (currentAvatarUrl != null && !currentAvatarUrl.isEmpty()) {
                // 删除文件
                String filename = currentAvatarUrl.substring(currentAvatarUrl.lastIndexOf("/") + 1);
                Path filePath = Paths.get(uploadPath + "avatars/", filename);
                Files.deleteIfExists(filePath);
                
                // 清空数据库中的头像URL
                userService.updateUserAvatar(userId, null);
            }

            return ResponseEntity.ok(Result.success("头像删除成功"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Result.error("删除头像失败: " + e.getMessage()));
        }
    }
}