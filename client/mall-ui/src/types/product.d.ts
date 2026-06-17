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

/** SKU 简要信息（SPU 详情中的 SKU 列表） */
export interface SkuBriefVO {
  skuId: string
  skuName: string
  price: number
  image: string
}

/** SKU 完整详情（含库存、划线价、规格属性） */
export interface SkuVO {
  skuId: string
  spuId: string
  skuCode: string
  skuName: string
  attrsJson: string        // 规格属性 JSON，如 {"颜色":"金色","存储":"128GB"}
  price: number
  marketPrice: number       // 划线价（分）
  image: string
  weight: number
  availableStock: number    // 可用库存
}

/** SPU 详情 */
export interface SpuDetailVO extends SpuVO {
  description: string
  images: string[]
  reviewCount: number
  skus: SkuBriefVO[]
}

// ==================== UI 状态专用类型（非后端 VO） ====================

/** 规格值 */
export interface SpecValueVO {
  value: string
  disabled: boolean
}

/** 规格分组（UI 层从 SKU 名称派生） */
export interface SpecGroupVO {
  specName: string
  values: SpecValueVO[]
}

/** 选中的规格组合 */
export interface SelectedSpecs {
  [specName: string]: string
}
