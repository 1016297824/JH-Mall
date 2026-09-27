import { createRouter, createWebHistory } from 'vue-router'
import routes from './routes'
import authRoutes from './routes/auth.routes'
import { registerGuards } from './guards'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    ...authRoutes,
    {
      path: '/',
      component: () => import('@/layouts/AppLayout.vue'),
      children: routes,
    },
  ],
  scrollBehavior() {
    return { top: 0 }
  },
})

registerGuards(router)

export default router
