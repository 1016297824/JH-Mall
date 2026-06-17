import request from './client'

/** 加入购物车 */
export function postCartItem(params: { skuId: string; quantity: number }): Promise<void> {
  return request.post('/order/cart/items', params).then((res) => res.data.data)
}
