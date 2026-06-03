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
  hotScore?: number
}

export interface SkuBriefVO {
  skuId: string
  skuName: string
  price: number
  image: string
}

export interface SpuDetailVO extends SpuVO {
  description: string
  images: string[]
  reviewCount: number
  skus: SkuBriefVO[]
}
