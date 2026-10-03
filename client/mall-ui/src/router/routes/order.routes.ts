import type { RouteRecordRaw } from 'vue-router'

const orderRoutes: RouteRecordRaw[] = [
  {
    path: 'cart',
    name: 'cart',
    component: () => import('@/pages/order/CartPage/CartPage.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: 'checkout',
    name: 'checkout',
    component: () => import('@/pages/order/CheckoutPage/CheckoutPage.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: 'orders',
    name: 'orderList',
    component: () => import('@/pages/order/OrderListPage/OrderListPage.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: 'orders/:orderNo',
    name: 'orderDetail',
    component: () => import('@/pages/order/OrderDetailPage/OrderDetailPage.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: 'after-sales',
    name: 'afterSale',
    component: () => import('@/pages/order/AfterSalePage/AfterSalePage.vue'),
    meta: { requiresAuth: true },
  },
]

export default orderRoutes
