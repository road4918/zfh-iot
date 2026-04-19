<template>
  <div class="app-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <span>表计管理</span>
            <el-input
              v-model="queryParams.keyword"
              placeholder="请输入表计编号/名称"
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
            <el-select
              v-model="queryParams.meterType"
              placeholder="表计类型"
              clearable
              style="width: 120px; margin-left: 10px"
              @change="handleSearch"
            >
              <el-option
                v-for="item in meterTypes"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
            <el-select
              v-model="queryParams.status"
              placeholder="状态"
              clearable
              style="width: 120px; margin-left: 10px"
              @change="handleSearch"
            >
              <el-option
                v-for="item in deviceStatus"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </div>
          <el-button v-permission="'meter:create'" type="primary" @click="handleAdd">新增表计</el-button>
        </div>
      </template>

      <el-table :data="meterList" v-loading="loading" stripe>
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="meterNo" label="表计编号" width="140" />
        <el-table-column prop="meterName" label="表计名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="meterType" label="类型" width="80">
          <template #default="{ row }">
            <el-tag :color="getMeterTypeColor(row.meterType)" size="small" style="color: #fff; border: none">
              {{ getMeterTypeLabel(row.meterType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="所属网关" width="140">
          <template #default="{ row }">
            <template v-if="row.gatewayId">
              <el-button type="primary" link size="small" @click="handleUnbind(row)">解绑</el-button>
            </template>
            <template v-else>
              <el-button type="warning" link size="small" @click="handleBind(row)">绑定</el-button>
            </template>
          </template>
        </el-table-column>
        <el-table-column label="所属群组" width="140">
          <template #default="{ row }">
            <template v-if="row.groupId">
              <span>{{ getGroupName(row.groupId) }}</span>
              <el-button type="primary" link size="small" style="margin-left: 4px" @click="handleUnbindGroup(row)">解绑</el-button>
            </template>
            <template v-else>
              <el-button type="warning" link size="small" @click="handleBindGroup(row)">绑定</el-button>
            </template>
          </template>
        </el-table-column>
        <el-table-column prop="deviceAddress" label="设备地址" width="100" />
        <el-table-column prop="protocolCode" label="协议" width="90">
          <template #default="{ row }">
            <el-tag size="small">{{ row.protocolCode || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastReadingTime" label="最后抄表" width="170" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'meter:update'" type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button v-permission="'meter:delete'" type="danger" link @click="handleDelete(row)">删除</el-button>
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
      :title="isEdit ? '编辑表计' : '新增表计'"
      width="750px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="110px"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="表计编号" prop="meterNo">
              <el-input
                v-model="formData.meterNo"
                placeholder="请输入表计编号"
                :disabled="isEdit"
                maxlength="64"
                show-word-limit
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="表计名称" prop="meterName">
              <el-input
                v-model="formData.meterName"
                placeholder="请输入表计名称"
                maxlength="128"
                show-word-limit
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="表计类型" prop="meterType">
              <el-select v-model="formData.meterType" placeholder="请选择表计类型" style="width: 100%">
                <el-option
                  v-for="item in meterTypes"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="协议" prop="protocolCode">
              <el-select v-model="formData.protocolCode" placeholder="请选择协议" style="width: 100%">
                <el-option
                  v-for="item in protocolList"
                  :key="item.protocolCode"
                  :label="item.protocolName"
                  :value="item.protocolCode"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="厂商" prop="manufacturerId">
              <el-select v-model="formData.manufacturerId" placeholder="请选择厂商" style="width: 100%">
                <el-option
                  v-for="item in manufacturerList"
                  :key="item.id"
                  :label="item.manufacturerName"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备地址" prop="deviceAddress">
              <el-input v-model="formData.deviceAddress" placeholder="请输入设备地址" maxlength="32" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="所属群组" prop="groupId">
              <el-tree-select
                v-model="formData.groupId"
                :data="buildGroupTree(groupList)"
                :props="{ label: 'groupName', children: 'children', value: 'id' }"
                placeholder="请选择所属群组"
                check-strictly
                clearable
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="CT变比" prop="ctRatio">
              <el-input-number v-model="formData.ctRatio" :min="0.01" :precision="2" :step="0.1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="PT变比" prop="ptRatio">
              <el-input-number v-model="formData.ptRatio" :min="0.01" :precision="2" :step="0.1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="表计倍率" prop="meterRatio">
              <el-input-number v-model="formData.meterRatio" :min="0.01" :precision="2" :step="0.1" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="安装地址" prop="address">
              <el-input v-model="formData.address" placeholder="请输入安装地址" maxlength="256" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="安装日期" prop="installTime">
              <el-date-picker
                v-model="formData.installTime"
                type="date"
                placeholder="请选择安装日期"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="formData.remark"
            type="textarea"
            placeholder="请输入备注"
            maxlength="512"
            show-word-limit
            :rows="3"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="bindDialogVisible"
      title="绑定网关"
      width="500px"
      destroy-on-close
    >
      <el-form label-width="80px">
        <el-form-item label="当前表计">
          <span>{{ bindTarget.meterNo }} - {{ bindTarget.meterName }}</span>
        </el-form-item>
        <el-form-item label="选择网关">
          <el-select v-model="selectedGatewayId" placeholder="请选择网关" style="width: 100%">
            <el-option
              v-for="item in gatewayList"
              :key="item.id"
              :label="`${item.gatewayNo} - ${item.gatewayName}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bindDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitBind" :loading="bindLoading">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="bindGroupDialogVisible"
      title="绑定群组"
      width="500px"
      destroy-on-close
    >
      <el-form label-width="80px">
        <el-form-item label="当前表计">
          <span>{{ bindGroupTarget.meterNo }} - {{ bindGroupTarget.meterName }}</span>
        </el-form-item>
        <el-form-item label="选择群组">
          <el-tree-select
            v-model="selectedGroupId"
            :data="buildGroupTree(groupList)"
            :props="{ label: 'groupName', children: 'children', value: 'id' }"
            placeholder="请选择群组"
            check-strictly
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bindGroupDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitBindGroup">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import config from '@/config'
import { useUserStore } from '@/stores/user'
import { getMeterList, createMeter, updateMeter, deleteMeter, bindGateway, unbindGateway, bindGroup, unbindGroup } from '@/api/meter'
import { getProtocolList } from '@/api/protocol'
import { getManufacturerList } from '@/api/manufacturer'
import { getGatewayList } from '@/api/gateway'
import { getGroupList } from '@/api/group'

const { deviceStatus, meterTypes } = config
const userStore = useUserStore()

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const bindDialogVisible = ref(false)
const bindLoading = ref(false)
const isEdit = ref(false)
const meterList = ref([])
const protocolList = ref([])
const manufacturerList = ref([])
const gatewayList = ref([])
const groupList = ref([])
const total = ref(0)
const formRef = ref(null)
const bindTarget = ref({})
const selectedGatewayId = ref(null)

const queryParams = reactive({
  page: 1,
  size: 20,
  keyword: '',
  status: undefined,
  meterType: undefined
})

const formData = reactive({
  id: null,
  meterNo: '',
  meterName: '',
  meterType: null,
  manufacturerId: null,
  protocolCode: '',
  deviceAddress: '',
  ctRatio: 1.0,
  ptRatio: 1.0,
  meterRatio: 1.0,
  address: '',
  installTime: '',
  groupId: null,
  remark: ''
})

const formRules = {
  meterNo: [
    { required: true, message: '请输入表计编号', trigger: 'blur' },
    { min: 1, max: 64, message: '长度在 1 到 64 个字符', trigger: 'blur' }
  ],
  meterName: [
    { required: true, message: '请输入表计名称', trigger: 'blur' },
    { min: 1, max: 128, message: '长度在 1 到 128 个字符', trigger: 'blur' }
  ],
  meterType: [
    { required: true, message: '请选择表计类型', trigger: 'change' }
  ]
}

const getMeterTypeLabel = (type) => {
  const item = meterTypes.find(t => t.value === type)
  return item ? item.label : '未知'
}

const getMeterTypeColor = (type) => {
  const item = meterTypes.find(t => t.value === type)
  return item ? item.color : '#909399'
}

const getStatusType = (status) => {
  const item = deviceStatus.find(s => s.value === status)
  return item ? item.type : 'info'
}

const getStatusLabel = (status) => {
  const item = deviceStatus.find(s => s.value === status)
  return item ? item.label : '未知'
}

const fetchData = async () => {
  loading.value = true
  try {
    const params = { ...queryParams, tenantId: userStore.effectiveTenantId }
    const res = await getMeterList(params)
    meterList.value = res.list || []
    total.value = res.total || 0
  } catch (error) {
    console.error('Failed to fetch meter list:', error)
  } finally {
    loading.value = false
  }
}

const fetchProtocols = async () => {
  try {
    const res = await getProtocolList({ page: 1, size: 100 })
    protocolList.value = res.list || []
  } catch (error) {
    console.error('Failed to fetch protocol list:', error)
  }
}

const fetchManufacturers = async () => {
  try {
    const res = await getManufacturerList({ page: 1, size: 100 })
    manufacturerList.value = res.list || []
  } catch (error) {
    console.error('Failed to fetch manufacturer list:', error)
  }
}

const fetchGateways = async () => {
  try {
    const res = await getGatewayList({ page: 1, size: 500, tenantId: userStore.effectiveTenantId })
    gatewayList.value = res.list || []
  } catch (error) {
    console.error('Failed to fetch gateway list:', error)
  }
}

const fetchGroups = async () => {
  try {
    const params = {}
    if (userStore.effectiveTenantId) {
      params.tenantId = userStore.effectiveTenantId
    }
    const res = await getGroupList(params)
    groupList.value = res || []
  } catch (error) {
    console.error('Failed to fetch group list:', error)
  }
}

const getGroupName = (groupId) => {
  if (!groupId) return '-'
  const path = []
  let current = groupList.value.find(g => g.id === groupId)
  while (current) {
    path.unshift(current.groupName)
    current = current.parentId ? groupList.value.find(g => g.id === current.parentId) : null
  }
  return path.length ? path.join(' / ') : '-'
}

const buildGroupTree = (groups) => {
  const map = {}
  const tree = []
  groups.forEach(g => {
    map[g.id] = { ...g, children: [] }
  })
  groups.forEach(g => {
    if (g.parentId && map[g.parentId]) {
      map[g.parentId].children.push(map[g.id])
    } else {
      tree.push(map[g.id])
    }
  })
  return tree
}

const bindGroupDialogVisible = ref(false)
const bindGroupTarget = ref({})
const selectedGroupId = ref(null)

const handleBindGroup = (row) => {
  bindGroupTarget.value = row
  selectedGroupId.value = null
  bindGroupDialogVisible.value = true
}

const handleUnbindGroup = (row) => {
  ElMessageBox.confirm(
    `确定要解绑表计 "${row.meterName || row.meterNo}" 的群组吗？`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    await unbindGroup(row.id)
    ElMessage.success('解绑成功')
    fetchData()
  }).catch(() => {})
}

const submitBindGroup = async () => {
  if (!selectedGroupId.value) {
    ElMessage.warning('请选择群组')
    return
  }
  try {
    await bindGroup(bindGroupTarget.value.id, selectedGroupId.value)
    ElMessage.success('绑定成功')
    bindGroupDialogVisible.value = false
    fetchData()
  } catch (error) {
    console.error('Failed to bind group:', error)
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
  formData.meterNo = ''
  formData.meterName = ''
  formData.meterType = null
  formData.manufacturerId = null
  formData.protocolCode = ''
  formData.deviceAddress = ''
  formData.ctRatio = 1.0
  formData.ptRatio = 1.0
  formData.meterRatio = 1.0
  formData.address = ''
  formData.installTime = ''
  formData.groupId = null
  formData.remark = ''
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
  formData.meterNo = row.meterNo
  formData.meterName = row.meterName
  formData.meterType = row.meterType
  formData.manufacturerId = row.manufacturerId
  formData.protocolCode = row.protocolCode
  formData.deviceAddress = row.deviceAddress
  formData.ctRatio = row.ctRatio
  formData.ptRatio = row.ptRatio
  formData.meterRatio = row.meterRatio
  formData.address = row.address
  formData.installTime = row.installTime
  formData.groupId = row.groupId
  formData.remark = row.remark
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const submitData = { ...formData, tenantId: userStore.effectiveTenantId }
    if (isEdit.value) {
      await updateMeter(submitData.id, submitData)
      ElMessage.success('更新成功')
    } else {
      await createMeter(submitData)
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
    `确定要删除表计 "${row.meterName || row.meterNo}" 吗？`,
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    await deleteMeter(row.id)
    ElMessage.success('删除成功')
    fetchData()
  }).catch(() => {})
}

const handleBind = (row) => {
  bindTarget.value = row
  selectedGatewayId.value = null
  bindDialogVisible.value = true
}

const handleUnbind = (row) => {
  ElMessageBox.confirm(
    `确定要解绑表计 "${row.meterName || row.meterNo}" 的网关吗？`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    await unbindGateway(row.id)
    ElMessage.success('解绑成功')
    fetchData()
  }).catch(() => {})
}

const submitBind = async () => {
  if (!selectedGatewayId.value) {
    ElMessage.warning('请选择网关')
    return
  }
  bindLoading.value = true
  try {
    await bindGateway(bindTarget.value.id, selectedGatewayId.value)
    ElMessage.success('绑定成功')
    bindDialogVisible.value = false
    fetchData()
  } catch (error) {
    console.error('Failed to bind gateway:', error)
  } finally {
    bindLoading.value = false
  }
}

onMounted(() => {
  fetchData()
  fetchProtocols()
  fetchManufacturers()
  fetchGateways()
  fetchGroups()
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
