<template>
  <div class="app-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <span>角色管理</span>
            <el-select
              v-if="isPlatformUser"
              v-model="queryParams.roleType"
              placeholder="角色类型"
              clearable
              style="width: 120px; margin-left: 20px"
              @change="handleSearch"
            >
              <el-option label="平台角色" :value="0" />
              <el-option label="租户角色" :value="1" />
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
          <el-button v-permission="'role:create'" type="primary" @click="handleAdd">新增角色</el-button>
        </div>
      </template>
      
      <el-table :data="roleList" v-loading="loading" stripe>
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="roleCode" label="角色编码" />
        <el-table-column prop="roleName" label="角色名称" />
        <el-table-column prop="roleType" label="角色类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.roleType === 0 ? 'primary' : 'success'">
              {{ row.roleType === 0 ? '平台' : '租户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="tenantName" label="所属租户" width="150">
          <template #default="{ row }">
            <span v-if="row.roleType === 0">-</span>
            <span v-else>{{ row.tenantName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.roleCode !== 'admin'" v-permission="'role:update'" type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button v-if="row.roleCode !== 'admin'" v-permission="'role:assign-permission'" type="primary" link @click="handlePermission(row)">分配权限</el-button>
            <el-button v-if="row.roleCode !== 'admin'" v-permission="'role:delete'" type="danger" link @click="handleDelete(row)">删除</el-button>
            <span v-if="row.roleCode === 'admin'" class="admin-tag">系统管理员</span>
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
    
    <!-- 新增/编辑角色对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑角色' : '新增角色'"
      width="550px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="角色类型" prop="roleType">
          <el-radio-group v-model="formData.roleType" :disabled="isEdit" @change="handleRoleTypeChange">
            <el-radio :label="0" v-if="isPlatformUser">平台角色</el-radio>
            <el-radio :label="1">租户角色</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item 
          label="所属租户" 
          prop="tenantId" 
          v-if="formData.roleType === 1"
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
        
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="formData.roleCode" placeholder="请输入角色编码" :disabled="isEdit" />
        </el-form-item>
        
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="formData.roleName" placeholder="请输入角色名称" />
        </el-form-item>
        
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="3"
            placeholder="请输入角色描述"
          />
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
    
    <!-- 分配权限对话框 -->
    <el-dialog
      v-model="permissionDialogVisible"
      title="分配权限"
      width="700px"
      destroy-on-close
    >
      <div class="permission-tree-header">
        <el-checkbox v-model="expandAll" @change="handleExpandAll">展开/折叠全部</el-checkbox>
        <el-checkbox v-model="checkAll" @change="handleCheckAll">全选/取消全选</el-checkbox>
        <span class="permission-legend">
          <span class="legend-item"><el-icon><Folder /></el-icon> 菜单</span>
          <span class="legend-item"><el-icon><CircleCheck /></el-icon> 按钮</span>
        </span>
      </div>
      <el-tree
        :key="treeKey"
        ref="treeRef"
        :data="permissionTree"
        :props="treeProps"
        :default-checked-keys="checkedPermissions"
        :default-expand-all="expandAll"
        node-key="id"
        show-checkbox
        height="400px"
        @check-change="handleCheckChange"
      >
        <template #default="{ node, data }">
          <span class="custom-tree-node">
            <el-icon v-if="data.permType === 1" class="perm-icon menu-icon"><Folder /></el-icon>
            <el-icon v-else-if="data.permType === 2" class="perm-icon button-icon"><CircleCheck /></el-icon>
            <el-icon v-else class="perm-icon api-icon"><Link /></el-icon>
            <span class="perm-name">{{ data.permName }}</span>
            <el-tag v-if="data.permType === 1" size="small" type="primary" class="perm-tag">菜单</el-tag>
            <el-tag v-else-if="data.permType === 2" size="small" type="warning" class="perm-tag">按钮</el-tag>
            <el-tag v-else size="small" type="info" class="perm-tag">API</el-tag>
          </span>
        </template>
      </el-tree>
      <template #footer>
        <el-button @click="permissionDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handlePermissionSubmit" :loading="permissionLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Folder, CircleCheck, Link, Search } from '@element-plus/icons-vue'
import {
  getRoleList,
  createRole,
  updateRole,
  deleteRole,
  getRolePermissions,
  assignRolePermissions,
  getPermissionTree
} from '@/api/role'
import { getTenantList } from '@/api/tenant'
import { useUserStore } from '@/stores/user'

const loading = ref(false)
const submitLoading = ref(false)
const permissionLoading = ref(false)
const dialogVisible = ref(false)
const permissionDialogVisible = ref(false)
const isEdit = ref(false)
const roleList = ref([])
const total = ref(0)
const currentRoleId = ref(null)
const permissionTree = ref([])
const checkedPermissions = ref([])
const expandedKeys = ref([])
const expandAll = ref(false)
const checkAll = ref(false)
const treeKey = ref(0)
const tenantOptions = ref([])
const formRef = ref(null)
const treeRef = ref(null)
const userStore = useUserStore()

const isPlatformUser = computed(() => {
  return Number(userStore.userInfo?.userType) === 0
})

const queryParams = reactive({
  page: 1,
  size: 20,
  roleType: null,
  tenantId: null
})

const formData = reactive({
  id: null,
  roleType: 1,
  tenantId: null,
  roleCode: '',
  roleName: '',
  description: '',
  status: 1
})

const formRules = {
  roleType: [
    { required: true, message: '请选择角色类型', trigger: 'change' }
  ],
  roleCode: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    { min: 2, max: 64, message: '长度在 2 到 64 个字符', trigger: 'blur' }
  ],
  roleName: [
    { required: true, message: '请输入角色名称', trigger: 'blur' },
    { min: 2, max: 128, message: '长度在 2 到 128 个字符', trigger: 'blur' }
  ]
}

const treeProps = {
  children: 'children',
  label: 'permName'
}

const fetchData = async () => {
  loading.value = true
  try {
    const params = { ...queryParams }
    if (!isPlatformUser.value) {
      delete params.roleType
      delete params.tenantId
    }
    const res = await getRoleList({ page: queryParams.page, size: queryParams.size, ...params })
    roleList.value = res.list || []
    total.value = res.total || 0
  } catch (error) {
    console.error('Failed to fetch role list:', error)
  } finally {
    loading.value = false
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

const handleRoleTypeChange = (val) => {
  if (val === 0) {
    formData.tenantId = null
  }
}

const resetForm = () => {
  formData.id = null
  formData.roleType = isPlatformUser.value ? 0 : 1
  formData.tenantId = null
  formData.roleCode = ''
  formData.roleName = ''
  formData.description = ''
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
  formData.roleType = row.roleType
  formData.tenantId = row.tenantId
  formData.roleCode = row.roleCode
  formData.roleName = row.roleName
  formData.description = row.description
  formData.status = row.status
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  
  submitLoading.value = true
  try {
    const submitData = { ...formData }
    if (submitData.roleType === 0) {
      submitData.tenantId = null
    }
    
    if (isEdit.value) {
      await updateRole(submitData.id, submitData)
      ElMessage.success('更新成功')
    } else {
      await createRole(submitData)
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

const handleDelete = (row) => {
  ElMessageBox.confirm(
    `确定要删除角色 "${row.roleName}" 吗？`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    await deleteRole(row.id)
    ElMessage.success('删除成功')
    fetchData()
  }).catch(() => {})
}

const handlePermission = async (row) => {
  currentRoleId.value = row.id
  permissionDialogVisible.value = true
  expandAll.value = false
  checkAll.value = false
  treeKey.value += 1
  
  try {
    const [treeRes, rolePermsRes] = await Promise.all([
      getPermissionTree(),
      getRolePermissions(row.id)
    ])
    permissionTree.value = treeRes || []
    checkedPermissions.value = rolePermsRes || []
  } catch (error) {
    console.error('Failed to fetch permission data:', error)
  }
}

const getAllIds = (tree) => {
  const ids = []
  const traverse = (nodes) => {
    nodes.forEach(node => {
      ids.push(node.id)
      if (node.children && node.children.length > 0) {
        traverse(node.children)
      }
    })
  }
  traverse(tree)
  return ids
}

const handleExpandAll = (val) => {
  expandAll.value = val
  treeKey.value += 1
}

const handleCheckChange = () => {
  const checkedKeys = treeRef.value.getCheckedKeys()
  const allIds = getAllIds(permissionTree.value)
  checkAll.value = checkedKeys.length === allIds.length && allIds.length > 0
}

const handleCheckAll = (val) => {
  if (val) {
    const allIds = getAllIds(permissionTree.value)
    treeRef.value.setCheckedKeys(allIds)
  } else {
    treeRef.value.setCheckedKeys([])
  }
}

const handlePermissionSubmit = async () => {
  const checkedKeys = treeRef.value.getCheckedKeys(false)
  const halfCheckedKeys = treeRef.value.getHalfCheckedKeys()
  const allCheckedKeys = [...new Set([...checkedKeys, ...halfCheckedKeys])]
  
  permissionLoading.value = true
  try {
    await assignRolePermissions(currentRoleId.value, allCheckedKeys)
    ElMessage.success('权限分配成功')
    permissionDialogVisible.value = false
  } catch (error) {
    console.error('Failed to assign permissions:', error)
  } finally {
    permissionLoading.value = false
  }
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
}

.permission-tree-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 15px;
  padding-bottom: 15px;
  border-bottom: 1px solid #e4e7ed;

  .permission-legend {
    margin-left: auto;
    display: flex;
    gap: 15px;

    .legend-item {
      display: flex;
      align-items: center;
      gap: 5px;
      font-size: 13px;
      color: #606266;

      .el-icon {
        font-size: 14px;
      }
    }
  }
}

.custom-tree-node {
  display: flex;
  align-items: center;
  gap: 8px;

  .perm-icon {
    font-size: 16px;

    &.menu-icon {
      color: #409eff;
    }

    &.button-icon {
      color: #e6a23c;
    }

    &.api-icon {
      color: #909399;
    }
  }

  .perm-name {
    flex: 1;
  }

  .perm-tag {
    margin-left: 8px;
  }
}

:deep(.el-tree-node__content) {
  height: 36px;
}

:deep(.el-tree) {
  max-height: 400px;
  overflow-y: auto;
}

.admin-tag {
  color: #909399;
  font-size: 13px;
}
</style>
