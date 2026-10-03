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
  {
    path: 'points',
    name: 'points',
    component: () => import('@/pages/user/PointsPage/PointsPage.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: 'membership',
    name: 'membership',
    component: () => import('@/pages/user/MembershipPage/MembershipPage.vue'),
    meta: { requiresAuth: true },
  },
]

export default userRoutes
