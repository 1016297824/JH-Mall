export const BannerLinkType = {
  CATEGORY: 'CATEGORY',
  PRODUCT: 'PRODUCT',
  URL: 'URL',
} as const

export type BannerLinkType = (typeof BannerLinkType)[keyof typeof BannerLinkType]
