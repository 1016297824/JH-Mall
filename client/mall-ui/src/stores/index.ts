import { createPinia } from 'pinia'

export const pinia = createPinia()

export { useAuthStore } from './auth.store'
export { useCartStore } from './cart.store'
export { useOrderStore } from './order.store'
