export function formatPrice(priceInCents: number): string {
  return (priceInCents / 100).toFixed(2)
}

export function formatDate(dateStr: string): string {
  const date = new Date(dateStr)
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

export function formatOrderStatus(status: string): string {
  const map: Record<string, string> = {
    WAIT_PAY: '待付款',
    PAID: '已支付',
    WAIT_DELIVER: '待发货',
    WAIT_RECEIVE: '待收货',
    COMPLETED: '已完成',
    CANCELLED: '已取消',
    CLOSED: '已关闭',
    AFTER_SALE: '售后中',
    REFUNDED: '已退款',
  }
  return map[status] || status
}
