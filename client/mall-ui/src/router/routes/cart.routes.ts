import type { RouteRecordRaw } from 'vue-router'

const cartRoutes: RouteRecordRaw[] = [
  {
    path: 'cart',
    name: 'cart',
    component: () => import('@/pages/cart/CartPage/CartPage.vue'),
    meta: { requiresAuth: false },
  },
]

export default cartRoutes
