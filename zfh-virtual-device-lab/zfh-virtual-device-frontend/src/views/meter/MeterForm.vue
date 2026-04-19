<template>
  <el-dialog
    :title="isEdit ? '编辑表计' : '新增表计'"
    v-model="visible"
    width="600px"
    @close="handleClose"
  >
    <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
      <el-form-item label="表计名称" prop="name">
        <el-input v-model="form.name" />
      </el-form-item>
      
      <el-form-item label="表计类型" prop="meterType">
        <el-radio-group v-model="form.meterType">
          <el-radio label="ELECTRIC">电表</el-radio>
          <el-radio label="WATER">水表</el-radio>
        </el-radio-group>
      </el-form-item>
      
      <el-form-item label="通讯地址" prop="communicationAddress">
        <el-input v-model="form.communicationAddress" :disabled="isEdit" />
      </el-form-item>
      
      <el-form-item label="协议" prop="protocol">
        <el-select v-model="form.protocol" style="width: 100%">
          <el-option label="MQTT" value="MQTT" />
        </el-select>
      </el-form-item>
      
      <el-form-item label="自动上报" prop="autoReport">
        <el-switch v-model="form.autoReport" />
      </el-form-item>
      
      <el-form-item label="上报间隔(秒)" prop="reportInterval">
        <el-input-number v-model="form.reportInterval" :min="1" :max="3600" />
      </el-form-item>
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
import { createMeter, updateMeter } from '../../api/meter'

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
  meterType: 'ELECTRIC',
  communicationAddress: '',
  protocol: 'MQTT',
  connectionMode: 'DIRECT',
  autoReport: true,
  reportInterval: 30
}

const form = reactive({ ...defaultForm })

const rules = {
  name: [{ required: true, message: '请输入表计名称', trigger: 'blur' }],
  meterType: [{ required: true, message: '请选择表计类型', trigger: 'change' }],
  communicationAddress: [{ required: true, message: '请输入通讯地址', trigger: 'blur' }],
  protocol: [{ required: true, message: '请选择协议', trigger: 'change' }]
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
      await updateMeter(props.data.id, form)
    } else {
      await createMeter(form)
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
