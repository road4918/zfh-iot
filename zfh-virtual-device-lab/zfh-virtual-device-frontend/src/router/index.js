import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
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
