import type { RouteRecordRaw } from 'vue-router'

/** 认证路由：顶级路由，不套 AppLayout（全屏无底部 TabBar） */
const authRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/pages/auth/LoginPage/LoginPage.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/pages/auth/RegisterPage/RegisterPage.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/forgot',
    name: 'forgot',
    component: () => import('@/pages/auth/ForgotPasswordPage/ForgotPasswordPage.vue'),
    meta: { requiresAuth: false },
  },
]

export default authRoutes
