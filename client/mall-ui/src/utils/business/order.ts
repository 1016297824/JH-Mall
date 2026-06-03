// 订单相关业务工具

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
