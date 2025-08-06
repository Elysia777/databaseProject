<template>
  <div class="profile">
    <el-card class="profile-card">
      <template #header>
        <div class="card-header">
          <span>个人设置</span>
        </div>
      </template>
      
      <el-form
        ref="profileFormRef"
        :model="profileForm"
        :rules="profileRules"
        label-width="100px"
        class="profile-form"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="头像" prop="avatar">
              <AvatarUpload 
                v-if="userStore.user"
                :userId="userStore.user.id" 
                :userName="profileForm.realName || userStore.user.username"
                size="large"
                @avatar-updated="handleAvatarUpdated"
              />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model="profileForm.realName" placeholder="请输入真实姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="profileForm.phone" placeholder="请输入手机号" />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别" prop="gender">
              <el-select v-model="profileForm.gender" placeholder="请选择性别" style="width: 100%">
                <el-option label="男" value="MALE" />
                <el-option label="女" value="FEMALE" />
                <el-option label="其他" value="OTHER" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-form-item label="个人简介" prop="bio">
          <el-input
            v-model="profileForm.bio"
            type="textarea"
            :rows="3"
            placeholder="请输入个人简介"
          />
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="handleSaveProfile" :loading="saveLoading">
            保存修改
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    
    <!-- 修改密码卡片 -->
    <el-card class="password-card">
      <template #header>
        <div class="card-header">
          <span>修改密码</span>
        </div>
      </template>
      
      <el-form
        ref="passwordFormRef"
        :model="passwordForm"
        :rules="passwordRules"
        label-width="100px"
        class="password-form"
      >
        <el-form-item label="当前密码" prop="oldPassword">
          <el-input
            v-model="passwordForm.oldPassword"
            type="password"
            placeholder="请输入当前密码"
            show-password
          />
        </el-form-item>
        
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            placeholder="请输入新密码"
            show-password
          />
        </el-form-item>
        
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            show-password
          />
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="handleChangePassword" :loading="passwordLoading">
            修改密码
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import AvatarUpload from '@/components/AvatarUpload.vue'

const userStore = useUserStore()

// 表单引用
const profileFormRef = ref()
const passwordFormRef = ref()

// 加载状态
const saveLoading = ref(false)
const passwordLoading = ref(false)

// 个人资料表单
const profileForm = reactive({
  realName: '',
  phone: '',
  email: '',
  gender: '',
  bio: '',
  avatar: ''
})

// 修改密码表单
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 表单验证规则
const profileRules = {
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ]
}

const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 处理头像更新
const handleAvatarUpdated = (avatarUrl) => {
  profileForm.avatar = avatarUrl
  // 更新用户store中的头像
  if (userStore.user) {
    userStore.user.avatar = avatarUrl
  }
  ElMessage.success('头像更新成功！')
}

// 保存个人资料
const handleSaveProfile = async () => {
  try {
    await profileFormRef.value.validate()
    saveLoading.value = true
    
    // 调用用户信息更新API
    const response = await fetch(`http://localhost:8080/api/auth/user/${userStore.user.id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${userStore.token}`
      },
      body: JSON.stringify({
        realName: profileForm.realName,
        phone: profileForm.phone,
        email: profileForm.email,
        avatar: profileForm.avatar
      })
    })
    
    const result = await response.json()
    
    if (result.code === 200) {
      // 更新本地用户信息
      Object.assign(userStore.user, {
        realName: profileForm.realName,
        phone: profileForm.phone,
        email: profileForm.email,
        avatar: profileForm.avatar
      })
      ElMessage.success('个人资料保存成功')
    } else {
      throw new Error(result.message || '保存失败')
    }
  } catch (error) {
    console.error('保存个人资料失败:', error)
    ElMessage.error('保存失败: ' + error.message)
  } finally {
    saveLoading.value = false
  }
}

// 修改密码
const handleChangePassword = async () => {
  try {
    await passwordFormRef.value.validate()
    passwordLoading.value = true
    
    // 调用修改密码API
    const response = await fetch('http://localhost:8080/api/auth/change-password', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${userStore.token}`
      },
      body: JSON.stringify({
        userId: userStore.user.id,
        oldPassword: passwordForm.oldPassword,
        newPassword: passwordForm.newPassword
      })
    })
    
    const result = await response.json()
    
    if (result.code === 200) {
      ElMessage.success('密码修改成功')
      passwordForm.oldPassword = ''
      passwordForm.newPassword = ''
      passwordForm.confirmPassword = ''
    } else {
      throw new Error(result.message || '密码修改失败')
    }
  } catch (error) {
    console.error('修改密码失败:', error)
    ElMessage.error('密码修改失败: ' + error.message)
  } finally {
    passwordLoading.value = false
  }
}

// 重置表单
const handleReset = () => {
  profileFormRef.value.resetFields()
  loadUserProfile()
}

// 加载用户资料
const loadUserProfile = async () => {
  if (userStore.user) {
    try {
      // 从服务器获取最新的用户信息
      const response = await fetch(`http://localhost:8080/api/auth/user/${userStore.user.id}`, {
        headers: {
          'Authorization': `Bearer ${userStore.token}`
        }
      })
      
      const result = await response.json()
      
      if (result.code === 200) {
        const userData = result.data
        Object.assign(profileForm, {
          realName: userData.realName || '',
          phone: userData.phone || '',
          email: userData.email || '',
          gender: userData.gender || '',
          bio: userData.bio || '',
          avatar: userData.avatar || ''
        })
        
        // 更新本地用户信息
        Object.assign(userStore.user, userData)
      }
    } catch (error) {
      console.error('加载用户资料失败:', error)
      // 如果API调用失败，使用本地数据
      Object.assign(profileForm, {
        realName: userStore.user.realName || '',
        phone: userStore.user.phone || '',
        email: userStore.user.email || '',
        gender: userStore.user.gender || '',
        bio: userStore.user.bio || '',
        avatar: userStore.user.avatar || ''
      })
    }
  }
}

onMounted(() => {
  loadUserProfile()
})
</script>

<style scoped>
.profile {
  padding: 20px;
}

.profile-card {
  margin-bottom: 20px;
}

.password-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.profile-form,
.password-form {
  max-width: 600px;
}

/* 头像上传组件样式已在AvatarUpload.vue中定义 */
</style> 