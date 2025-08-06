<template>
  <div class="avatar-upload">
    <div class="avatar-container" @click="triggerFileInput">
      <img 
        v-if="avatarUrl" 
        :src="avatarUrl" 
        :alt="userName" 
        class="avatar-image"
      />
      <div v-else class="default-avatar">
        {{ defaultAvatarText }}
      </div>
      <div class="upload-overlay">
        <i class="upload-icon">📷</i>
        <span class="upload-text">更换头像</span>
      </div>
    </div>
    
    <input 
      ref="fileInput"
      type="file" 
      accept="image/*" 
      @change="handleFileSelect"
      style="display: none"
    />
    
    <div v-if="uploading" class="upload-progress">
      <div class="progress-bar">
        <div class="progress-fill" :style="{ width: uploadProgress + '%' }"></div>
      </div>
      <span class="progress-text">上传中... {{ uploadProgress }}%</span>
    </div>
    
    <div class="avatar-actions">
      <button 
        v-if="avatarUrl" 
        @click="deleteAvatar" 
        class="delete-btn"
        :disabled="uploading"
      >
        删除头像
      </button>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, watch } from 'vue'
import { useUserStore } from '@/stores/user'

export default {
  name: 'AvatarUpload',
  props: {
    userId: {
      type: Number,
      required: true
    },
    userName: {
      type: String,
      default: '用户'
    },
    size: {
      type: String,
      default: 'large' // large, medium, small
    }
  },
  emits: ['avatar-updated'],
  setup(props, { emit }) {
    const userStore = useUserStore()
    const fileInput = ref(null)
    const uploading = ref(false)
    const uploadProgress = ref(0)
    const avatarUrl = ref('')
    
    // 初始化头像URL
    const initAvatarUrl = () => {
      if (userStore.user?.avatar) {
        // 如果头像URL不是完整的HTTP URL，则添加服务器前缀
        const avatar = userStore.user.avatar
        if (avatar.startsWith('http')) {
          avatarUrl.value = avatar
        } else {
          avatarUrl.value = `http://localhost:8080${avatar}`
        }
      } else {
        avatarUrl.value = ''
      }
    }
    
    // 组件挂载时初始化头像URL
    onMounted(() => {
      initAvatarUrl()
      console.log('AvatarUpload mounted:', {
        userId: props.userId,
        userName: props.userName,
        userStoreAvatar: userStore.user?.avatar,
        computedAvatarUrl: avatarUrl.value
      })
    })
    
    // 监听用户store变化
    watch(() => userStore.user?.avatar, (newAvatar) => {
      console.log('User avatar changed:', newAvatar)
      initAvatarUrl()
    })

    const defaultAvatarText = computed(() => {
      return props.userName.charAt(0).toUpperCase()
    })

    const triggerFileInput = () => {
      if (!uploading.value) {
        fileInput.value.click()
      }
    }

    const handleFileSelect = async (event) => {
      const file = event.target.files[0]
      if (!file) return

      // 验证文件类型
      if (!file.type.startsWith('image/')) {
        alert('请选择图片文件')
        return
      }

      // 验证文件大小 (5MB)
      if (file.size > 5 * 1024 * 1024) {
        alert('文件大小不能超过5MB')
        return
      }

      await uploadAvatar(file)
    }

    const uploadAvatar = async (file) => {
      uploading.value = true
      uploadProgress.value = 0

      const formData = new FormData()
      formData.append('file', file)
      formData.append('userId', props.userId)

      try {
        const xhr = new XMLHttpRequest()
        
        // 监听上传进度
        xhr.upload.addEventListener('progress', (event) => {
          if (event.lengthComputable) {
            uploadProgress.value = Math.round((event.loaded / event.total) * 100)
          }
        })

        const response = await new Promise((resolve, reject) => {
          xhr.onload = () => {
            if (xhr.status === 200) {
              resolve(JSON.parse(xhr.responseText))
            } else {
              reject(new Error('上传失败'))
            }
          }
          xhr.onerror = () => reject(new Error('网络错误'))
          
          xhr.open('POST', '/api/upload/avatar')
          xhr.send(formData)
        })

        if (response.code === 200) {
          const serverAvatarUrl = response.data.avatarUrl
          const fullAvatarUrl = `http://localhost:8080${serverAvatarUrl}`
          
          avatarUrl.value = fullAvatarUrl
          
          // 更新用户store中的头像（保存相对路径）
          if (userStore.user) {
            userStore.user.avatar = serverAvatarUrl
          }
          
          emit('avatar-updated', serverAvatarUrl)
          alert('头像上传成功！')
        } else {
          throw new Error(response.message || '上传失败')
        }
      } catch (error) {
        console.error('头像上传失败:', error)
        alert('头像上传失败: ' + error.message)
      } finally {
        uploading.value = false
        uploadProgress.value = 0
        // 清空文件输入
        fileInput.value.value = ''
      }
    }

    const deleteAvatar = async () => {
      if (!confirm('确定要删除头像吗？')) return

      try {
        const response = await fetch(`/api/upload/avatar/${props.userId}`, {
          method: 'DELETE'
        })
        
        const result = await response.json()
        
        if (result.code === 200) {
          avatarUrl.value = ''
          
          // 更新用户store中的头像
          if (userStore.user) {
            userStore.user.avatar = null
          }
          
          emit('avatar-updated', null)
          alert('头像删除成功！')
        } else {
          throw new Error(result.message || '删除失败')
        }
      } catch (error) {
        console.error('删除头像失败:', error)
        alert('删除头像失败: ' + error.message)
      }
    }

    return {
      fileInput,
      uploading,
      uploadProgress,
      avatarUrl,
      defaultAvatarText,
      triggerFileInput,
      handleFileSelect,
      deleteAvatar
    }
  }
}
</script>

<style scoped>
.avatar-upload {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 15px;
}

.avatar-container {
  position: relative;
  cursor: pointer;
  border-radius: 50%;
  overflow: hidden;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.avatar-container:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
}

.avatar-image {
  width: 120px;
  height: 120px;
  object-fit: cover;
  display: block;
}

.default-avatar {
  width: 120px;
  height: 120px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  font-weight: bold;
}

.upload-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
  color: white;
}

.avatar-container:hover .upload-overlay {
  opacity: 1;
}

.upload-icon {
  font-size: 24px;
  margin-bottom: 5px;
}

.upload-text {
  font-size: 12px;
  text-align: center;
}

.upload-progress {
  width: 200px;
  text-align: center;
}

.progress-bar {
  width: 100%;
  height: 6px;
  background: #f0f0f0;
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 8px;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #4CAF50, #45a049);
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 12px;
  color: #666;
}

.avatar-actions {
  display: flex;
  gap: 10px;
}

.delete-btn {
  padding: 8px 16px;
  background: #ff4757;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  transition: background 0.3s ease;
}

.delete-btn:hover:not(:disabled) {
  background: #ff3742;
}

.delete-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

/* 响应式尺寸 */
.avatar-upload.small .avatar-image,
.avatar-upload.small .default-avatar {
  width: 60px;
  height: 60px;
}

.avatar-upload.small .default-avatar {
  font-size: 24px;
}

.avatar-upload.medium .avatar-image,
.avatar-upload.medium .default-avatar {
  width: 80px;
  height: 80px;
}

.avatar-upload.medium .default-avatar {
  font-size: 32px;
}
</style>