<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useSearchStore } from '@/stores'
import type { AggregationBucket } from '@/types'

const SORT_OPTIONS = [
  { label: '综合', value: 'default' },
  { label: '销量', value: 'sales' },
  { label: '价格升', value: 'price_asc' },
  { label: '价格降', value: 'price_desc' },
  { label: '新品', value: 'newest' },
] as const

const SKELETON_COUNT = 6

const route = useRoute()
const router = useRouter()
const store = useSearchStore()

const colorBorder = '#BAE6FD' // v.$color-border，Template 无法直接引用 SCSS 变量

const localKeyword = ref('')
const showSuggestions = ref(false)
const drawerVisible = ref(false)
const suggestTimer = ref<ReturnType<typeof setTimeout> | null>(null)
const suggestionRefs = ref<HTMLElement[]>([])

// 从 Store 解构常用状态
const loading = computed(() => store.loading)
const results = computed(() => store.results)
const total = computed(() => store.total)
const noMore = computed(() => store.noMore)
const suggestions = computed(() => store.suggestions)
const aggregations = computed(() => store.aggregations)

const sort = computed({
  get: () => store.filters.sort,
  set: (val: string) => {
    store.filters.sort = val
    store.page = 1
    store.doSearch().catch(() => ElMessage.error('搜索失败，请稍后重试'))
    window.scrollTo({ top: 0, behavior: 'smooth' })
  },
})

const selectedCategoryIds = ref<number[]>([])
const selectedBrandIds = ref<number[]>([])
const priceMinInput = ref<number | null>(null)
const priceMaxInput = ref<number | null>(null)

/** 已选筛选项的名称缓存（用于在新聚合中不存在时补回） */
const selectedCategoryNames = ref<Record<number, string>>({})
const selectedBrandNames = ref<Record<number, string>>({})

/** 合并聚合：新聚合 + 已选但不在新聚合中的筛选项（count=0 兜底保留） */
const effectiveAggregations = computed(() => {
  const aggs = aggregations.value
  const cats = [...aggs.categories]
  const brands = [...aggs.brands]
  for (const cid of selectedCategoryIds.value) {
    if (!cats.some((c) => Number(c.key) === cid)) {
      const name = selectedCategoryNames.value[cid] ?? String(cid)
      cats.push({ key: String(cid), name, count: 0 })
    }
  }
  for (const bid of selectedBrandIds.value) {
    if (!brands.some((b) => Number(b.key) === bid)) {
      const name = selectedBrandNames.value[bid] ?? String(bid)
      brands.push({ key: String(bid), name, count: 0 })
    }
  }
  return { categories: cats, brands }
})

/** 是否有筛选条件激活 */
const hasActiveFilters = computed(
  () =>
    selectedCategoryIds.value.length > 0 ||
    selectedBrandIds.value.length > 0 ||
    priceMinInput.value !== null ||
    priceMaxInput.value !== null,
)

/** 同步筛选值到 Store 并搜索 */
function applyFilters() {
  store.filters.categoryIds = [...selectedCategoryIds.value]
  store.filters.brandIds = [...selectedBrandIds.value]
  // 前端输入为元，ES 索引以分为单位存储，需 ×100 转换
  store.filters.priceMin = priceMinInput.value !== null ? priceMinInput.value * 100 : null
  store.filters.priceMax = priceMaxInput.value !== null ? priceMaxInput.value * 100 : null
  store.page = 1
  store.doSearch().catch(() => ElMessage.error('筛选失败，请稍后重试'))
  closeFilterMobile()
}

/** 重置筛选 */
function handleResetFilters() {
  selectedCategoryIds.value = []
  selectedBrandIds.value = []
  priceMinInput.value = null
  priceMaxInput.value = null
  selectedCategoryNames.value = {}
  selectedBrandNames.value = {}
  store.resetFilters()
  store.doSearch().catch(() => ElMessage.error('搜索失败，请稍后重试'))
}

/** 处理搜索输入确认 */
function handleSearchSubmit() {
  showSuggestions.value = false
  if (localKeyword.value.trim()) {
    router.replace({ query: { keyword: localKeyword.value.trim() } })
  } else {
    router.replace({ query: {} })
  }
  store.page = 1
  store.doSearch(localKeyword.value.trim()).catch(() => ElMessage.error('搜索失败，请稍后重试'))
}

/** 输入变化时获取建议（防抖） */
function handleKeywordInput(val: string) {
  if (suggestTimer.value) {
    clearTimeout(suggestTimer.value)
  }
  if (!val.trim()) {
    showSuggestions.value = false
    store.suggestions = []
    return
  }
  suggestTimer.value = setTimeout(() => {
    store.fetchSuggestions(val.trim())
    if (store.suggestions.length > 0) {
      showSuggestions.value = true
    }
  }, 200)
}

/** 点击建议词 */
function handleSuggestionClick(suggestion: string) {
  localKeyword.value = suggestion
  showSuggestions.value = false
  handleSearchSubmit()
}

/** 键盘导航建议词 */
function handleSuggestionKeydown(index: number) {
  const refs = suggestionRefs.value
  if (!refs || index < 0 || index >= refs.length) return
  refs[index]?.focus()
}

/** 选择类目（多选切换） */
function handleCategorySelect(cat: AggregationBucket) {
  const id = Number(cat.key)
  const idx = selectedCategoryIds.value.indexOf(id)
  if (idx >= 0) {
    selectedCategoryIds.value.splice(idx, 1)
  } else {
    selectedCategoryIds.value.push(id)
    selectedCategoryNames.value[id] = cat.name
  }
  applyFilters()
}

/** 选择品牌（多选切换） */
function handleBrandSelect(brand: AggregationBucket) {
  const id = Number(brand.key)
  const idx = selectedBrandIds.value.indexOf(id)
  if (idx >= 0) {
    selectedBrandIds.value.splice(idx, 1)
  } else {
    selectedBrandIds.value.push(id)
    selectedBrandNames.value[id] = brand.name
  }
  applyFilters()
}

/** 价格区间确认 */
function handlePriceApply() {
  applyFilters()
}

/** 加载更多 */
function handleLoadMore() {
  store.loadMore().catch(() => ElMessage.error('加载失败，请稍后重试'))
}

/** 关闭移动端筛选面板 */
function closeFilterMobile() {
  drawerVisible.value = false
}

/** 监听 popover 外部点击关闭 */
function handlePopoverHide() {
  showSuggestions.value = false
}

/** 输入框聚焦时显示已有建议 */
function handleInputFocus() {
  if (store.suggestions.length > 0) {
    showSuggestions.value = true
  }
}

// 初始化：读取 URL keyword 参数
onMounted(() => {
  const initialKeyword = (route.query.keyword as string) ?? ''
  localKeyword.value = initialKeyword
  if (initialKeyword) {
    // 从首页导航进入时（携带新 keyword），清空旧筛选条件再搜索
    selectedCategoryIds.value = []
    selectedBrandIds.value = []
    priceMinInput.value = null
    priceMaxInput.value = null
    selectedCategoryNames.value = {}
    selectedBrandNames.value = {}
    store.resetFilters()
    store.doSearch(initialKeyword).catch(() => ElMessage.error('搜索失败，请稍后重试'))
  }
})

// 清理建议定时器
onBeforeUnmount(() => {
  if (suggestTimer.value) {
    clearTimeout(suggestTimer.value)
  }
})
</script>

<template>
  <div class="search-page">
    <!-- 搜索栏 -->
    <div class="search-page__header" role="search" aria-label="商品搜索">
      <el-popover
        :visible="showSuggestions"
        placement="bottom-start"
        :width="480"
        trigger="manual"
        popper-class="search-page__suggestions-popover"
        @click-outside="handlePopoverHide"
      >
        <template #reference>
          <el-input
            v-model="localKeyword"
            placeholder="搜索商品"
            :prefix-icon="Search"
            size="large"
            clearable
            class="search-page__input"
            @input="handleKeywordInput"
            @keyup.enter="handleSearchSubmit"
            @focus="handleInputFocus"
            @clear="handleSearchSubmit"
          >
            <template #append>
              <el-button :icon="Search" @click="handleSearchSubmit">搜索</el-button>
            </template>
          </el-input>
        </template>
        <ul v-if="suggestions.length > 0" class="search-page__suggestions">
          <li
            v-for="(s, i) in suggestions"
            :key="s"
            class="search-page__suggestion-item"
            tabindex="0"
            @click="handleSuggestionClick(s)"
            @keydown.enter.prevent="handleSuggestionClick(s)"
            @keydown.arrow-down.prevent="handleSuggestionKeydown(i + 1)"
            @keydown.arrow-up.prevent="handleSuggestionKeydown(i - 1)"
            @keydown.esc.prevent="handlePopoverHide"
            :ref="(el: unknown) => { if (el) suggestionRefs[i] = el as HTMLElement }"
          >
            <el-icon class="search-page__suggestion-icon"><Search /></el-icon>
            {{ s }}
          </li>
        </ul>
      </el-popover>
    </div>

    <!-- 移动端/平板端筛选栏 -->
    <div class="search-page__filter-bar-mobile">
      <el-button
        :icon="Search"
        :type="hasActiveFilters ? 'primary' : 'default'"
        @click="drawerVisible = true"
      >
        筛选
        <span v-if="hasActiveFilters" class="search-page__filter-dot" />
      </el-button>
      <span class="search-page__total-hint">共 {{ total }} 件</span>
      <el-button
        v-if="hasActiveFilters"
        text
        type="primary"
        size="small"
        @click="handleResetFilters"
      >
        清除筛选
      </el-button>
    </div>

    <!-- 主体：筛选侧栏 + 结果区 -->
    <div class="search-page__body">
      <!-- PC 端筛选侧栏 -->
      <aside class="search-page__sidebar">
        <div class="search-page__filter-section">
          <h3 class="search-page__filter-title">商品类目</h3>
          <ul class="search-page__filter-list" role="listbox" aria-label="商品类目筛选">
            <li
              v-for="cat in effectiveAggregations.categories"
              :key="cat.key"
              class="search-page__filter-item"
              :class="{ 'search-page__filter-item--active': selectedCategoryIds.includes(Number(cat.key)) }"
              :aria-selected="selectedCategoryIds.includes(Number(cat.key))"
              role="option"
              @click="handleCategorySelect(cat)"
            >
              <el-checkbox
                :model-value="selectedCategoryIds.includes(Number(cat.key))"
                size="small"
              />
              <span class="search-page__filter-label">{{ cat.name }}</span>
              <span class="search-page__filter-count">{{ cat.count }}</span>
            </li>
          </ul>
        </div>

        <div class="search-page__filter-section">
          <h3 class="search-page__filter-title">品牌</h3>
          <ul class="search-page__filter-list" role="listbox" aria-label="品牌筛选">
            <li
              v-for="brand in effectiveAggregations.brands"
              :key="brand.key"
              class="search-page__filter-item"
              :class="{ 'search-page__filter-item--active': selectedBrandIds.includes(Number(brand.key)) }"
              :aria-selected="selectedBrandIds.includes(Number(brand.key))"
              role="option"
              @click="handleBrandSelect(brand)"
            >
              <el-checkbox
                :model-value="selectedBrandIds.includes(Number(brand.key))"
                size="small"
              />
              <span class="search-page__filter-label">{{ brand.name }}</span>
              <span class="search-page__filter-count">{{ brand.count }}</span>
            </li>
          </ul>
        </div>

        <div class="search-page__filter-section">
          <h3 class="search-page__filter-title">价格区间</h3>
          <div class="search-page__price-range">
            <el-input-number
              v-model="priceMinInput"
              :min="0"
              placeholder="最低价"
              :controls="false"
              size="small"
              inputmode="numeric"
            />
            <span class="search-page__price-separator">-</span>
            <el-input-number
              v-model="priceMaxInput"
              :min="0"
              placeholder="最高价"
              :controls="false"
              size="small"
              inputmode="numeric"
            />
          </div>
          <el-button
            type="primary"
            size="small"
            class="search-page__price-btn"
            @click="handlePriceApply"
          >
            确定
          </el-button>
        </div>

        <el-button
          v-if="hasActiveFilters"
          text
          type="primary"
          size="small"
          class="search-page__sidebar-reset"
          @click="handleResetFilters"
        >
          清除所有筛选
        </el-button>
      </aside>

      <!-- 右侧结果区 -->
      <section class="search-page__results" role="region" aria-label="搜索结果" aria-live="polite">
        <!-- 排序栏 -->
        <div class="search-page__sort-bar">
          <el-radio-group
            v-model="sort"
            size="small"
            class="search-page__sort-group"
            aria-label="排序方式"
          >
            <el-radio-button
              v-for="opt in SORT_OPTIONS"
              :key="opt.value"
              :value="opt.value"
            >
              {{ opt.label }}
            </el-radio-button>
          </el-radio-group>
          <span class="search-page__total-label">共 {{ total }} 件</span>
        </div>

        <!-- 加载骨架屏 -->
        <div v-if="loading && results.length === 0" class="search-page__skeleton-grid" aria-busy="true">
          <el-skeleton
            v-for="n in SKELETON_COUNT"
            :key="n"
            animated
            class="search-page__skeleton-card"
          >
            <template #template>
              <div class="search-page__skeleton-image" />
              <el-skeleton-item variant="text" style="width: 80%; margin-top: 12px" />
              <el-skeleton-item variant="text" style="width: 40%; margin-top: 8px" />
              <el-skeleton-item variant="text" style="width: 30%; margin-top: 8px" />
            </template>
          </el-skeleton>
        </div>

        <!-- 空结果 -->
        <div v-else-if="!loading && results.length === 0" class="search-page__empty">
          <el-empty description="未找到相关商品">
            <template #image>
              <el-icon :size="64" :color="colorBorder"><Search /></el-icon>
            </template>
            <el-button type="primary" @click="handleResetFilters">未找到相关商品，试试其他关键词</el-button>
          </el-empty>
        </div>

        <!-- 商品卡片网格 -->
        <div v-else class="search-page__grid">
          <router-link
            v-for="item in results"
            :key="item.spuId"
            :to="`/products/${item.spuId}`"
            class="search-page__card-link"
          >
            <el-card class="search-page__card" shadow="hover" :body-style="{ padding: '12px' }">
              <div class="search-page__card-image">
                <img
                  v-if="item.image"
                  :src="item.image"
                  :alt="item.spuName"
                  loading="lazy"
                />
                <div v-else class="search-page__card-placeholder" />
              </div>
              <div class="search-page__card-info">
                <h3 class="search-page__card-name" v-html="item.spuNameHighlight || item.spuName" />
                <div class="search-page__card-price">
                  <span class="search-page__card-price-symbol">¥</span>
                  <span class="search-page__card-price-value">{{ item.price }}</span>
                </div>
                <span class="search-page__card-sales">已售 {{ item.salesCount }}</span>
              </div>
            </el-card>
          </router-link>
        </div>

        <!-- 加载更多 -->
        <div v-if="results.length > 0" class="search-page__load-more">
          <div v-if="loading" class="search-page__loading-more">
            <span class="search-page__spinner" />
            <span>正在加载更多</span>
          </div>
          <el-button
            v-else-if="!noMore"
            plain
            size="large"
            @click="handleLoadMore"
          >
            加载更多
          </el-button>
          <p v-else class="search-page__no-more">已展示全部 {{ total }} 件商品</p>
        </div>
      </section>
    </div>

    <!-- 移动端筛选抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      title="筛选"
      direction="ltr"
      size="280px"
      class="search-page__drawer"
    >
      <div class="search-page__filter-section">
        <h3 class="search-page__filter-title">商品类目</h3>
        <ul class="search-page__filter-list" role="listbox" aria-label="商品类目筛选">
          <li
            v-for="cat in effectiveAggregations.categories"
            :key="cat.key"
            class="search-page__filter-item"
            :class="{ 'search-page__filter-item--active': selectedCategoryIds.includes(Number(cat.key)) }"
            :aria-selected="selectedCategoryIds.includes(Number(cat.key))"
            role="option"
            @click="handleCategorySelect(cat)"
          >
            <el-checkbox
              :model-value="selectedCategoryIds.includes(Number(cat.key))"
              size="small"
            />
            <span class="search-page__filter-label">{{ cat.name }}</span>
            <span class="search-page__filter-count">{{ cat.count }}</span>
          </li>
        </ul>
      </div>

      <div class="search-page__filter-section">
        <h3 class="search-page__filter-title">品牌</h3>
        <ul class="search-page__filter-list" role="listbox" aria-label="品牌筛选">
          <li
            v-for="brand in effectiveAggregations.brands"
            :key="brand.key"
            class="search-page__filter-item"
            :class="{ 'search-page__filter-item--active': selectedBrandIds.includes(Number(brand.key)) }"
            :aria-selected="selectedBrandIds.includes(Number(brand.key))"
            role="option"
            @click="handleBrandSelect(brand)"
          >
            <el-checkbox
              :model-value="selectedBrandIds.includes(Number(brand.key))"
              size="small"
            />
            <span class="search-page__filter-label">{{ brand.name }}</span>
            <span class="search-page__filter-count">{{ brand.count }}</span>
          </li>
        </ul>
      </div>

      <div class="search-page__filter-section">
        <h3 class="search-page__filter-title">价格区间</h3>
        <div class="search-page__price-range">
          <el-input-number
            v-model="priceMinInput"
            :min="0"
            placeholder="最低价"
            :controls="false"
            size="small"
            inputmode="numeric"
          />
          <span class="search-page__price-separator">-</span>
          <el-input-number
            v-model="priceMaxInput"
            :min="0"
            placeholder="最高价"
            :controls="false"
            size="small"
            inputmode="numeric"
          />
        </div>
        <el-button
          type="primary"
          size="small"
          class="search-page__price-btn"
          @click="handlePriceApply"
        >
          确定
        </el-button>
      </div>

      <el-button
        v-if="hasActiveFilters"
        text
        type="primary"
        size="small"
        class="search-page__sidebar-reset"
        @click="handleResetFilters"
      >
        清除所有筛选
      </el-button>
    </el-drawer>
  </div>
</template>

<style lang="scss" scoped>
@use '@/assets/styles/variables' as v;

.search-page {
  max-width: 1280px;
  margin: 0 auto;
  padding: v.$spacing-lg;

  // ========== 搜索栏 ==========
  &__header {
    margin-bottom: v.$spacing-lg;
  }

  &__input {
    max-width: 720px;
    margin: 0 auto;
  }

  // ========== 搜索建议 ==========
  &__suggestions {
    list-style: none;
    padding: v.$spacing-xs 0;
    margin: 0;
  }

  &__suggestion-item {
    display: flex;
    align-items: center;
    gap: v.$spacing-sm;
    padding: v.$spacing-sm v.$spacing-md;
    font-size: 14px;
    color: v.$color-text-primary;
    cursor: pointer;
    transition: background-color v.$duration-fast v.$ease-default;

    &:hover {
      background-color: v.$color-bg;
    }
  }

  &__suggestion-icon {
    color: v.$color-primary-light;
    font-size: 14px;
  }

  // ========== 移动端/平板端筛选栏 ==========
  &__filter-bar-mobile {
    display: none;
    align-items: center;
    gap: v.$spacing-md;
    margin-bottom: v.$spacing-md;
    padding: v.$spacing-sm 0;
  }

  &__filter-dot {
    display: inline-block;
    width: 6px;
    height: 6px;
    background: v.$color-cta;
    border-radius: 50%;
    margin-left: v.$spacing-xs;
  }

  &__total-hint {
    font-size: 13px;
    color: v.$color-text-regular;
  }

  // ========== 主体两栏 ==========
  &__body {
    display: flex;
    gap: v.$spacing-xl;
    align-items: flex-start;
  }

  // ========== PC 筛选侧栏 ==========
  &__sidebar {
    flex: 0 0 220px;
    position: sticky;
    top: 80px;
  }

  &__filter-section {
    margin-bottom: v.$spacing-lg;

    &:last-child {
      margin-bottom: 0;
    }
  }

  &__filter-title {
    font-family: v.$font-heading;
    font-size: 14px;
    font-weight: 600;
    color: v.$color-text-primary;
    margin: 0 0 v.$spacing-sm;
    padding-bottom: v.$spacing-sm;
    border-bottom: 1px solid v.$color-border;
  }

  &__filter-list {
    list-style: none;
    padding: 0;
    margin: 0;
  }

  &__filter-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: v.$spacing-sm v.$spacing-sm;
    font-size: 13px;
    color: v.$color-text-regular;
    cursor: pointer;
    border-radius: v.$radius-sm;
    transition: background-color v.$duration-fast v.$ease-default;

    &:hover {
      background-color: v.$color-bg;
    }

    &--active {
      color: v.$color-primary;
      background-color: v.$color-bg-card;
      font-weight: 600;
    }
  }

  &__filter-label {
    flex: 1;
    margin-left: v.$spacing-sm;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__filter-count {
    font-size: 11px;
    color: v.$color-text-secondary;
  }

  // ========== 价格区间 ==========
  &__price-range {
    display: flex;
    align-items: center;
    gap: v.$spacing-xs;
    margin-bottom: v.$spacing-sm;

    :deep(.el-input-number) {
      width: 85px;
    }
  }

  &__price-separator {
    font-size: 13px;
    color: v.$color-text-regular;
  }

  &__price-btn {
    width: 100%;
  }

  &__sidebar-reset {
    margin-top: v.$spacing-md;
  }

  // ========== 结果区 ==========
  &__results {
    flex: 1;
    min-width: 0;
  }

  // ========== 排序栏 ==========
  &__sort-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: v.$spacing-lg;
    padding-bottom: v.$spacing-md;
    border-bottom: 1px solid v.$color-border;
  }

  &__sort-group {
    :deep(.el-radio-button__inner) {
      // Element Plus 默认边框圆角需覆盖以匹配设计系统
      border-radius: v.$radius-md !important;
      border-color: v.$color-border;
      padding: 6px 16px;
      font-size: 13px;
    }
  }

  &__total-label {
    font-size: 13px;
    color: v.$color-text-regular;
  }

  // ========== 骨架屏 ==========
  &__skeleton-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: v.$spacing-md;
  }

  &__skeleton-card {
    padding: v.$spacing-md;
  }

  &__skeleton-image {
    width: 100%;
    aspect-ratio: 1;
    background: v.$color-bg-card;
    border-radius: v.$radius-md;
  }

  // ========== 商品网格 ==========
  &__grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: v.$spacing-md;
  }

  &__card-link {
    text-decoration: none;
    color: inherit;
    display: block;
    height: 100%;
  }

  &__card {
    cursor: pointer;
    height: 100%;

    :deep(.el-card__body) {
      padding: 12px;
    }
  }

  &__card:hover &__card-image img {
    transform: scale(1.02);
  }

  &__card-image {
    aspect-ratio: 1;
    overflow: hidden;
    border-radius: v.$radius-md;
    margin-bottom: v.$spacing-sm;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      transition: transform v.$duration-normal v.$ease-default;
    }
  }

  &__card-placeholder {
    width: 100%;
    height: 100%;
    background: v.$color-bg-card;
  }

  &__card-name {
    font-size: 18px;
    font-weight: 600;
    line-height: 1.4;
    margin-bottom: v.$spacing-sm;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  &__card-price {
    display: flex;
    align-items: baseline;
    margin-bottom: v.$spacing-xs;
  }

  &__card-price-symbol {
    font-size: 14px;
    font-weight: 700;
    color: v.$color-primary;
  }

  &__card-price-value {
    font-size: 20px;
    font-weight: 700;
    font-family: v.$font-heading;
    color: v.$color-primary;
  }

  &__card-sales {
    font-size: 12px;
    color: v.$color-text-regular;
  }

  // ========== 空结果 ==========
  &__empty {
    display: flex;
    justify-content: center;
    padding: v.$spacing-3xl 0;
  }

  // ========== 加载更多 ==========
  &__load-more {
    display: flex;
    justify-content: center;
    padding: v.$spacing-xl 0 v.$spacing-2xl;
  }

  &__loading-more {
    display: flex;
    align-items: center;
    gap: v.$spacing-sm;
    font-size: 14px;
    color: v.$color-text-regular;
  }

  &__spinner {
    display: inline-block;
    width: 18px;
    height: 18px;
    border: 2px solid v.$color-border;
    border-top-color: v.$color-primary;
    border-radius: 50%;
    animation: spin 0.6s linear infinite;
  }

  &__no-more {
    font-size: 13px;
    color: v.$color-text-regular;
    margin: 0;
  }

  // ========== 响应式 ==========
  @media (max-width: 1024px) {
    &__sidebar {
      display: none;
    }

    &__filter-bar-mobile {
      display: flex;
    }

    &__grid {
      grid-template-columns: repeat(2, 1fr);
    }

    &__skeleton-grid {
      grid-template-columns: repeat(2, 1fr);
    }
  }

  @media (max-width: 768px) {
    padding: v.$spacing-md;

    &__header {
      margin-bottom: v.$spacing-md;
    }

    &__grid {
      gap: v.$spacing-sm;
    }

    &__skeleton-grid {
      gap: v.$spacing-sm;
    }

    &__sort-bar {
      flex-direction: column;
      gap: v.$spacing-sm;
      align-items: flex-start;
    }

    &__total-label {
      display: none;
    }
  }
}

// ========== 全局 popover 样式 ==========
:global(.search-page__suggestions-popover) {
  // Element Plus popover 默认 padding 需覆盖以匹配设计规范
  padding: 0 !important;
  border-radius: v.$radius-md;
  box-shadow: v.$shadow-md;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
