import type { RouteRecordRaw } from 'vue-router'

const orderRoutes: RouteRecordRaw[] = [
  {
    path: 'cart',
    name: 'cart',
    component: () => import('@/pages/order/CartPage/CartPage.vue'),
    meta: { requiresAuth: false },
  },
]

export default orderRoutes
