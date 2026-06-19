import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { search, suggest } from '@/api/search'
import type { SearchItemVO, SearchResultVO, AggregationBucket } from '@/types'

interface FilterState {
  categoryIds: number[]
  brandIds: number[]
  priceMin: number | null
  priceMax: number | null
  sort: string
  sortOrder: string
}

const DEFAULT_FILTERS: FilterState = {
  categoryIds: [],
  brandIds: [],
  priceMin: null,
  priceMax: null,
  sort: 'default',
  sortOrder: 'desc',
}

const SORT_MAP: Record<string, { sort?: string; sortOrder?: string }> = {
  default: {},
  sales: { sort: 'salesCount', sortOrder: 'desc' },
  price_asc: { sort: 'price', sortOrder: 'ASC' },
  price_desc: { sort: 'price', sortOrder: 'DESC' },
  newest: { sort: 'createTime', sortOrder: 'desc' },
}

export const useSearchStore = defineStore('search', () => {
  const keyword = ref('')
  const results = ref<SearchItemVO[]>([])
  const total = ref(0)
  const page = ref(1)
  const size = ref(20)
  const loading = ref(false)
  const suggestions = ref<string[]>([])
  const filters = ref<FilterState>({ ...DEFAULT_FILTERS })
  const aggregations = ref<{ categories: AggregationBucket[]; brands: AggregationBucket[] }>({
    categories: [],
    brands: [],
  })

  const noMore = computed(() => results.value.length >= total.value)

  /** 执行搜索（page=1 替换结果，page>1 追加结果） */
  async function doSearch(kw?: string) {
    if (kw !== undefined) keyword.value = kw
    loading.value = true
    try {
      const mapped = SORT_MAP[filters.value.sort] ?? {}
      const res: SearchResultVO = await search({
        keyword: keyword.value,
        page: page.value,
        size: size.value,
        categoryIds: filters.value.categoryIds.length > 0 ? filters.value.categoryIds : undefined,
        brandIds: filters.value.brandIds.length > 0 ? filters.value.brandIds : undefined,
        priceMin: filters.value.priceMin,
        priceMax: filters.value.priceMax,
        ...mapped,
      })
      if (page.value === 1) {
        results.value = res.items
      } else {
        results.value = [...results.value, ...res.items]
      }
      total.value = res.total
      aggregations.value = res.aggregations ?? { categories: [], brands: [] }
    } finally {
      loading.value = false
    }
  }

  /** 加载更多（翻页并追加） */
  async function loadMore() {
    if (noMore.value || loading.value) return
    page.value++
    await doSearch()
  }

  /** 获取搜索建议 */
  async function fetchSuggestions(kw: string) {
    if (!kw) {
      suggestions.value = []
      return
    }
    suggestions.value = await suggest(kw)
  }

  /** 重置筛选条件 */
  function resetFilters() {
    filters.value = { ...DEFAULT_FILTERS }
    page.value = 1
  }

  return {
    keyword,
    results,
    total,
    page,
    size,
    loading,
    suggestions,
    filters,
    aggregations,
    noMore,
    doSearch,
    loadMore,
    fetchSuggestions,
    resetFilters,
  }
})
