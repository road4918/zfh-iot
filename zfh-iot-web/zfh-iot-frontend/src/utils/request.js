import axios from 'axios'
import { ElMessage } from 'element-plus'
import Cookies from 'js-cookie'
import router from '@/router'

const service = axios.create({
  baseURL: import.meta.env.VITE_APP_BASE_API || '/api/v1',
  timeout: 10000
})

service.interceptors.request.use(
  config => {
    const token = Cookies.get('token')
    if (token) {
      config.headers['Authorization'] = 'Bearer ' + token
    }
    return config
  },
  error => {
    console.error(error)
    return Promise.reject(error)
  }
)

service.interceptors.response.use(
  response => {
    const res = response.data
    
    if (res.code !== 200) {
      ElMessage.error(res.message || 'Request failed')
      
      if (res.code === 401) {
        Cookies.remove('token')
        router.push('/login')
      }
      
      return Promise.reject(new Error(res.message || 'Request failed'))
    }
    
    return res.data
  },
  error => {
    console.error('err', error)
    ElMessage.error(error.message || 'Request failed')
    return Promise.reject(error)
  }
)

export default service
