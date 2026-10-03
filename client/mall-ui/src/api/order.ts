import request from './client'

/** 加入购物车 */
export function postCartItem(params: { skuId: string; quantity: number }): Promise<void> {
  return request.post('/order/cart/items', params).then((res) => res.data.data)
}

/**
 * 购物车项
 *
 * <p>金额单位为分；Long 型 ID 用 string 承载，避免 JS 精度丢失。
 * 字段与后端 {@code CartVO} 一一对应。</p>
 */
export interface CartItem {
  id: string
  skuId: string
  spuId: string
  skuCode: string
  skuName: string
  mainImage: string
  /** 单价（分） */
  price: number
  quantity: number
  /** 0=未选中 1=已选中 */
  isSelected: number
  /** 可售库存 */
  availableQty: number
  onSale: boolean
  /** 是否可购买（在售且有货） */
  purchasable: boolean
}

/** 查询购物车 */
export function getCartItems(): Promise<CartItem[]> {
  return request.get('/order/cart/items').then((res) => res.data.data)
}

/** 修改数量 */
export function putCartItemQuantity(id: string, quantity: number): Promise<void> {
  return request.put(`/order/cart/items/${id}`, { quantity }).then((res) => res.data.data)
}

/** 勾选 / 取消勾选 */
export function putCartItemSelected(id: string, isSelected: boolean): Promise<void> {
  return request.put(`/order/cart/items/${id}/selected`, { isSelected }).then((res) => res.data.data)
}

/** 删除单项 */
export function deleteCartItem(id: string): Promise<void> {
  return request.delete(`/order/cart/items/${id}`).then((res) => res.data.data)
}

/** 清空购物车 */
export function deleteCartItems(): Promise<void> {
  return request.delete('/order/cart/items').then((res) => res.data.data)
}

/** 下单请求参数 */
export interface CreateOrderParams {
  /** 收货地址 ID（string 承载 Long，避免 JS 精度丢失） */
  addressId: string
  /** 使用的优惠券记录 ID，可空；为空则由后端试算引擎自动择优 */
  couponRecordId?: string
  /** 买家备注，最长 200 字符 */
  remark?: string
}

/**
 * 创建订单
 *
 * <p>商品明细不由前端提交：后端按当前用户<b>已勾选的购物车项</b>下单，
 * 因此调用前必须确保购物车勾选状态已落库。</p>
 *
 * <p><b>幂等键必须由调用方持有并复用</b>：同一笔下单的重复提交（网络重试、
 * 用户连点）必须携带同一个 key，后端才会返回同一张订单；每次新下单才换新 key。
 * 放在本函数内部生成会让重试变成「又下了一单」。</p>
 *
 * @param params 下单参数
 * @param idempotentKey 幂等键，请求头 Idempotent-Key
 * @returns 订单号
 */
export function createOrder(params: CreateOrderParams, idempotentKey: string): Promise<string> {
  return request
    .post('/order/orders', params, { headers: { 'Idempotent-Key': idempotentKey } })
    .then((res) => res.data.data)
}

/** 订单项（下单时刻快照，金额单位为分） */
export interface OrderItem {
  id: string
  spuId: string
  skuId: string
  skuCode: string
  skuName: string
  spuName: string
  mainImage: string
  /** 销售属性 JSON 快照 */
  attrsJson: string
  quantity: number
  /** 成交单价（分） */
  price: number
  /** 单项总价（分） */
  totalPrice: number
}

/** 订单 */
export interface Order {
  id: string
  orderNo: string
  /** 订单状态码，取值见后端 OrderStatusEnum */
  orderStatus: number
  orderStatusDesc: string
  /** 商品总金额（分） */
  totalAmount: number
  /** 优惠总金额（分） */
  discountAmount: number
  /** 运费（分） */
  freightAmount: number
  /** 实付金额（分） */
  payAmount: number
  /** 支付过期时间，ISO 字符串 */
  payExpireTime: string
  cancelType: string
  cancelReason: string
  logisticsCompany: string
  logisticsNo: string
  remark: string
  createTime: string
  items: OrderItem[]
  /** 当前可执行的操作码，由后端状态机推导 */
  actions: string[]
  /** 是否待支付（后端计算属性，供前端展示倒计时） */
  waitPay: boolean
}

/**
 * 查询我的订单列表
 *
 * @param status 状态过滤，不传为全部
 * @param page 页码，从 1 开始
 * @param size 每页条数，服务端上限 50
 * @returns 订单列表
 */
export function getOrders(status?: number, page = 1, size = 10): Promise<Order[]> {
  return request.get('/order/orders', { params: { status, page, size } }).then((res) => res.data.data)
}

/**
 * 查询订单详情
 *
 * @param orderNo 订单号
 * @returns 订单详情
 */
export function getOrderDetail(orderNo: string): Promise<Order> {
  return request.get(`/order/orders/${orderNo}`).then((res) => res.data.data)
}

/**
 * 取消订单
 *
 * @param orderNo 订单号
 */
export function cancelOrder(orderNo: string): Promise<void> {
  return request.post(`/order/orders/${orderNo}/cancellation`).then((res) => res.data.data)
}

/**
 * 确认收货
 *
 * @param orderNo 订单号
 */
export function confirmReceipt(orderNo: string): Promise<void> {
  return request.put(`/order/orders/${orderNo}/receipt`).then((res) => res.data.data)
}
