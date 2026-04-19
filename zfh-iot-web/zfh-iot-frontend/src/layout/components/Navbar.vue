<template>
  <div class="navbar">
    <div class="left">
      <div class="hamburger" @click="toggleSidebar">
        <el-icon :size="20">
          <Fold v-if="!appStore.sidebarCollapsed" />
          <Expand v-else />
        </el-icon>
      </div>
    </div>
    <div class="right">
      <el-dropdown @command="handleCommand">
        <span class="user-info">
          {{ userStore.userInfo?.realName || userStore.userInfo?.username || 'User' }}
          <el-icon><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="changePassword">修改密码</el-dropdown-item>
            <el-dropdown-item command="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>

  <el-dialog
    v-model="dialogVisible"
    title="修改密码"
    width="480px"
    destroy-on-close
    @closed="resetForm"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
    >
      <el-form-item label="旧密码" prop="oldPassword">
        <el-input
          v-model="formData.oldPassword"
          type="password"
          show-password
          placeholder="请输入旧密码"
        />
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword">
        <el-input
          v-model="formData.newPassword"
          type="password"
          show-password
          placeholder="8位以上，包含大小写字母和特殊字符"
        />
      </el-form-item>
      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input
          v-model="formData.confirmPassword"
          type="password"
          show-password
          placeholder="请再次输入新密码"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="handleSubmitPassword">
        确定
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import { changeOwnPassword } from '@/api/user'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'

const appStore = useAppStore()
const userStore = useUserStore()
const dialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref(null)

const formData = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateComplexPassword = (_, value, callback) => {
  const passwordPattern = /^(?=.*[a-z])(?=.*[A-Z])(?=.*[^A-Za-z0-9]).{8,}$/

  if (!value) {
    callback(new Error('请输入新密码'))
    return
  }

  if (!passwordPattern.test(value)) {
    callback(new Error('密码需8位以上，包含大小写字母和特殊字符'))
    return
  }

  callback()
}

const validateConfirmPassword = (_, value, callback) => {
  if (!value) {
    callback(new Error('请再次输入新密码'))
    return
  }

  if (value !== formData.newPassword) {
    callback(new Error('两次输入的新密码不一致'))
    return
  }

  callback()
}

const formRules = {
  oldPassword: [
    { required: true, message: '请输入旧密码', trigger: 'blur' }
  ],
  newPassword: [
    { validator: validateComplexPassword, trigger: 'blur' }
  ],
  confirmPassword: [
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const toggleSidebar = () => {
  appStore.toggleSidebar()
}

const resetForm = () => {
  formRef.value?.resetFields()
}

const handleSubmitPassword = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitLoading.value = true
  try {
    await changeOwnPassword({ ...formData })
    dialogVisible.value = false
    await ElMessageBox.alert('密码修改成功，请重新登录。', '提示', {
      type: 'success',
      confirmButtonText: '确定'
    })
    userStore.logout()
  } finally {
    submitLoading.value = false
  }
}

const handleCommand = (command) => {
  if (command === 'changePassword') {
    dialogVisible.value = true
    return
  }

  if (command === 'logout') {
    userStore.logout()
  }
}
</script>

<style scoped lang="scss">
.navbar {
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 15px;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);

  .left {
    display: flex;
    align-items: center;

    .hamburger {
      padding: 0 15px;
      cursor: pointer;

      &:hover {
        background: rgba(0, 0, 0, 0.025);
      }
    }
  }

  .right {
    .user-info {
      cursor: pointer;
      display: flex;
      align-items: center;

      .el-icon {
        margin-left: 5px;
      }
    }
  }
}
</style>
