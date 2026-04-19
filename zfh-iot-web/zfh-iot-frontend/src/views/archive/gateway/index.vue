<template>
  <div class="app-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <span>网关管理</span>
            <el-input
              v-model="queryParams.keyword"
              placeholder="请输入网关编号/名称"
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
          <el-button v-permission="'gateway:create'" type="primary" @click="handleAdd">新增网关</el-button>
        </div>
      </template>

      <el-table :data="gatewayList" v-loading="loading" stripe>
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="gatewayNo" label="网关编号" width="140" />
        <el-table-column prop="commAddr" label="通讯地址" width="120" />
        <el-table-column prop="gatewayName" label="网关名称" min-width="120" show-overflow-tooltip />
        <el-table-column label="网关类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.gatewayType === GatewayTypeEnum.MQTT.code ? 'success' : 'primary'">{{ getGatewayTypeName(row.gatewayType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="协议" width="160">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ getGatewayProtocolName(row.protocolCode) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="IP地址" width="160">
          <template #default="{ row }">
            {{ row.ipAddress ? `${row.ipAddress}:${row.port}` : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="location" label="安装位置" width="120" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastOnlineTime" label="最后在线时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'gateway:update'" type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button v-permission="'gateway:delete'" type="danger" link @click="handleDelete(row)">删除</el-button>
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
      :title="isEdit ? '编辑网关' : '新增网关'"
      width="650px"
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
            <el-form-item label="网关编号">
              <el-input
                v-model="formData.gatewayNo"
                placeholder="系统自动生成"
                disabled
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="网关名称" prop="gatewayName">
              <el-input
                v-model="formData.gatewayName"
                placeholder="请输入网关名称"
                maxlength="128"
                show-word-limit
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="网关类型" prop="gatewayType">
              <el-select v-model="formData.gatewayType" placeholder="请选择网关类型" style="width: 100%">
                <el-option
                  v-for="item in GatewayTypeList"
                  :key="item.code"
                  :label="item.name"
                  :value="item.code"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="协议" prop="protocolCode">
              <el-select v-model="formData.protocolCode" placeholder="请选择协议" style="width: 100%">
                <el-option
                  v-for="item in GatewayProtocolList"
                  :key="item.code"
                  :label="item.name"
                  :value="item.code"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="通讯地址" prop="commAddr">
          <el-input
            v-model="formData.commAddr"
            :placeholder="commAddrPlaceholder"
            :maxlength="commAddrMaxLength"
            show-word-limit
            style="width: 50%"
          />
          <span style="margin-left: 10px; color: #909399; font-size: 12px">{{ commAddrHint }}</span>
        </el-form-item>

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
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="IP地址" prop="ipAddress">
              <el-input v-model="formData.ipAddress" placeholder="请输入IP地址" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="端口" prop="port">
              <el-input-number v-model="formData.port" :min="1" :max="65535" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="安装位置" prop="location">
          <el-input
            v-model="formData.location"
            placeholder="请输入网关安装位置"
            maxlength="255"
            show-word-limit
          />
        </el-form-item>
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
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import config from '@/config'
import { useUserStore } from '@/stores/user'
import { getGatewayList, createGateway, updateGateway, deleteGateway } from '@/api/gateway'
import { getManufacturerList } from '@/api/manufacturer'
import { GatewayTypeEnum, GatewayTypeList, getGatewayTypeName } from '@/enums/gateway'
import { GatewayProtocolEnum, GatewayProtocolList, getGatewayProtocolName } from '@/enums/protocol'

const { deviceStatus } = config
const userStore = useUserStore()

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const gatewayList = ref([])
const manufacturerList = ref([])
const total = ref(0)
const formRef = ref(null)

const queryParams = reactive({
  page: 1,
  size: 20,
  keyword: '',
  status: undefined
})

const formData = reactive({
  id: null,
  gatewayNo: '',
  gatewayName: '',
  gatewayType: '',
  manufacturerId: null,
  protocolCode: '',
  commAddr: '',
  ipAddress: '',
  port: 8080,
  location: '',
  remark: ''
})

const commAddrPlaceholder = computed(() => {
  return formData.protocolCode === GatewayProtocolEnum.DLT645_2007.code
    ? '请输入8位16进制通讯地址'
    : '请输入英文+数字通讯地址'
})

const commAddrMaxLength = computed(() => {
  return formData.protocolCode === GatewayProtocolEnum.DLT645_2007.code ? 8 : 32
})

const commAddrHint = computed(() => {
  return formData.protocolCode === GatewayProtocolEnum.DLT645_2007.code
    ? '8位16进制数字'
    : '英文+数字，最长32位'
})

const validateCommAddr = (rule, value, callback) => {
  if (!value) {
    return callback(new Error('请输入通讯地址'))
  }
  if (formData.protocolCode === GatewayProtocolEnum.DLT645_2007.code) {
    if (!/^[0-9a-fA-F]{8}$/.test(value)) {
      return callback(new Error('必须为8位16进制数字'))
    }
  } else if (formData.protocolCode === GatewayProtocolEnum.MQTT_ENERGY.code) {
    if (!/^[a-zA-Z0-9]{1,32}$/.test(value)) {
      return callback(new Error('只能包含英文和数字，最长32位'))
    }
  }
  callback()
}

const formRules = {
  gatewayName: [
    { required: true, message: '请输入网关名称', trigger: 'blur' },
    { min: 2, max: 128, message: '长度在 2 到 128 个字符', trigger: 'blur' }
  ],
  gatewayType: [
    { required: true, message: '请选择网关类型', trigger: 'change' }
  ],
  protocolCode: [
    { required: true, message: '请选择协议', trigger: 'change' }
  ],
  commAddr: [
    { required: true, validator: validateCommAddr, trigger: 'blur' }
  ]
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
    const params = { ...queryParams }
    params.tenantId = userStore.effectiveTenantId
    const res = await getGatewayList(params)
    gatewayList.value = res.list || []
    total.value = res.total || 0
  } catch (error) {
    console.error('Failed to fetch gateway list:', error)
  } finally {
    loading.value = false
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
  formData.gatewayNo = ''
  formData.gatewayName = ''
  formData.gatewayType = ''
  formData.manufacturerId = null
  formData.protocolCode = ''
  formData.commAddr = ''
  formData.ipAddress = ''
  formData.port = 8080
  formData.location = ''
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
  formData.gatewayNo = row.gatewayNo
  formData.gatewayName = row.gatewayName
  formData.gatewayType = row.gatewayType
  formData.manufacturerId = row.manufacturerId
  formData.protocolCode = row.protocolCode
  formData.commAddr = row.commAddr
  formData.ipAddress = row.ipAddress
  formData.port = row.port
  formData.location = row.location
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
      await updateGateway(submitData.id, submitData)
      ElMessage.success('更新成功')
    } else {
      await createGateway(submitData)
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
    `确定要删除网关 "${row.gatewayName || row.gatewayNo}" 吗？`,
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    await deleteGateway(row.id)
    ElMessage.success('删除成功')
    fetchData()
  }).catch(() => {})
}

onMounted(() => {
  fetchData()
  fetchManufacturers()
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
