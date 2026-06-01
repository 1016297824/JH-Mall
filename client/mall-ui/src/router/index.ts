import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: () => import('@/layouts/AppLayout.vue'),
      children: [
        {
          path: '',
          name: 'home',
          component: () => import('@/pages/home/HomePage.vue'),
          meta: { requiresAuth: false },
        },
        {
          path: 'categories',
          name: 'categories',
          component: () => import('@/pages/product/CategoryPage.vue'),
          meta: { requiresAuth: false },
        },
        {
          path: 'categories/:id',
          name: 'categoryProducts',
          component: () => import('@/pages/product/ProductListPage.vue'),
          meta: { requiresAuth: false },
        },
        {
          path: 'spus/:id',
          name: 'productDetail',
          component: () => import('@/pages/product/ProductDetailPage.vue'),
          meta: { requiresAuth: false },
        },
        {
          path: 'cart',
          name: 'cart',
          component: () => import('@/pages/cart/CartPage.vue'),
          meta: { requiresAuth: false },
        },
        {
          path: 'profile',
          name: 'profile',
          component: () => import('@/pages/user/UserCenterPage.vue'),
          meta: { requiresAuth: false },
        },
      ],
    },
  ],
})

export default router
