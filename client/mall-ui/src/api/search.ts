import request from './client'
import type { SearchReqDTO, SearchResultVO } from '@/types'

/**
 * 商品搜索
 * @param params - 搜索请求参数
 * @returns 搜索结果（含聚合）
 */
export async function search(params: SearchReqDTO): Promise<SearchResultVO> {
  const { data } = await request.post('/api/search', params)
  return data.data
}

/**
 * 搜索建议
 * @param keyword - 搜索关键词
 * @returns 建议词列表
 */
export async function suggest(keyword: string): Promise<string[]> {
  const { data } = await request.get('/api/search/suggest', { params: { keyword } })
  return data.data
}
