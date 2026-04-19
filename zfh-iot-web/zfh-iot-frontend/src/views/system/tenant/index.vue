<template>
  <div class="app-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <span>租户管理</span>
            <el-input
              v-model="queryParams.keyword"
              placeholder="请输入租户编码/名称"
              clearable
              style="width: 250px; margin-left: 20px"
              @keyup.enter="handleSearch"
            >
              <template #append>
                <el-button @click="handleSearch">
                  <el-icon><Search /></el-icon>
                </el-button>
              </template>
            </el-input>
          </div>
          <el-button v-permission="'tenant:create'" type="primary" @click="handleAdd">新增租户</el-button>
        </div>
      </template>
      
      <el-table :data="tenantList" v-loading="loading" stripe>
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="tenantCode" label="租户编码" width="120" />
        <el-table-column prop="tenantName" label="租户名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="contactName" label="联系人" width="100" />
        <el-table-column prop="contactPhone" label="联系电话" width="120" />
        <el-table-column prop="maxDevices" label="设备配额" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ row.maxDevices }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="maxGateways" label="网关配额" width="100">
          <template #default="{ row }">
            <el-tag size="small" type="warning">{{ row.maxGateways }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="storageDays" label="存储天数" width="90" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="(val) => handleStatusChange(row, val)"
              v-permission="'tenant:update'"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'tenant:update'" type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button v-permission="'tenant:delete'" type="danger" link @click="handleDelete(row)">删除</el-button>
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
      :title="isEdit ? '编辑租户' : '新增租户'"
      width="600px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="租户编码" prop="tenantCode">
              <el-input 
                v-model="formData.tenantCode" 
                placeholder="请输入租户编码" 
                :disabled="isEdit"
                maxlength="64"
                show-word-limit
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="租户名称" prop="tenantName">
              <el-input 
                v-model="formData.tenantName" 
                placeholder="请输入租户名称"
                maxlength="128"
                show-word-limit
              />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="联系人" prop="contactName">
              <el-input v-model="formData.contactName" placeholder="请输入联系人" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="contactPhone">
              <el-input v-model="formData.contactPhone" placeholder="请输入联系电话" maxlength="20" />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="设备配额" prop="maxDevices">
              <el-input-number 
                v-model="formData.maxDevices" 
                :min="1" 
                :max="100000" 
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="网关配额" prop="maxGateways">
              <el-input-number 
                v-model="formData.maxGateways" 
                :min="1" 
                :max="10000" 
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="存储天数" prop="storageDays">
              <el-input-number 
                v-model="formData.storageDays" 
                :min="30" 
                :max="3650" 
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="formData.status">
                <el-radio :label="1">启用</el-radio>
                <el-radio :label="0">禁用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getTenantList, createTenant, updateTenant, deleteTenant, updateTenantStatus } from '@/api/tenant'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const tenantList = ref([])
const total = ref(0)
const formRef = ref(null)

const queryParams = reactive({
  page: 1,
  size: 20,
  keyword: ''
})

const formData = reactive({
  id: null,
  tenantCode: '',
  tenantName: '',
  contactName: '',
  contactPhone: '',
  maxDevices: 1000,
  maxGateways: 100,
  storageDays: 365,
  status: 1
})

const formRules = {
  tenantCode: [
    { required: true, message: '请输入租户编码', trigger: 'blur' },
    { min: 2, max: 64, message: '长度在 2 到 64 个字符', trigger: 'blur' }
  ],
  tenantName: [
    { required: true, message: '请输入租户名称', trigger: 'blur' },
    { min: 2, max: 128, message: '长度在 2 到 128 个字符', trigger: 'blur' }
  ],
  maxDevices: [
    { required: true, message: '请输入设备配额', trigger: 'blur' }
  ],
  maxGateways: [
    { required: true, message: '请输入网关配额', trigger: 'blur' }
  ]
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getTenantList(queryParams)
    tenantList.value = res.list || []
    total.value = res.total || 0
  } catch (error) {
    console.error('Failed to fetch tenant list:', error)
  } finally {
    loading.value = false
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

const resetForm = () => {
  formData.id = null
  formData.tenantCode = ''
  formData.tenantName = ''
  formData.contactName = ''
  formData.contactPhone = ''
  formData.maxDevices = 1000
  formData.maxGateways = 100
  formData.storageDays = 365
  formData.status = 1
}

const handleAdd = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  resetForm()
  formData.id = row.id
  formData.tenantCode = row.tenantCode
  formData.tenantName = row.tenantName
  formData.contactName = row.contactName
  formData.contactPhone = row.contactPhone
  formData.maxDevices = row.maxDevices
  formData.maxGateways = row.maxGateways
  formData.storageDays = row.storageDays
  formData.status = row.status
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  
  submitLoading.value = true
  try {
    if (isEdit.value) {
      await updateTenant(formData.id, formData)
      ElMessage.success('更新成功')
    } else {
      await createTenant(formData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (error) {
    console.error('Failed to submit:', error)
  } finally {
    submitLoading.value = false
  }
}

const handleStatusChange = async (row, status) => {
  try {
    await updateTenantStatus(row.id, status)
    ElMessage.success(status === 1 ? '已启用' : '已禁用')
  } catch (error) {
    console.error('Failed to update status:', error)
    // 恢复状态
    row.status = status === 1 ? 0 : 1
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm(
    `确定要删除租户 "${row.tenantName}" 吗？删除后该租户下的所有数据将无法访问，请谨慎操作！`,
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    await deleteTenant(row.id)
    ElMessage.success('删除成功')
    fetchData()
  }).catch(() => {})
}

onMounted(() => {
  fetchData()
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
}
</style>
