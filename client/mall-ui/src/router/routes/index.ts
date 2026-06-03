import homeRoutes from './home.routes'
import productRoutes from './product.routes'
import cartRoutes from './cart.routes'
import userRoutes from './user.routes'

const routes = [
  ...homeRoutes,
  ...productRoutes,
  ...cartRoutes,
  ...userRoutes,
]

export default routes
