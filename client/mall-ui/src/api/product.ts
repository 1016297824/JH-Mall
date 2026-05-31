import request from './request'
import type { CategoryVO, SpuVO, PageResult } from '@/types/product'

export function getCategoryTree(): Promise<CategoryVO[]> {
  return request.get('/product/categories').then((res) => res.data.data)
}

export function getSpuList(params: {
  page?: number
  size?: number
  categoryId?: number
  brandId?: number
  keyword?: string
  sort?: string
}): Promise<PageResult<SpuVO>> {
  return request.get('/product/spus', { params }).then((res) => res.data.data)
}
