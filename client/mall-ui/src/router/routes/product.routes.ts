import type { RouteRecordRaw } from 'vue-router'

const productRoutes: RouteRecordRaw[] = [
  {
    path: 'categories',
    name: 'categories',
    component: () => import('@/pages/product/CategoryPage/CategoryPage.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: 'categories/:id',
    name: 'categoryProducts',
    component: () => import('@/pages/product/ProductListPage/ProductListPage.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: 'spus/:id',
    name: 'productDetail',
    component: () => import('@/pages/product/ProductDetailPage/ProductDetailPage.vue'),
    meta: { requiresAuth: false },
  },
]

export default productRoutes
