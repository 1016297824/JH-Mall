import type { RouteRecordRaw } from 'vue-router'

const homeRoutes: RouteRecordRaw[] = [
  {
    path: '',
    name: 'home',
    component: () => import('@/pages/home/HomePage/HomePage.vue'),
    meta: { requiresAuth: false },
  },
]

export default homeRoutes
