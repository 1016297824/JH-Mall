import type { RouteRecordRaw } from 'vue-router'

const marketingRoutes: RouteRecordRaw[] = [
  {
    path: 'coupons',
    name: 'couponCenter',
    component: () => import('@/pages/marketing/CouponCenterPage/CouponCenterPage.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: 'coupons/mine',
    name: 'myCoupons',
    component: () => import('@/pages/marketing/MyCouponsPage/MyCouponsPage.vue'),
    meta: { requiresAuth: true },
  },
]

export default marketingRoutes
