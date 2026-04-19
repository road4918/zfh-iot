<template>
  <div class="app-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <span>用户管理</span>
            <el-input
              v-model="queryParams.keyword"
              placeholder="请输入用户名/真实姓名"
              clearable
              style="width: 200px; margin-left: 20px"
              @keyup.enter="handleSearch"
            />
            <el-select
              v-if="isPlatformUser"
              v-model="queryParams.userType"
              placeholder="用户类型"
              clearable
              style="width: 120px; margin-left: 10px"
              @change="handleSearch"
            >
              <el-option label="平台用户" :value="0" />
              <el-option label="租户用户" :value="1" />
            </el-select>
            <el-select
              v-if="isPlatformUser"
              v-model="queryParams.tenantId"
              placeholder="所属租户"
              clearable
              style="width: 150px; margin-left: 10px"
              @change="handleSearch"
            >
              <el-option
                v-for="tenant in tenantOptions"
                :key="tenant.id"
                :label="tenant.tenantName"
                :value="tenant.id"
              />
            </el-select>
            <el-button style="margin-left: 10px" @click="handleSearch">
              <el-icon><Search /></el-icon>
            </el-button>
          </div>
          <el-button v-permission="'user:create'" type="primary" @click="handleAdd">新增用户</el-button>
        </div>
      </template>
      
      <el-table :data="userList" v-loading="loading" stripe>
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="realName" label="真实姓名" />
        <el-table-column prop="userType" label="用户类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.userType === 0 ? 'primary' : 'success'">
              {{ row.userType === 0 ? '平台' : '租户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="tenantName" label="所属租户" width="150">
          <template #default="{ row }">
            <span v-if="row.userType === 0">-</span>
            <span v-else>{{ row.tenantName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.username !== 'admin'" v-permission="'user:update'" type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button v-if="row.username !== 'admin'" v-permission="'user:reset-password'" type="warning" link @click="handleResetPassword(row)">重置密码</el-button>
            <el-button v-if="row.username !== 'admin'" v-permission="'user:delete'" type="danger" link @click="handleDelete(row)">删除</el-button>
            <span v-if="row.username === 'admin'" class="admin-tag">系统管理员</span>
          </template>
        </el-table-column>
      </el-table>
      
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="queryParams.page"
          v-model:page-size="queryParams.size"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
    
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑用户' : '新增用户'"
      width="550px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="用户类型" prop="userType">
          <el-radio-group v-model="formData.userType" :disabled="isEdit" @change="handleUserTypeChange">
            <el-radio :label="0" v-if="isPlatformUser">平台用户</el-radio>
            <el-radio :label="1">租户用户</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item 
          label="所属租户" 
          prop="tenantId" 
          v-if="formData.userType === 1"
          :rules="[{ required: true, message: '请选择所属租户', trigger: 'change' }]"
        >
          <el-select 
            v-model="formData.tenantId" 
            placeholder="请选择租户" 
            style="width: 100%"
            :disabled="isEdit || !isPlatformUser"
          >
            <el-option
              v-for="tenant in tenantOptions"
              :key="tenant.id"
              :label="tenant.tenantName"
              :value="tenant.id"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="用户名" prop="username">
          <el-input v-model="formData.username" placeholder="请输入用户名" :disabled="isEdit" />
        </el-form-item>
        
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="formData.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="formData.phone" placeholder="请输入手机号" />
        </el-form-item>
        
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="formData.email" placeholder="请输入邮箱" />
        </el-form-item>
        
        <el-form-item label="角色" prop="roleIds">
          <el-select v-model="formData.roleIds" multiple placeholder="请选择角色" style="width: 100%">
            <el-option
              v-for="role in roleOptions"
              :key="role.id"
              :label="role.roleName"
              :value="role.id"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getUserList, createUser, updateUser, deleteUser, resetUserPassword, getUserRoles } from '@/api/user'
import { getRoleList } from '@/api/role'
import { getTenantList } from '@/api/tenant'
import { useUserStore } from '@/stores/user'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const userList = ref([])
const total = ref(0)
const roleOptions = ref([])
const tenantOptions = ref([])
const formRef = ref(null)
const userStore = useUserStore()

const isPlatformUser = computed(() => {
  return Number(userStore.userInfo?.userType) === 0
})

const queryParams = reactive({
  page: 1,
  size: 20,
  keyword: '',
  userType: null,
  tenantId: null
})

const formData = reactive({
  id: null,
  userType: 1,
  tenantId: null,
  username: '',
  realName: '',
  phone: '',
  email: '',
  password: '',
  roleIds: [],
  status: 1
})

const formRules = {
  userType: [
    { required: true, message: '请选择用户类型', trigger: 'change' }
  ],
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 64, message: '长度在 3 到 64 个字符', trigger: 'blur' }
  ],
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' }
  ],
  roleIds: [
    { required: true, message: '请选择角色', trigger: 'change', type: 'array' }
  ]
}

const fetchData = async () => {
  loading.value = true
  try {
    const params = { ...queryParams }
    if (!isPlatformUser.value) {
      delete params.userType
      delete params.tenantId
    }
    const res = await getUserList(params)
    userList.value = res.list || []
    total.value = res.total || 0
  } catch (error) {
    console.error('Failed to fetch user list:', error)
  } finally {
    loading.value = false
  }
}

const fetchRoles = async () => {
  try {
    const params = isPlatformUser.value ? {} : { roleType: 1 }
    const res = await getRoleList({ page: 1, size: 100, ...params })
    roleOptions.value = res.list || []
  } catch (error) {
    console.error('Failed to fetch roles:', error)
  }
}

const fetchTenants = async () => {
  if (!isPlatformUser.value) return
  try {
    const res = await getTenantList({ page: 1, size: 1000, status: 1 })
    tenantOptions.value = res.list || []
  } catch (error) {
    console.error('Failed to fetch tenants:', error)
  }
}

const handleSearch = () => {
  queryParams.page = 1
  fetchData()
}

const handleSizeChange = (val) => {
  queryParams.size = val
  fetchData()
}

const handleCurrentChange = (val) => {
  queryParams.page = val
  fetchData()
}

const handleUserTypeChange = (val) => {
  if (val === 0) {
    formData.tenantId = null
  }
}

const resetForm = () => {
  formData.id = null
  formData.userType = isPlatformUser.value ? 0 : 1
  formData.tenantId = isPlatformUser.value ? null : userStore.userInfo?.tenantId
  formData.username = ''
  formData.realName = ''
  formData.phone = ''
  formData.email = ''
  formData.password = ''
  formData.roleIds = []
  formData.status = 1
}

const handleAdd = () => {
  isEdit.value = false
  resetForm()
  fetchRoles()
  dialogVisible.value = true
}

const handleEdit = async (row) => {
  isEdit.value = true
  resetForm()
  formData.id = row.id
  formData.userType = row.userType
  formData.tenantId = row.tenantId
  formData.username = row.username
  formData.realName = row.realName
  formData.phone = row.phone
  formData.email = row.email
  formData.status = row.status
  
  await fetchRoles()
  
  try {
    const roleRes = await getUserRoles(row.id)
    formData.roleIds = roleRes || []
  } catch (error) {
    console.error('Failed to fetch user roles:', error)
  }
  
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  
  submitLoading.value = true
  try {
    const submitData = { ...formData }
    if (submitData.userType === 0) {
      submitData.tenantId = null
    }
    
    if (isEdit.value) {
      await updateUser(submitData.id, submitData)
      ElMessage.success('更新成功')
      dialogVisible.value = false
    } else {
      const res = await createUser(submitData)
      const generatedPassword = res.password
      dialogVisible.value = false
      ElMessageBox.alert(
        `用户创建成功！<br><br>用户名：<strong>${submitData.username}</strong><br>初始密码：<strong style="color: #f56c6c; font-size: 16px;">${generatedPassword}</strong><br><br>请妥善保管密码并及时通知用户`,
        '创建成功',
        {
          confirmButtonText: '确定',
          dangerouslyUseHTMLString: true,
          type: 'success'
        }
      )
    }
    fetchData()
  } catch (error) {
    console.error('Failed to submit:', error)
  } finally {
    submitLoading.value = false
  }
}

const handleResetPassword = (row) => {
  ElMessageBox.confirm(
    `确定要重置用户 "${row.username}" 的密码吗？`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    const res = await resetUserPassword(row.id)
    const newPassword = res.password
    ElMessageBox.alert(
      `密码重置成功！<br><br>用户名：<strong>${row.username}</strong><br>新密码：<strong style="color: #f56c6c; font-size: 16px;">${newPassword}</strong><br><br>请妥善保管密码并及时通知用户`,
      '重置成功',
      {
        confirmButtonText: '确定',
        dangerouslyUseHTMLString: true,
        type: 'success'
      }
    )
  }).catch(() => {})
}

const handleDelete = (row) => {
  ElMessageBox.confirm(
    `确定要删除用户 "${row.username}" 吗？`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    await deleteUser(row.id)
    ElMessage.success('删除成功')
    fetchData()
  }).catch(() => {})
}

onMounted(() => {
  fetchData()
  fetchTenants()
})
</script>

<style scoped lang="scss">
.app-container {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .header-left {
      display: flex;
      align-items: center;
    }
  }
  
  .pagination-container {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
  
  .admin-tag {
    color: #909399;
    font-size: 13px;
  }
}
</style>
