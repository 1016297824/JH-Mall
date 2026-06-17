<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getSpuDetail, getSkuDetail } from '@/api/product'
import { postCartItem } from '@/api/order'
import type { SpuDetailVO, SkuBriefVO, SkuVO, SpecGroupVO } from '@/types'
import ProductGallery from './components/ProductGallery.vue'
import ProductInfoSection from './components/ProductInfoSection.vue'
import ProductTabs from './components/ProductTabs.vue'

const route = useRoute()
const router = useRouter()

// ==================== 状态 ====================
const loading = ref(true)
const error = ref('')
const spu = ref<SpuDetailVO | null>(null)
const currentImageIdx = ref(0)
const selectedSpecs = ref<Record<string, string>>({})
const quantity = ref(1)
const activeTab = ref<'specs' | 'reviews' | 'afterSale'>('specs')
/** UI 规格分组（从 SKU attrs 派生） */
const specGroups = ref<SpecGroupVO[]>([])
/** 当前选中 SKU 的完整详情（含库存、划线价） */
const currentSkuDetail = ref<SkuVO | null>(null)
/** SKU 详情加载中（防闪烁） */
const skuLoading = ref(false)

// ==================== 规格派生 ====================

/**
 * 从 SKU attrs 列表推导规格分组。
 * 按 k（属性名）去重分组，收集每组的唯一 v（属性值）。
 */
function deriveSpecGroups(skus: { attrs: { k: string; v: string }[] }[]): SpecGroupVO[] {
  if (skus.length === 0) return []
  // 取第一个有 attrs 的 SKU 确定规格维度及顺序
  const firstWithAttrs = skus.find((s) => s.attrs.length > 0)
  if (!firstWithAttrs) return []
  const groupKeys = firstWithAttrs.attrs.map((a) => a.k)
  const groups: SpecGroupVO[] = []
  for (const key of groupKeys) {
    const uniqueValues = [...new Set(skus.flatMap((s) => s.attrs.filter((a) => a.k === key).map((a) => a.v)))]
    groups.push({
      specName: key,
      values: uniqueValues.map((v) => ({ value: v, disabled: false })),
    })
  }
  return groups
}

// ==================== 计算属性 ====================

const hasSelection = computed(() => Object.keys(selectedSpecs.value).length > 0)

const isSpecComplete = computed(
  () =>
    specGroups.value.length > 0 && specGroups.value.every((g) => selectedSpecs.value[g.specName]),
)

/** 按 attrs 匹配 SKU */
const currentSku = computed<SkuBriefVO | null>(() => {
  if (!isSpecComplete.value || !spu.value) return null
  const selEntries = Object.entries(selectedSpecs.value)
  return (
    spu.value.skus.find((s) => {
      if (s.attrs.length !== selEntries.length) return false
      return selEntries.every(([k, v]) => s.attrs.some((a) => a.k === k && a.v === v))
    }) ?? null
  )
})

const currentPrice = computed(() => {
  if (!spu.value) return 0
  if (currentSkuDetail.value) return currentSkuDetail.value.price
  if (currentSku.value) return currentSku.value.price
  return spu.value.priceMin
})

const currentOriginalPrice = computed(() => {
  if (currentSkuDetail.value?.marketPrice) return currentSkuDetail.value.marketPrice
  return undefined
})

const stock = computed(() => currentSkuDetail.value?.availableStock ?? 0)

const maxQuantity = computed(() => Math.min(stock.value, 99))

const canBuy = computed(() => isSpecComplete.value && stock.value > 0)

/**
 * 根据已选规格计算每个选项是否可选。
 *
 * <p>核心逻辑：对每个候选值，查找是否存在 SKU 同时满足：
 * ① 候选属性匹配；② 其他已选规格全部匹配（排除本组自身）。</p>
 *
 * <p>这样已选组内部的选项仍可切换，但只限于有实际 SKU 的组合。</p>
 */
const enrichedSpecGroups = computed<SpecGroupVO[]>(() => {
  if (!specGroups.value.length || !spu.value) return []
  return specGroups.value.map((group) => {
    return {
      ...group,
      values: group.values.map((val) => {
        const hasMatch = spu.value!.skus.some((sku) => {
          // ① 当前候选值必须在该 SKU 的 attrs 中
          if (!sku.attrs.some((a) => a.k === group.specName && a.v === val.value)) return false
          // ② 其他已选规格必须全部匹配（排除本组自身）
          return Object.entries(selectedSpecs.value)
            .filter(([k]) => k !== group.specName)
            .every(([k, v]) => sku.attrs.some((a) => a.k === k && a.v === v))
        })
        return { ...val, disabled: !hasMatch }
      }),
    }
  })
})

// ==================== 方法 ====================

function onSpecSelect(specName: string, value: string) {
  const next = { ...selectedSpecs.value }
  if (next[specName] === value) {
    delete next[specName]
  } else {
    next[specName] = value
  }
  selectedSpecs.value = next
  quantity.value = 1
}

function onQuantityChange(v: number) {
  quantity.value = v
}

async function addToCart() {
  if (!canBuy.value || !currentSku.value) return
  try {
    await postCartItem({ skuId: currentSku.value.skuId, quantity: quantity.value })
    ElMessage.success('已加入购物车')
  } catch {
    ElMessage.error('加入购物车失败')
  }
}

function buyNow() {
  if (!canBuy.value || !currentSku.value) return
  const token = localStorage.getItem('accessToken')
  if (!token) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    ElMessage.warning('请先登录')
    return
  }
  router.push({
    path: '/checkout',
    query: { skuId: currentSku.value.skuId, qty: quantity.value.toString() },
  })
}

async function loadDetail() {
  loading.value = true
  error.value = ''
  try {
    const spuId = route.params.id as string
    spu.value = await getSpuDetail(spuId)
    specGroups.value = deriveSpecGroups(spu.value.skus)
    // images 为空或全是占位 URL 时，用 SKU 的 image 兜底（最多 3 张）
    const hasValidImages = spu.value.images?.some((img) => !img.includes('cdn.example.com'))
    if (!hasValidImages) {
      spu.value.images = spu.value.skus
        .filter((s) => s.image && !s.image.includes('cdn.example.com'))
        .slice(0, 3)
        .map((s) => s.image)
    }
    currentImageIdx.value = 0
    selectedSpecs.value = {}
    quantity.value = 1
    currentSkuDetail.value = null
  } catch {
    error.value = '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

watch(currentSku, async (sku) => {
  if (!sku) {
    currentSkuDetail.value = null
    skuLoading.value = false
    return
  }
  skuLoading.value = true
  try {
    currentSkuDetail.value = await getSkuDetail(sku.skuId)
  } catch {
    currentSkuDetail.value = null
  } finally {
    skuLoading.value = false
  }
})

onMounted(loadDetail)
</script>

<template>
  <a href="#main-content" class="skip-link">跳到主要内容</a>
  <div class="product-detail-page">
    <!-- 加载态骨架屏 -->
    <div v-if="loading" class="skeleton-container">
      <div class="skeleton-left">
        <div class="skeleton-box skeleton-main-img" />
        <div class="skeleton-thumbs">
          <div v-for="n in 4" :key="n" class="skeleton-box skeleton-thumb" />
        </div>
      </div>
      <div class="skeleton-right">
        <div class="skeleton-box skeleton-title" />
        <div class="skeleton-box skeleton-price" />
        <div class="skeleton-box skeleton-stats" />
        <div class="skeleton-box skeleton-specs" />
        <div class="skeleton-box skeleton-btn" />
      </div>
    </div>

    <!-- 错误态 -->
    <div v-else-if="error" class="error-state">
      <div class="error-icon">!</div>
      <p class="error-msg">{{ error }}</p>
      <button class="retry-btn" @click="loadDetail">重新加载</button>
    </div>

    <!-- 主内容 -->
    <div v-else-if="spu" id="main-content" class="detail-container" tabindex="-1">
      <ProductGallery
        :images="spu.images"
        :fallback="spu.mainImage"
        :alt="spu.spuName"
        :active-idx="currentImageIdx"
        :sold-out="!canBuy && isSpecComplete && stock === 0"
        @select="currentImageIdx = $event"
      />
      <ProductInfoSection
        :spu="spu"
        :spec-groups="specGroups"
        :enriched-spec-groups="enrichedSpecGroups"
        :selected-specs="selectedSpecs"
        :quantity="quantity"
        :max-quantity="maxQuantity"
        :current-price="currentPrice"
        :current-original-price="currentOriginalPrice"
        :stock="stock"
        :is-spec-complete="isSpecComplete"
        :has-selection="hasSelection"
        :can-buy="canBuy"
        :sku-loading="skuLoading"
        @select="onSpecSelect"
        @update:quantity="onQuantityChange"
        @add-to-cart="addToCart"
        @buy-now="buyNow"
      />
      <ProductTabs
        v-model="activeTab"
        :spec-groups="specGroups"
        :brand-name="spu.brandName ?? ''"
        :sku-code="currentSku?.skuCode ?? ''"
      />
    </div>
  </div>
</template>

<style lang="scss" scoped>
@use '@/assets/styles/variables' as v;

// ==================== 页面容器 ====================
.skip-link {
  position: absolute;
  top: -100px;
  left: 16px;
  padding: 8px 16px;
  background: v.$color-primary;
  color: #fff;
  border-radius: v.$radius-md;
  font-size: 14px;
  font-family: v.$font-body;
  text-decoration: none;
  z-index: 1000;

  &:focus {
    top: 16px;
  }
}

.product-detail-page {
  min-height: 100vh;
  background: v.$color-bg;
}

.detail-container {
  max-width: 1280px;
  margin: 0 auto;
  padding: v.$spacing-lg;
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-template-areas:
    'gallery info'
    'tabs   tabs';
  gap: v.$spacing-2xl v.$spacing-xl;
}

// ==================== 骨架屏 ====================
.skeleton-container {
  max-width: 1280px;
  margin: 0 auto;
  padding: v.$spacing-lg;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: v.$spacing-2xl v.$spacing-xl;
}

.skeleton-box {
  background: linear-gradient(90deg, v.$color-bg-card 25%, v.$color-border 50%, v.$color-bg-card 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: v.$radius-md;
}

@keyframes shimmer {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

.skeleton-main-img {
  width: 100%;
  aspect-ratio: 1;
  border-radius: v.$radius-lg;
  margin-bottom: v.$spacing-md;
}
.skeleton-thumbs {
  display: flex;
  gap: v.$spacing-sm;
}
.skeleton-thumb {
  width: 64px;
  height: 64px;
  border-radius: v.$radius-sm;
}
.skeleton-title {
  height: 28px;
  width: 80%;
  margin-bottom: v.$spacing-md;
}
.skeleton-price {
  height: 40px;
  width: 40%;
  margin-bottom: v.$spacing-md;
}
.skeleton-stats {
  height: 20px;
  width: 30%;
  margin-bottom: v.$spacing-xl;
}
.skeleton-specs {
  height: 80px;
  width: 100%;
  margin-bottom: v.$spacing-xl;
}
.skeleton-btn {
  height: 48px;
  width: 60%;
}

// ==================== 错误态 ====================
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px v.$spacing-lg;
  gap: v.$spacing-md;
}

.error-icon {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: v.$color-border;
  color: v.$color-text-secondary;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 700;
  font-family: v.$font-heading;
}

.error-msg {
  font-size: 15px;
  color: v.$color-text-regular;
  margin: 0;
}

.retry-btn {
  padding: 8px 24px;
  border: 1px solid v.$color-primary;
  border-radius: v.$radius-md;
  background: #fff;
  color: v.$color-primary;
  font-size: 14px;
  font-family: v.$font-body;
  cursor: pointer;
  transition: all v.$duration-normal v.$ease-default;

  &:hover {
    background: v.$color-primary;
    color: #fff;
  }
}

// ==================== 响应式 ====================
@media (max-width: 767px) {
  .detail-container {
    grid-template-columns: 1fr;
    grid-template-areas: 'gallery' 'info' 'tabs';
    gap: v.$spacing-lg;
    padding: v.$spacing-md;
  }
}

// ==================== 可访问性 ====================
@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    // 无障碍必需：尊重用户系统动画偏好，禁用所有动效
    animation: none !important;
    transition: none !important;
  }
}

:focus-visible {
  outline: 2px solid #0369a1;
  outline-offset: 2px;
}
</style>
