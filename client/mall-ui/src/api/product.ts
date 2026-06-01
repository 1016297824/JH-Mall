import request from './request'
import type { CategoryVO, SpuVO, SpuDetailVO, PageResult } from '@/types/product'

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

export function getHotList(limit: number): Promise<SpuVO[]> {
  return request.get('/product/spus/hot', { params: { limit } }).then((res) => res.data.data)
}

export function getSpuDetail(spuId: string): Promise<SpuDetailVO> {
  return request.get(`/product/spus/${spuId}`).then((res) => res.data.data)
}
