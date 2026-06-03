import { createPinia } from 'pinia'

export const pinia = createPinia()

export { useAuthStore } from './auth.store'
export { useUserStore } from './user.store'
export { useProductStore } from './product.store'
export { useSearchStore } from './search.store'
export { useOrderStore } from './order.store'
export { usePaymentStore } from './payment.store'
export { useMarketingStore } from './marketing.store'
