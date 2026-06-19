import type { RouteRecordRaw } from 'vue-router'

const searchRoutes: RouteRecordRaw[] = [
  {
    path: 'search',
    name: 'search',
    component: () => import('@/pages/search/SearchPage/SearchPage.vue'),
    meta: { requiresAuth: false },
  },
]

export default searchRoutes
