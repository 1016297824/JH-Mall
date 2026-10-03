import type { RouteRecordRaw } from 'vue-router'

const userRoutes: RouteRecordRaw[] = [
  {
    path: 'profile',
    name: 'profile',
    component: () => import('@/pages/user/UserCenterPage/UserCenterPage.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: 'address',
    name: 'addressList',
    component: () => import('@/pages/user/AddressListPage/AddressListPage.vue'),
    meta: { requiresAuth: true },
  },
]

export default userRoutes
