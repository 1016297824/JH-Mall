import type { RouteRecordRaw } from 'vue-router'

const userRoutes: RouteRecordRaw[] = [
  {
    path: 'profile',
    name: 'profile',
    component: () => import('@/pages/user/UserCenterPage/UserCenterPage.vue'),
    meta: { requiresAuth: false },
  },
]

export default userRoutes
