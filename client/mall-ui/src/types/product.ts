export interface MallResult<T> {
  errorCode: string
  errorMessage: string
  data: T
}

export interface PageResult<T> {
  page: number
  size: number
  total: number
  records: T[]
}

export interface CategoryVO {
  categoryId: string
  parentId: string
  name: string
  level: number
  icon: string | null
  sortOrder: number
  path: string
  children?: CategoryVO[]
}

export interface SpuVO {
  spuId: string
  spuName: string
  mainImage: string
  priceMin: number
  priceMax: number
  salesCount: number
  categoryId: string
  brandId: string
}
