# zfh-virtual-device-lab Frontend Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Vue 3 frontend for zfh-virtual-device-lab MVP, providing Web UI for virtual gateway/meter management, real-time communication logs, and device connection control.

**Architecture:** Vue 3 + Vite + Element Plus + Pinia. Uses SockJS + STOMP for WebSocket real-time updates. Axios for HTTP API calls.

**Tech Stack:** Vue 3.4, Vite 5, Element Plus 2, Pinia 2, Vue Router 4, Axios, SockJS-client, Stompjs

---

## Chunk 1: Project Setup & Base Layout

### Task 1: Initialize Vue Project

**Files:**
- Create: `zfh-virtual-device-frontend/package.json`
- Create: `zfh-virtual-device-frontend/vite.config.js`
- Create: `zfh-virtual-device-frontend/index.html`
- Create: `zfh-virtual-device-frontend/src/main.js`
- Create: `zfh-virtual-device-frontend/src/App.vue`

- [ ] **Step 1: Create package.json**

```json
{
  "name": "zfh-virtual-device-frontend",
  "private": true,
  "version": "1.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "vue": "^3.4.0",
    "vue-router": "^4.2.0",
    "pinia": "^2.1.0",
    "element-plus": "^2.5.0",
    "axios": "^1.6.0",
    "sockjs-client": "^1.6.1",
    "@stomp/stompjs": "^7.0.0",
    "@element-plus/icons-vue": "^2.3.0"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^5.0.0",
    "vite": "^5.0.0"
  }
}
```

- [ ] **Step 2: Create vite.config.js**

```javascript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/ws': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        ws: true
      }
    }
  },
  build: {
    outDir: 'dist'
  }
})
```

- [ ] **Step 3: Create index.html**

```html
<!DOCTYPE html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <link rel="icon" type="image/svg+xml" href="/vite.svg" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>虚拟设备实验室</title>
  </head>
  <body>
    <div id="app"></div>
    <script type="module" src="/src/main.js"></script>
  </body>
</html>
```

- [ ] **Step 4: Create main.js**

```javascript
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'

const app = createApp(App)

// Register all icons
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus)

app.mount('#app')
```

- [ ] **Step 5: Create App.vue**

```vue
<template>
  <router-view />
</template>

<script setup>
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}
</style>
```

- [ ] **Step 6: Install dependencies and verify**

```bash
cd zfh-virtual-device-frontend
npm install
npm run dev
```
Expected: Vite dev server starts on port 5173, blank page loads.

- [ ] **Step 7: Commit**

```bash
git add zfh-virtual-device-frontend/
git commit -m "feat: initialize Vue 3 frontend project"
```

---

### Task 2: Create Router and Layout

**Files:**
- Create: `zfh-virtual-device-frontend/src/router/index.js`
- Create: `zfh-virtual-device-frontend/src/layouts/MainLayout.vue`
- Create: `zfh-virtual-device-frontend/src/views/LoginView.vue`
- Create: `zfh-virtual-device-frontend/src/views/DashboardView.vue`

- [ ] **Step 1: Create router**

```javascript
import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('../views/LoginView.vue'),
      meta: { public: true }
    },
    {
      path: '/',
      component: () => import('../layouts/MainLayout.vue'),
      redirect: '/gateways',
      children: [
        {
          path: 'gateways',
          name: 'Gateways',
          component: () => import('../views/gateway/GatewayList.vue')
        },
        {
          path: 'meters',
          name: 'Meters',
          component: () => import('../views/meter/MeterList.vue')
        },
        {
          path: 'logs',
          name: 'Logs',
          component: () => import('../views/log/LogList.vue')
        }
      ]
    }
  ]
})

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()
  if (!to.meta.public && !authStore.token) {
    next('/login')
  } else {
    next()
  }
})

export default router
```

- [ ] **Step 2: Create MainLayout**

```vue
<template>
  <el-container class="main-layout">
    <el-aside width="200px" class="sidebar">
      <div class="logo">虚拟设备实验室</div>
      <el-menu
        :default-active="$route.path"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <el-menu-item index="/gateways">
          <el-icon><Connection /></el-icon>
          <span>网关管理</span>
        </el-menu-item>
        <el-menu-item index="/meters">
          <el-icon><Odometer /></el-icon>
          <span>表计管理</span>
        </el-menu-item>
        <el-menu-item index="/logs">
          <el-icon><Document /></el-icon>
          <span>通讯日志</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    
    <el-container>
      <el-header class="header">
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-icon><User /></el-icon>
              admin
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const handleCommand = (command) => {
  if (command === 'logout') {
    authStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.main-layout {
  height: 100vh;
}

.sidebar {
  background-color: #304156;
}

.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: #fff;
  font-size: 16px;
  font-weight: bold;
  border-bottom: 1px solid #1f2d3d;
}

.header {
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.header-right {
  cursor: pointer;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 5px;
}

.main-content {
  background-color: #f0f2f5;
  padding: 20px;
}
</style>
```

- [ ] **Step 3: Create LoginView**

```vue
<template>
  <div class="login-container">
    <el-card class="login-card">
      <template #header>
        <div class="login-header">虚拟设备实验室</div>
      </template>
      
      <el-form :model="form" :rules="rules" ref="formRef">
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="用户名"
            prefix-icon="User"
            size="large"
          />
        </el-form-item>
        
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            prefix-icon="Lock"
            size="large"
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            @click="handleLogin"
            style="width: 100%"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: 'admin',
  password: 'admin123'
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  
  loading.value = true
  try {
    await authStore.login(form.username, form.password)
    ElMessage.success('登录成功')
    router.push('/')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f0f2f5;
}

.login-card {
  width: 400px;
}

.login-header {
  text-align: center;
  font-size: 20px;
  font-weight: bold;
  color: #303133;
}
</style>
```

- [ ] **Step 4: Commit**

```bash
git add zfh-virtual-device-frontend/src/router/
git add zfh-virtual-device-frontend/src/layouts/
git add zfh-virtual-device-frontend/src/views/
git commit -m "feat: add router, layout and login page"
```

---

## Chunk 2: API Client & State Management

### Task 3: Create API Client and Auth Store

**Files:**
- Create: `zfh-virtual-device-frontend/src/utils/request.js`
- Create: `zfh-virtual-device-frontend/src/stores/auth.js`
- Create: `zfh-virtual-device-frontend/src/api/auth.js`

- [ ] **Step 1: Create HTTP request interceptor**

```javascript
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.request.use(
  config => {
    const authStore = useAuthStore()
    if (authStore.token) {
      config.headers.Authorization = `Bearer ${authStore.token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message))
    }
    return res
  },
  error => {
    const { response } = error
    if (response?.status === 401) {
      const authStore = useAuthStore()
      authStore.logout()
      window.location.href = '/login'
    } else {
      ElMessage.error(response?.data?.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default request
```

- [ ] **Step 2: Create auth store**

```javascript
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi } from '../api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const user = ref(JSON.parse(localStorage.getItem('user') || '{}'))

  const login = async (username, password) => {
    const res = await loginApi({ username, password })
    token.value = res.data.token
    user.value = { username }
    localStorage.setItem('token', res.data.token)
    localStorage.setItem('user', JSON.stringify(user.value))
    return res
  }

  const logout = () => {
    token.value = ''
    user.value = {}
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  return { token, user, login, logout }
})
```

- [ ] **Step 3: Create auth API**

```javascript
import request from '../utils/request'

export const login = (data) => {
  return request.post('/auth/login', data)
}

export const getProfile = () => {
  return request.get('/auth/profile')
}
```

- [ ] **Step 4: Commit**

```bash
git add zfh-virtual-device-frontend/src/utils/
git add zfh-virtual-device-frontend/src/stores/
git add zfh-virtual-device-frontend/src/api/
git commit -m "feat: add API client and auth store"
```

---

### Task 4: Create Gateway/Meter API Modules

**Files:**
- Create: `zfh-virtual-device-frontend/src/api/gateway.js`
- Create: `zfh-virtual-device-frontend/src/api/meter.js`
- Create: `zfh-virtual-device-frontend/src/api/log.js`

- [ ] **Step 1: Create gateway API**

```javascript
import request from '../utils/request'

export const getGatewayList = (params) => {
  return request.get('/gateways', { params })
}

export const getGatewayById = (id) => {
  return request.get(`/gateways/${id}`)
}

export const createGateway = (data) => {
  return request.post('/gateways', data)
}

export const updateGateway = (id, data) => {
  return request.put(`/gateways/${id}`, data)
}

export const deleteGateway = (id) => {
  return request.delete(`/gateways/${id}`)
}

export const startGateway = (id) => {
  return request.post(`/gateways/${id}/start`)
}

export const stopGateway = (id) => {
  return request.post(`/gateways/${id}/stop`)
}
```

- [ ] **Step 2: Create meter API**

```javascript
import request from '../utils/request'

export const getMeterList = (params) => {
  return request.get('/meters', { params })
}

export const getMeterById = (id) => {
  return request.get(`/meters/${id}`)
}

export const createMeter = (data) => {
  return request.post('/meters', data)
}

export const updateMeter = (id, data) => {
  return request.put(`/meters/${id}`, data)
}

export const deleteMeter = (id) => {
  return request.delete(`/meters/${id}`)
}

export const startMeter = (id) => {
  return request.post(`/meters/${id}/start`)
}

export const stopMeter = (id) => {
  return request.post(`/meters/${id}/stop`)
}
```

- [ ] **Step 3: Create log API**

```javascript
import request from '../utils/request'

export const getLogList = (params) => {
  return request.get('/logs', { params })
}

export const clearLogs = () => {
  return request.delete('/logs')
}
```

- [ ] **Step 4: Commit**

```bash
git add zfh-virtual-device-frontend/src/api/
git commit -m "feat: add gateway, meter and log API modules"
```

---

## Chunk 3: Gateway Management Pages

### Task 5: Gateway List Page

**Files:**
- Create: `zfh-virtual-device-frontend/src/views/gateway/GatewayList.vue`
- Create: `zfh-virtual-device-frontend/src/views/gateway/GatewayForm.vue`

- [ ] **Step 1: Create GatewayList component**

```vue
<template>
  <div class="gateway-list">
    <div class="toolbar">
      <el-button type="primary" @click="handleCreate">
        <el-icon><Plus /></el-icon>新增网关
      </el-button>
    </div>
    
    <el-table :data="gatewayList" v-loading="loading" border>
      <el-table-column prop="name" label="网关名称" />
      <el-table-column prop="communicationAddress" label="通讯地址" />
      <el-table-column prop="protocol" label="协议" width="100" />
      <el-table-column prop="commMode" label="通讯模式" width="100">
        <template #default="{ row }">
          <el-tag>{{ row.commMode === 'SERVER' ? '服务端' : '客户端' }}</el-tag>
        </template>
      </el-table-column>
      
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">
            {{ getStatusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      
      <el-table-column label="操作" width="250" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 'OFFLINE'"
            type="success"
            size="small"
            @click="handleStart(row)"
          >
            启动
          </el-button>
          <el-button
            v-else
            type="warning"
            size="small"
            @click="handleStop(row)"
          >
            停止
          </el-button>
          <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <el-pagination
      v-model:current-page="page.current"
      v-model:page-size="page.size"
      :total="page.total"
      layout="total, prev, pager, next"
      @change="loadData"
    />
    
    <GatewayForm
      v-model:visible="formVisible"
      :data="currentRow"
      @success="loadData"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getGatewayList, startGateway, stopGateway, deleteGateway } from '../../api/gateway'
import GatewayForm from './GatewayForm.vue'

const loading = ref(false)
const gatewayList = ref([])
const formVisible = ref(false)
const currentRow = ref(null)

const page = reactive({
  current: 1,
  size: 10,
  total: 0
})

const getStatusType = (status) => {
  const map = { ONLINE: 'success', OFFLINE: 'info', ERROR: 'danger' }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = { ONLINE: '在线', OFFLINE: '离线', ERROR: '异常' }
  return map[status] || status
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getGatewayList({ current: page.current, size: page.size })
    gatewayList.value = res.data.records
    page.total = res.data.total
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleCreate = () => {
  currentRow.value = null
  formVisible.value = true
}

const handleEdit = (row) => {
  currentRow.value = row
  formVisible.value = true
}

const handleStart = async (row) => {
  try {
    await startGateway(row.id)
    ElMessage.success('启动成功')
    loadData()
  } catch (error) {
    ElMessage.error('启动失败')
  }
}

const handleStop = async (row) => {
  try {
    await stopGateway(row.id)
    ElMessage.success('停止成功')
    loadData()
  } catch (error) {
    ElMessage.error('停止失败')
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确认删除该网关？', '提示', { type: 'warning' })
    await deleteGateway(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(loadData)
</script>

<style scoped>
.gateway-list {
  .toolbar {
    margin-bottom: 20px;
  }
}
</style>
```

- [ ] **Step 2: Create GatewayForm dialog**

```vue
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
```

- [ ] **Step 3: Commit**

```bash
git add zfh-virtual-device-frontend/src/views/gateway/
git commit -m "feat: add gateway management pages"
```

---

## Chunk 4: Meter Management Pages

### Task 6: Meter List Page

**Files:**
- Create: `zfh-virtual-device-frontend/src/views/meter/MeterList.vue`
- Create: `zfh-virtual-device-frontend/src/views/meter/MeterForm.vue`

- [ ] **Step 1: Create MeterList (similar to GatewayList)**

```vue
<template>
  <div class="meter-list">
    <div class="toolbar">
      <el-button type="primary" @click="handleCreate">
        <el-icon><Plus /></el-icon>新增表计
      </el-button>
    </div>
    
    <el-table :data="meterList" v-loading="loading" border>
      <el-table-column prop="name" label="表计名称" />
      <el-table-column prop="meterType" label="类型" width="100">
        <template #default="{ row }">
          <el-tag>{{ row.meterType === 'ELECTRIC' ? '电表' : '水表' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="communicationAddress" label="通讯地址" />
      <el-table-column prop="protocol" label="协议" width="100" />
      
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">
            {{ getStatusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      
      <el-table-column prop="autoReport" label="自动上报" width="100">
        <template #default="{ row }">
          <el-tag :type="row.autoReport ? 'success' : 'info'">
            {{ row.autoReport ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      
      <el-table-column label="操作" width="250" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 'OFFLINE'"
            type="success"
            size="small"
            @click="handleStart(row)"
          >
            启动
          </el-button>
          <el-button
            v-else
            type="warning"
            size="small"
            @click="handleStop(row)"
          >
            停止
          </el-button>
          <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <el-pagination
      v-model:current-page="page.current"
      v-model:page-size="page.size"
      :total="page.total"
      layout="total, prev, pager, next"
      @change="loadData"
    />
    
    <MeterForm
      v-model:visible="formVisible"
      :data="currentRow"
      @success="loadData"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMeterList, startMeter, stopMeter, deleteMeter } from '../../api/meter'
import MeterForm from './MeterForm.vue'

const loading = ref(false)
const meterList = ref([])
const formVisible = ref(false)
const currentRow = ref(null)

const page = reactive({
  current: 1,
  size: 10,
  total: 0
})

const getStatusType = (status) => {
  const map = { ONLINE: 'success', OFFLINE: 'info', ERROR: 'danger' }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = { ONLINE: '在线', OFFLINE: '离线', ERROR: '异常' }
  return map[status] || status
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getMeterList({ current: page.current, size: page.size })
    meterList.value = res.data.records
    page.total = res.data.total
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleCreate = () => {
  currentRow.value = null
  formVisible.value = true
}

const handleEdit = (row) => {
  currentRow.value = row
  formVisible.value = true
}

const handleStart = async (row) => {
  try {
    await startMeter(row.id)
    ElMessage.success('启动成功')
    loadData()
  } catch (error) {
    ElMessage.error('启动失败')
  }
}

const handleStop = async (row) => {
  try {
    await stopMeter(row.id)
    ElMessage.success('停止成功')
    loadData()
  } catch (error) {
    ElMessage.error('停止失败')
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确认删除该表计？', '提示', { type: 'warning' })
    await deleteMeter(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(loadData)
</script>

<style scoped>
.meter-list {
  .toolbar {
    margin-bottom: 20px;
  }
}
</style>
```

- [ ] **Step 2: Create MeterForm**

```vue
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
```

- [ ] **Step 3: Commit**

```bash
git add zfh-virtual-device-frontend/src/views/meter/
git commit -m "feat: add meter management pages"
```

---

## Chunk 5: Communication Log Page

### Task 7: Log List Page with Real-time Updates

**Files:**
- Create: `zfh-virtual-device-frontend/src/views/log/LogList.vue`
- Create: `zfh-virtual-device-frontend/src/stores/websocket.js`

- [ ] **Step 1: Create WebSocket store**

```javascript
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useAuthStore } from './auth'

export const useWebSocketStore = defineStore('websocket', () => {
  const client = ref(null)
  const connected = ref(false)
  const deviceStatus = ref([])
  const commLogs = ref([])
  const stats = ref({})

  const connect = () => {
    const authStore = useAuthStore()
    if (!authStore.token) return

    const stompClient = new Client({
      webSocketFactory: () => new SockJS(`/ws/virtual-device?token=${authStore.token}`),
      reconnectDelay: 5000,
      onConnect: () => {
        connected.value = true
        console.log('WebSocket connected')

        // Subscribe to device status
        stompClient.subscribe('/topic/device-status', (message) => {
          deviceStatus.value.push(JSON.parse(message.body))
        })

        // Subscribe to communication logs
        stompClient.subscribe('/topic/comm-logs', (message) => {
          commLogs.value.unshift(JSON.parse(message.body))
          // Keep last 100 logs
          if (commLogs.value.length > 100) {
            commLogs.value = commLogs.value.slice(0, 100)
          }
        })

        // Subscribe to stats
        stompClient.subscribe('/topic/stats', (message) => {
          stats.value = JSON.parse(message.body)
        })
      },
      onDisconnect: () => {
        connected.value = false
        console.log('WebSocket disconnected')
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame)
      }
    })

    stompClient.activate()
    client.value = stompClient
  }

  const disconnect = () => {
    client.value?.deactivate()
    connected.value = false
  }

  return {
    client,
    connected,
    deviceStatus,
    commLogs,
    stats,
    connect,
    disconnect
  }
})
```

- [ ] **Step 2: Create LogList page**

```vue
<template>
  <div class="log-list">
    <div class="toolbar">
      <el-button type="danger" @click="handleClear">
        <el-icon><Delete /></el-icon>清空日志
      </el-button>
      <el-tag :type="wsStore.connected ? 'success' : 'danger'">
        WebSocket: {{ wsStore.connected ? '已连接' : '未连接' }}
      </el-tag>
    </div>
    
    <el-table :data="logList" v-loading="loading" border height="600">
      <el-table-column prop="timestamp" label="时间" width="180" />
      <el-table-column prop="deviceType" label="设备类型" width="100">
        <template #default="{ row }">
          <el-tag>{{ row.deviceType === 'GATEWAY' ? '网关' : '表计' }}</el-tag>
        </template>
      </el-table-column>
      
      <el-table-column prop="deviceId" label="设备ID" width="80" />
      
      <el-table-column prop="direction" label="方向" width="80">
        <template #default="{ row }">
          <el-tag :type="row.direction === 'UP' ? 'success' : 'primary'">
            {{ row.direction === 'UP' ? '↑ 上行' : '↓ 下行' }}
          </el-tag>
        </template>
      </el-table-column>
      
      <el-table-column prop="protocol" label="协议" width="100" />
      
      <el-table-column prop="rawData" label="原始报文" show-overflow-tooltip />
      
      <el-table-column prop="parsedData" label="解析数据" show-overflow-tooltip />
    </el-table>
    
    <el-pagination
      v-model:current-page="page.current"
      v-model:page-size="page.size"
      :total="page.total"
      layout="total, prev, pager, next"
      @change="loadData"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getLogList, clearLogs } from '../../api/log'
import { useWebSocketStore } from '../../stores/websocket'

const loading = ref(false)
const logList = ref([])
const wsStore = useWebSocketStore()

const page = reactive({
  current: 1,
  size: 20,
  total: 0
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await getLogList({
      current: page.current,
      size: page.size
    })
    logList.value = res.data.records
    page.total = res.data.total
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleClear = async () => {
  try {
    await ElMessageBox.confirm('确认清空所有日志？', '提示', { type: 'warning' })
    await clearLogs()
    ElMessage.success('清空成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('清空失败')
    }
  }
}

// Watch for real-time WebSocket logs
watch(() => wsStore.commLogs, (newLogs) => {
  if (newLogs.length > 0 && page.current === 1) {
    // Prepend new logs to the list
    logList.value = [...newLogs.slice(0, 10), ...logList.value].slice(0, page.size)
  }
}, { deep: true })

onMounted(() => {
  loadData()
  wsStore.connect()
})

onUnmounted(() => {
  wsStore.disconnect()
})
</script>

<style scoped>
.log-list {
  .toolbar {
    margin-bottom: 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}
</style>
```

- [ ] **Step 3: Commit**

```bash
git add zfh-virtual-device-frontend/src/views/log/
git add zfh-virtual-device-frontend/src/stores/websocket.js
git commit -m "feat: add communication log page with WebSocket"
```

---

## Chunk 6: Integration & Build

### Task 8: Build and Integration Test

- [ ] **Step 1: Update MainLayout to connect WebSocket on login**

Modify: `zfh-virtual-device-frontend/src/layouts/MainLayout.vue`

Add in script setup:
```javascript
import { onMounted } from 'vue'
import { useWebSocketStore } from '../stores/websocket'

const wsStore = useWebSocketStore()

onMounted(() => {
  wsStore.connect()
})
```

- [ ] **Step 2: Build for production**

```bash
cd zfh-virtual-device-frontend
npm run build
```

Expected: `dist/` folder created with built assets.

- [ ] **Step 3: Test complete workflow**

1. Start backend: `cd zfh-virtual-device-backend && mvn spring-boot:run`
2. Start frontend: `cd zfh-virtual-device-frontend && npm run dev`
3. Open http://localhost:5173
4. Login with admin/admin123
5. Create a gateway
6. Create a meter (ELECTRIC type)
7. Start the meter
8. Navigate to Logs page
9. Verify real-time logs appear via WebSocket

- [ ] **Step 4: Commit**

```bash
git add zfh-virtual-device-frontend/
git commit -m "feat: complete frontend MVP implementation"
```

---

**Frontend plan complete and saved to `docs/superpowers/plans/2026-04-18-virtual-device-frontend.md`.**

**Summary:**
- **Chunk 1:** Project setup with Vite, Vue 3, Element Plus, router, layout, login page
- **Chunk 2:** API client (Axios), Pinia stores (auth, WebSocket), API modules
- **Chunk 3:** Gateway management (list, create, edit, delete, start/stop)
- **Chunk 4:** Meter management (list, create, edit, delete, start/stop)
- **Chunk 5:** Communication logs with real-time WebSocket updates
- **Chunk 6:** Build, integration test, and WebSocket lifecycle management

**Next Steps:**
1. Review frontend plan
2. Execute backend chunks (8 chunks total)
3. Execute frontend chunks (6 tasks total)
4. Integration testing

Do you want to proceed with execution?