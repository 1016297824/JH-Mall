/** 搜索结果单项 */
export interface SearchItemVO {
  spuId: number
  spuName: string
  spuNameHighlight?: string
  price: string
  image: string
  salesCount: number
}

/** 聚合桶（筛选分组统计） */
export interface AggregationBucket {
  key: string
  name: string
  count: number
}

/** 搜索结果VO */
export interface SearchResultVO {
  items: SearchItemVO[]
  total: number
  page: number
  size: number
  aggregations?: {
    categories: AggregationBucket[]
    brands: AggregationBucket[]
  }
}

/** 搜索请求DTO */
export interface SearchReqDTO {
  keyword?: string
  categoryIds?: number[]
  brandIds?: number[]
  priceMin?: number | null
  priceMax?: number | null
  sort?: string
  sortOrder?: string
  page?: number
  size?: number
}
