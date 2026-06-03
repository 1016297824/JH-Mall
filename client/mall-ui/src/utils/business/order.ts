import { ORDER_STATUS } from '../enums/order.enum'

export function formatOrderStatus(status: string): string {
  return ORDER_STATUS[status] || status
}
