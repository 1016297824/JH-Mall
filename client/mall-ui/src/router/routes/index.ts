import productRoutes from './product.routes'
import searchRoutes from './search.routes'
import orderRoutes from './order.routes'
import paymentRoutes from './payment.routes'
import userRoutes from './user.routes'
import marketingRoutes from './marketing.routes'

const routes = [
  ...productRoutes,
  ...searchRoutes,
  ...orderRoutes,
  ...paymentRoutes,
  ...userRoutes,
  ...marketingRoutes,
]

export default routes
