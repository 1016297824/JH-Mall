import type { RouteRecordRaw } from 'vue-router'

const paymentRoutes: RouteRecordRaw[] = [
  {
    path: 'payment/result',
    name: 'paymentResult',
    component: () => import('@/pages/payment/PaymentResultPage/PaymentResultPage.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: 'payment/:orderNo',
    name: 'payment',
    component: () => import('@/pages/payment/PaymentPage/PaymentPage.vue'),
    meta: { requiresAuth: true },
  },
]

export default paymentRoutes
