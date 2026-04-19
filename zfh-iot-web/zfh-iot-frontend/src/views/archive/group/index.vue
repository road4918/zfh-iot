<template>
  <div class="app-container">
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>群组管理</span>
              <el-button v-permission="'group:create'" type="primary" size="small" @click="handleAdd(0)">新增根群组</el-button>
            </div>
          </template>

          <div v-if="selectedGroup" class="selected-info">
            <span>当前: {{ selectedGroup.groupName }}</span>
            <el-button size="small" type="primary" link @click="handleAdd(selectedGroup.id)">新增子群组</el-button>
          </div>

          <el-tree
            ref="treeRef"
            :data="treeData"
            :props="{ label: 'groupName', children: 'children' }"
            node-key="id"
            highlight-current
            default-expand-all
            @node-click="handleNodeClick"
          >
            <template #default="{ node, data }">
              <div class="tree-node">
                <span>{{ data.groupName }}</span>
                <span class="tree-node-actions">
                  <el-button v-permission="'group:create'" type="primary" link size="small" @click.stop="handleAdd(data.id)">
                    <el-icon><Plus /></el-icon>
                  </el-button>
                  <el-button v-permission="'group:update'" type="primary" link size="small" @click.stop="handleEdit(data)">
                    <el-icon><Edit /></el-icon>
                  </el-button>
                  <el-button v-permission="'group:delete'" type="danger" link size="small" @click.stop="handleDelete(data)">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </span>
              </div>
            </template>
          </el-tree>

          <el-empty v-if="treeData.length === 0" description="暂无群组数据" />
        </el-card>
      </el-col>

      <el-col :span="16">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>{{ selectedGroup ? `${selectedGroup.groupName} - 设备列表` : '请选择群组' }}</span>
              <el-button
                v-if="selectedGroup"
                v-permission="'group:update'"
                type="primary"
                size="small"
                @click="showAddMeterDialog"
              >
                添加设备
              </el-button>
            </div>
          </template>

          <template v-if="selectedGroup">
            <el-table :data="groupMeters" v-loading="metersLoading" stripe>
              <el-table-column type="index" label="序号" width="60" />
              <el-table-column prop="meterNo" label="表计编号" width="140" />
              <el-table-column prop="meterName" label="表计名称" min-width="150" show-overflow-tooltip />
              <el-table-column prop="meterType" label="类型" width="80">
                <template #default="{ row }">
                  <el-tag size="small">{{ getMeterTypeLabel(row.meterType) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="deviceAddress" label="设备地址" width="100" />
              <el-table-column prop="status" label="状态" width="80">
                <template #default="{ row }">
                  <el-tag :type="getStatusType(row.status)" size="small">
                    {{ getStatusLabel(row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="100" fixed="right">
                <template #default="{ row }">
                  <el-button v-permission="'group:update'" type="danger" link @click="handleRemoveMeter(row)">移除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </template>
          <el-empty v-else description="请在左侧选择群组查看设备" />
        </el-card>
      </el-col>
    </el-row>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="90px">
        <el-form-item label="群组名称" prop="groupName">
          <el-input v-model="formData.groupName" placeholder="请输入群组名称" maxlength="128" show-word-limit />
        </el-form-item>
        <el-form-item label="群组类型" prop="groupType">
          <el-select v-model="formData.groupType" placeholder="请选择群组类型" style="width: 100%">
            <el-option label="区域" :value="1" />
            <el-option label="楼栋" :value="2" />
            <el-option label="楼层" :value="3" />
            <el-option label="自定义" :value="9" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="formData.sortOrder" :min="0" :max="9999" style="width: 100%" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            placeholder="请输入描述"
            maxlength="256"
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
      v-model="addMeterDialogVisible"
      title="添加设备到群组"
      width="700px"
      destroy-on-close
    >
      <div style="margin-bottom: 10px">
        <el-input
          v-model="meterSearchKeyword"
          placeholder="搜索表计编号/名称"
          clearable
          style="width: 300px"
          @keyup.enter="fetchAvailableMeters"
        >
          <template #append>
            <el-button @click="fetchAvailableMeters">
              <el-icon><Search /></el-icon>
            </el-button>
          </template>
        </el-input>
      </div>
      <el-table
        ref="addMeterTableRef"
        :data="availableMeters"
        v-loading="addMeterLoading"
        stripe
        max-height="400"
        @selection-change="handleMeterSelectionChange"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column prop="meterNo" label="表计编号" width="140" />
        <el-table-column prop="meterName" label="表计名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="meterType" label="类型" width="80">
          <template #default="{ row }">
            <el-tag size="small">{{ getMeterTypeLabel(row.meterType) }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="addMeterDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAddMeters" :loading="addMeterSubmitLoading" :disabled="selectedMeters.length === 0">
          添加 ({{ selectedMeters.length }})
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Edit, Delete } from '@element-plus/icons-vue'
import config from '@/config'
import { useUserStore } from '@/stores/user'
import { getGroupTree, createGroup, updateGroup, deleteGroup, getGroupMeters, addGroupMeters, removeGroupMeter } from '@/api/group'
import { getMeterList } from '@/api/meter'

const { deviceStatus, meterTypes } = config
const userStore = useUserStore()

const treeRef = ref(null)
const formRef = ref(null)
const treeData = ref([])
const selectedGroup = ref(null)
const groupMeters = ref([])
const metersLoading = ref(false)
const dialogVisible = ref(false)
const submitLoading = ref(false)
const isEdit = ref(false)
const parentIdForAdd = ref(0)

const formData = reactive({
  id: null,
  groupName: '',
  groupType: null,
  sortOrder: 0,
  description: ''
})

const formRules = {
  groupName: [
    { required: true, message: '请输入群组名称', trigger: 'blur' },
    { min: 1, max: 128, message: '长度在 1 到 128 个字符', trigger: 'blur' }
  ]
}

const dialogTitle = ref('新增群组')

const addMeterDialogVisible = ref(false)
const addMeterLoading = ref(false)
const addMeterSubmitLoading = ref(false)
const availableMeters = ref([])
const selectedMeters = ref([])
const meterSearchKeyword = ref('')

const getMeterTypeLabel = (type) => {
  const item = meterTypes.find(t => t.value === type)
  return item ? item.label : '未知'
}

const getStatusType = (status) => {
  const item = deviceStatus.find(s => s.value === status)
  return item ? item.type : 'info'
}

const getStatusLabel = (status) => {
  const item = deviceStatus.find(s => s.value === status)
  return item ? item.label : '未知'
}

const fetchTree = async () => {
  try {
    const params = {}
    if (userStore.effectiveTenantId) {
      params.tenantId = userStore.effectiveTenantId
    }
    const res = await getGroupTree(params)
    treeData.value = res || []
  } catch (error) {
    console.error('Failed to fetch group tree:', error)
  }
}

const fetchGroupMeters = async (groupId) => {
  metersLoading.value = true
  try {
    const res = await getGroupMeters(groupId)
    groupMeters.value = res || []
  } catch (error) {
    console.error('Failed to fetch group meters:', error)
  } finally {
    metersLoading.value = false
  }
}

const handleNodeClick = (data) => {
  selectedGroup.value = data
  fetchGroupMeters(data.id)
}

const resetForm = () => {
  formData.id = null
  formData.groupName = ''
  formData.groupType = null
  formData.sortOrder = 0
  formData.description = ''
}

const handleAdd = (parentId) => {
  isEdit.value = false
  parentIdForAdd.value = parentId
  dialogTitle.value = parentId === 0 ? '新增根群组' : '新增子群组'
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (data) => {
  isEdit.value = true
  dialogTitle.value = '编辑群组'
  resetForm()
  formData.id = data.id
  formData.groupName = data.groupName
  formData.groupType = data.groupType
  formData.sortOrder = data.sortOrder
  formData.description = data.description
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const submitData = { ...formData, tenantId: userStore.effectiveTenantId }
    if (isEdit.value) {
      await updateGroup(submitData.id, submitData)
      ElMessage.success('更新成功')
    } else {
      submitData.parentId = parentIdForAdd.value
      await createGroup(submitData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchTree()
  } catch (error) {
    console.error('Failed to submit:', error)
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = (data) => {
  ElMessageBox.confirm(
    `确定要删除群组 "${data.groupName}" 吗？`,
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    await deleteGroup(data.id)
    ElMessage.success('删除成功')
    if (selectedGroup.value && selectedGroup.value.id === data.id) {
      selectedGroup.value = null
      groupMeters.value = []
    }
    fetchTree()
  }).catch(() => {})
}

const showAddMeterDialog = () => {
  meterSearchKeyword.value = ''
  selectedMeters.value = []
  addMeterDialogVisible.value = true
  fetchAvailableMeters()
}

const fetchAvailableMeters = async () => {
  addMeterLoading.value = true
  try {
    const params = {
      page: 1,
      size: 100,
      tenantId: userStore.effectiveTenantId
    }
    if (meterSearchKeyword.value) {
      params.keyword = meterSearchKeyword.value
    }
    const res = await getMeterList(params)
    const allMeters = res.list || []
    availableMeters.value = allMeters.filter(m => !m.groupId)
  } catch (error) {
    console.error('Failed to fetch available meters:', error)
  } finally {
    addMeterLoading.value = false
  }
}

const handleMeterSelectionChange = (selection) => {
  selectedMeters.value = selection
}

const submitAddMeters = async () => {
  if (selectedMeters.value.length === 0) return
  addMeterSubmitLoading.value = true
  try {
    const meterIds = selectedMeters.value.map(m => m.id)
    await addGroupMeters(selectedGroup.value.id, meterIds)
    ElMessage.success('添加成功')
    addMeterDialogVisible.value = false
    fetchGroupMeters(selectedGroup.value.id)
  } catch (error) {
    console.error('Failed to add meters:', error)
  } finally {
    addMeterSubmitLoading.value = false
  }
}

const handleRemoveMeter = (row) => {
  ElMessageBox.confirm(
    `确定要从群组中移除表计 "${row.meterName || row.meterNo}" 吗？`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    await removeGroupMeter(selectedGroup.value.id, row.id)
    ElMessage.success('移除成功')
    fetchGroupMeters(selectedGroup.value.id)
  }).catch(() => {})
}

onMounted(() => {
  fetchTree()
})
</script>

<style scoped lang="scss">
.app-container {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .selected-info {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 12px;
    margin-bottom: 10px;
    background-color: #ecf5ff;
    border-radius: 4px;
    font-size: 13px;
    color: #409eff;
  }

  .tree-node {
    display: flex;
    justify-content: space-between;
    align-items: center;
    width: 100%;
    font-size: 14px;

    .tree-node-actions {
      display: none;
    }

    &:hover .tree-node-actions {
      display: inline-flex;
    }
  }
}
</style>
