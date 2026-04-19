<template>
  <el-dialog
    :title="isEdit ? '编辑网关' : '新增网关'"
    v-model="visible"
    width="600px"
    @close="handleClose"
  >
    <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
      <el-form-item label="网关名称" prop="name">
        <el-input v-model="form.name" />
      </el-form-item>
      
      <el-form-item label="通讯地址" prop="communicationAddress">
        <el-input v-model="form.communicationAddress" :disabled="isEdit" />
      </el-form-item>
      
      <el-form-item label="协议" prop="protocol">
        <el-select v-model="form.protocol" style="width: 100%">
          <el-option label="MQTT" value="MQTT" />
        </el-select>
      </el-form-item>
      
      <el-form-item label="通讯模式" prop="commMode">
        <el-radio-group v-model="form.commMode">
          <el-radio label="SERVER">服务端</el-radio>
          <el-radio label="CLIENT">客户端</el-radio>
        </el-radio-group>
      </el-form-item>
      
      <el-form-item v-if="form.commMode === 'SERVER'" label="侦听端口" prop="serverPort">
        <el-input-number v-model="form.serverPort" :min="1" :max="65535" />
      </el-form-item>
      
      <template v-if="form.commMode === 'CLIENT'">
        <el-form-item label="服务器IP" prop="clientHost">
          <el-input v-model="form.clientHost" />
        </el-form-item>
        <el-form-item label="服务器端口" prop="clientPort">
          <el-input-number v-model="form.clientPort" :min="1" :max="65535" />
        </el-form-item>
      </template>
      
      <template v-if="form.protocol === 'MQTT'">
        <el-form-item label="Broker地址" prop="mqttBroker">
          <el-input v-model="form.mqttBroker" placeholder="tcp://localhost:1883" />
        </el-form-item>
        <el-form-item label="Client ID" prop="mqttClientId">
          <el-input v-model="form.mqttClientId" />
        </el-form-item>
      </template>
    </el-form>
    
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createGateway, updateGateway } from '../../api/gateway'

const props = defineProps({
  visible: Boolean,
  data: Object
})

const emit = defineEmits(['update:visible', 'success'])

const visible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

const isEdit = computed(() => !!props.data)
const formRef = ref()
const submitting = ref(false)

const defaultForm = {
  name: '',
  communicationAddress: '',
  protocol: 'MQTT',
  commMode: 'CLIENT',
  serverPort: null,
  clientHost: '',
  clientPort: null,
  mqttBroker: '',
  mqttClientId: ''
}

const form = reactive({ ...defaultForm })

const rules = {
  name: [{ required: true, message: '请输入网关名称', trigger: 'blur' }],
  communicationAddress: [{ required: true, message: '请输入通讯地址', trigger: 'blur' }],
  protocol: [{ required: true, message: '请选择协议', trigger: 'change' }],
  commMode: [{ required: true, message: '请选择通讯模式', trigger: 'change' }],
  mqttBroker: [{ required: true, message: '请输入Broker地址', trigger: 'blur' }]
}

watch(() => props.data, (val) => {
  if (val) {
    Object.assign(form, val)
  } else {
    Object.assign(form, defaultForm)
  }
}, { immediate: true })

const handleClose = () => {
  formRef.value?.resetFields()
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateGateway(props.data.id, form)
    } else {
      await createGateway(form)
    }
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
    visible.value = false
    emit('success')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '操作失败')
  } finally {
    submitting.value = false
  }
}
</script>
