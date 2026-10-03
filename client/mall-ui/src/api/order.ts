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
