<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { SkuSelector } from '@/components/business/product'
import { getSpuDetail, getSkuDetail } from '@/api/product'
import { postCartItem } from '@/api/order'
import { formatPrice } from '@/utils/common/format'
import type { SpuDetailVO, SkuBriefVO, SkuVO, SpecGroupVO } from '@/types'

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
/** UI 规格分组（从 SKU 名称派生） */
const specGroups = ref<SpecGroupVO[]>([])
/** 当前选中 SKU 的完整详情（含库存、划线价） */
const currentSkuDetail = ref<SkuVO | null>(null)
/** SKU 详情加载中（防闪烁） */
const skuLoading = ref(false)

const tabs = [
  { key: 'specs' as const, label: '商品参数' },
  { key: 'reviews' as const, label: '用户评价' },
  { key: 'afterSale' as const, label: '售后说明' },
]

// ==================== 规格派生 ====================

/**
 * 从 SKU 名称列表推导规格分组。
 * 假设所有 SKU 名称用空格分隔规格值，且维度顺序一致。
 *
 * 示例：["金色 128GB", "金色 256GB", "银色 128GB"]
 *   → [{ specName: "规格一", values: ["金色","银色"] }, { specName: "规格二", values: ["128GB","256GB"] }]
 */
function deriveSpecGroups(skus: { skuName: string }[]): SpecGroupVO[] {
  if (skus.length === 0) return []
  const firstSku = skus[0]!
  const firstParts = firstSku.skuName.split(/\s+/)
  const dimCount = firstParts.length
  if (dimCount <= 1) return []
  const groups: SpecGroupVO[] = []
  for (let i = 0; i < dimCount; i++) {
    const uniqueValues = [...new Set(skus.map((s) => s.skuName.split(/\s+/)[i]!))]
    groups.push({
      specName: `规格${i + 1}`,
      values: uniqueValues.map((v) => ({ value: v, disabled: false })),
    })
  }
  return groups
}

// ==================== 计算属性 ====================

/** 是否已选中任意规格 */
const hasSelection = computed(() => Object.keys(selectedSpecs.value).length > 0)

/** 是否已选完所有规格分组 */
const isSpecComplete = computed(
  () =>
    specGroups.value.length > 0 && specGroups.value.every((g) => selectedSpecs.value[g.specName]),
)

/** 按名称片段匹配 SKU */
const currentSku = computed<SkuBriefVO | null>(() => {
  if (!isSpecComplete.value || !spu.value) return null
  const selValues = Object.values(selectedSpecs.value)
  return (
    spu.value.skus.find((s) => {
      const parts = s.skuName.split(/\s+/)
      return selValues.every((sv, idx) => parts[idx] === sv)
    }) ?? null
  )
})

/** 当前售价（分） */
const currentPrice = computed(() => {
  if (!spu.value) return 0
  if (currentSkuDetail.value) return currentSkuDetail.value.price
  if (currentSku.value) return currentSku.value.price
  return spu.value.priceMin
})

/** 当前划线价（分） */
const currentOriginalPrice = computed(() => {
  if (currentSkuDetail.value?.marketPrice) return currentSkuDetail.value.marketPrice
  return undefined
})

/** 当前库存 */
const stock = computed(() => currentSkuDetail.value?.availableStock ?? 0)

/** 最大可购买数量 */
const maxQuantity = computed(() => Math.min(stock.value, 99))

/** 是否可以购买 */
const canBuy = computed(() => isSpecComplete.value && stock.value > 0)

/** 当前展示的图片 */
const currentImage = computed(() => {
  if (!spu.value) return ''
  return spu.value.images?.[currentImageIdx.value] ?? spu.value.mainImage
})

/** 规格值拼接 */
function joinSpecs(values: { value: string }[]): string {
  return values.map((v) => v.value).join('、')
}

/** 缩略图列表（带索引，避免 v-for 使用数组索引作 key） */
const thumbnailList = computed(() => spu.value?.images?.map((img, idx) => ({ img, idx })) ?? [])

/**
 * 根据已选规格计算每个选项是否可选。
 * 由于 SkuBriefVO 不含库存，仅基于"该组合是否存在"来判断可选性。
 */
const enrichedSpecGroups = computed<SpecGroupVO[]>(() => {
  if (!specGroups.value.length || !spu.value) return []
  return specGroups.value.map((group, groupIdx) => {
    const isCurrentGroup = selectedSpecs.value[group.specName] !== undefined
    return {
      ...group,
      values: group.values.map((val) => {
        if (isCurrentGroup) return val
        const hasMatch = spu.value!.skus.some((sku) => {
          const parts = sku.skuName.split(/\s+/)
          if (parts[groupIdx] !== val.value) return false
          return Object.entries(selectedSpecs.value).every(([name, sel]) => {
            const idx = specGroups.value.findIndex((g) => g.specName === name)
            return parts[idx] === sel
          })
        })
        return { ...val, disabled: !hasMatch }
      }),
    }
  })
})

// ==================== 方法 ====================

/** 点击规格值 */
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

/** 数量输入 */
function onQuantityInput(e: Event) {
  const input = e.target as HTMLInputElement
  let v = parseInt(input.value, 10)
  if (isNaN(v) || v < 1) v = 1
  if (v > maxQuantity.value) v = maxQuantity.value
  quantity.value = v
}

/** 加入购物车 */
async function addToCart() {
  if (!canBuy.value || !currentSku.value) return
  try {
    await postCartItem({ skuId: currentSku.value.skuId, quantity: quantity.value })
    ElMessage.success('已加入购物车')
  } catch {
    ElMessage.error('加入购物车失败')
  }
}

/** 立即购买 */
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

/** 加载详情 */
async function loadDetail() {
  loading.value = true
  error.value = ''
  try {
    const spuId = route.params.id as string
    spu.value = await getSpuDetail(spuId)
    specGroups.value = deriveSpecGroups(spu.value.skus)
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

/** SKU 变更时拉取详情（库存、划线价） */
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
    <div v-else-if="spu" class="detail-container">
      <!-- 左栏：商品图片 -->
      <section class="gallery-section">
        <div class="main-image-wrapper">
          <img :src="currentImage" :alt="spu.spuName" class="main-image" loading="eager" />
          <span v-if="stock === 0" class="sold-out-overlay">已售罄</span>
        </div>
        <div v-if="spu.images?.length > 1" class="thumbnail-list">
          <button
            v-for="item in thumbnailList"
            :key="item.img"
            class="thumb-btn"
            :class="{ 'thumb-btn--active': currentImageIdx === item.idx }"
            :aria-label="'切换到第' + (item.idx + 1) + '张图'"
            @click="currentImageIdx = item.idx"
          >
            <img :src="item.img" :alt="'缩略图 ' + (item.idx + 1)" class="thumb-img" />
          </button>
        </div>
      </section>

      <!-- 右栏：商品信息 -->
      <section class="info-section">
        <!-- 商品名 + 摘要 -->
        <div class="info-header">
          <h1 class="product-name">{{ spu.spuName }}</h1>
          <p v-if="spu.description" class="product-desc">{{ spu.description }}</p>
        </div>

        <!-- 价格区 -->
        <div class="price-box">
          <div class="price-row">
            <span class="price-currency">¥</span>
            <span class="price-current">{{ formatPrice(currentPrice) }}</span>
            <span v-if="currentOriginalPrice" class="price-original">
              ¥{{ formatPrice(currentOriginalPrice) }}
            </span>
            <span v-if="!isSpecComplete && hasSelection" class="price-pending">
              选择规格后显示价格
            </span>
          </div>
          <div class="price-extra">
            <span class="sales-info">已售 {{ spu.salesCount ?? 0 }} 件</span>
            <span class="divider">|</span>
            <span class="review-info">{{ spu.reviewCount ?? 0 }} 条评价</span>
          </div>
        </div>

        <!-- SKU 选择器 -->
        <div class="sku-section">
          <SkuSelector
            v-if="specGroups.length"
            :spec-groups="enrichedSpecGroups"
            :skus="spu.skus"
            :selected-specs="selectedSpecs"
            @select="onSpecSelect"
          />
        </div>

        <!-- 数量选择器 -->
        <div class="quantity-section">
          <label for="qty-input" class="qty-label">数量</label>
          <div class="quantity-control">
            <button
              class="qty-btn"
              :disabled="quantity <= 1"
              @click="quantity = Math.max(1, quantity - 1)"
            >
              -
            </button>
            <input
              id="qty-input"
              class="qty-input"
              type="number"
              :value="quantity"
              :min="1"
              :max="maxQuantity"
              @input="onQuantityInput"
            />
            <button
              class="qty-btn"
              :disabled="quantity >= maxQuantity"
              @click="quantity = Math.min(maxQuantity, quantity + 1)"
            >
              +
            </button>
          </div>
          <span v-if="isSpecComplete" class="stock-info" aria-live="polite">
            <template v-if="skuLoading">查询库存中...</template>
            <template v-else>库存 {{ stock }} 件</template>
          </span>
        </div>

        <!-- 操作按钮 -->
        <div class="action-row">
          <button class="btn-cart" :disabled="!canBuy" @click="addToCart">加入购物车</button>
          <button class="btn-buy" :disabled="!canBuy" @click="buyNow">立即购买</button>
        </div>

        <!-- 服务承诺 -->
        <div class="service-promises">
          <span class="promise-item"><span aria-hidden="true" class="promise-check">✓ </span>7天无理由退换</span>
          <span class="promise-item"><span aria-hidden="true" class="promise-check">✓ </span>正品保证</span>
          <span class="promise-item"><span aria-hidden="true" class="promise-check">✓ </span>满99包邮</span>
        </div>
      </section>

      <!-- 底部 Tabs -->
      <section class="detail-tabs-section">
        <div class="tab-header">
          <button
            v-for="tab in tabs"
            :key="tab.key"
            class="tab-btn"
            :class="{ 'tab-btn--active': activeTab === tab.key }"
            @click="activeTab = tab.key"
          >
            {{ tab.label }}
          </button>
        </div>
        <div class="tab-content">
          <!-- 参数 Tab -->
          <div v-if="activeTab === 'specs'" class="tab-panel">
            <dl class="spec-list">
              <template v-for="group in specGroups" :key="group.specName">
                <dt>{{ group.specName }}</dt>
                <dd>{{ joinSpecs(group.values) }}</dd>
              </template>
              <dt>品牌</dt>
              <dd>{{ spu.brandId || 'JH-Mall' }}</dd>
              <dt>货号</dt>
              <dd>{{ spu.spuId }}</dd>
            </dl>
          </div>

          <!-- 评价 Tab -->
          <div v-else-if="activeTab === 'reviews'" class="tab-panel">
            <div class="empty-tab">
              <p>暂无评价，成为第一个评价的人吧</p>
            </div>
          </div>

          <!-- 售后说明 Tab -->
          <div v-else-if="activeTab === 'afterSale'" class="tab-panel">
            <ul class="after-sale-list">
              <li>支持 7 天无理由退货（商品完好、不影响二次销售）</li>
              <li>签收 15 天内如有质量问题可申请换货</li>
              <li>未发货订单可随时取消，已发货订单需签收后退货</li>
              <li>退款将在收到退货商品后 3 个工作日内原路返还</li>
            </ul>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<style lang="scss" scoped>
@use '@/assets/styles/variables' as v;

// ==================== 页面容器 ====================
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
  background: linear-gradient(90deg, #e2eaf0 25%, #d0dce5 50%, #e2eaf0 75%);
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

// ==================== 图片区 ====================
.gallery-section {
  grid-area: gallery;
  position: sticky;
  top: v.$spacing-lg;
  align-self: start;
}

.main-image-wrapper {
  position: relative;
  width: 100%;
  aspect-ratio: 1;
  border-radius: v.$radius-lg;
  overflow: hidden;
  background: #fff;
  box-shadow: v.$shadow-md;
}

.main-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: opacity v.$duration-slow v.$ease-default;
}

.sold-out-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.75);
  font-size: 20px;
  font-weight: 700;
  color: v.$color-text-primary;
  font-family: v.$font-heading;
  letter-spacing: 0.05em;
}

.thumbnail-list {
  display: flex;
  gap: v.$spacing-sm;
  margin-top: v.$spacing-md;
  justify-content: center;
}

.thumb-btn {
  width: 64px;
  height: 64px;
  padding: 2px;
  border: 2px solid transparent;
  border-radius: v.$radius-sm;
  background: #fff;
  cursor: pointer;
  transition: border-color v.$duration-normal v.$ease-default;
  overflow: hidden;

  &:hover {
    border-color: v.$color-primary-light;
  }

  &--active {
    border-color: v.$color-primary;
  }
}

.thumb-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 2px;
}

// ==================== 信息区 ====================
.info-section {
  grid-area: info;
  display: flex;
  flex-direction: column;
  gap: v.$spacing-lg;
}

.info-header {
  margin-bottom: 0;
}

.product-name {
  font-family: v.$font-heading;
  font-size: 28px;
  font-weight: 700;
  color: v.$color-text-primary;
  line-height: 1.2;
  margin: 0 0 v.$spacing-sm;
}

.product-desc {
  font-size: 14px;
  color: v.$color-text-secondary;
  line-height: 1.6;
  margin: 0;
}

// 价格区
.price-box {
  background: linear-gradient(135deg, #f8fbfe 0%, #eff6fb 100%);
  border-radius: v.$radius-lg;
  padding: v.$spacing-lg;
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: v.$spacing-xs;
  margin-bottom: v.$spacing-sm;
}

.price-currency {
  font-family: v.$font-heading;
  font-size: 18px;
  font-weight: 700;
  color: v.$color-primary;
}

.price-current {
  font-family: v.$font-heading;
  font-size: 20px;
  font-weight: 700;
  color: v.$color-primary;
  line-height: 1;
}

.price-original {
  font-size: 14px;
  color: #7a94a6;
  text-decoration: line-through;
  margin-left: v.$spacing-sm;
}

.price-pending {
  font-size: 15px;
  color: v.$color-text-secondary;
}

.price-extra {
  display: flex;
  align-items: center;
  gap: v.$spacing-sm;
  font-size: 13px;
  color: v.$color-text-secondary;
}

.divider {
  color: v.$color-border;
}

// SKU 区
.sku-section {
  padding: v.$spacing-md 0;
}

// 数量选择器
.quantity-section {
  display: flex;
  align-items: center;
  gap: v.$spacing-md;
}

.qty-label {
  font-size: 13px;
  font-weight: 600;
  color: v.$color-text-regular;
  font-family: v.$font-body;
  min-width: 32px;
}

.quantity-control {
  display: flex;
  align-items: center;
  border: 1px solid v.$color-border;
  border-radius: v.$radius-md;
  overflow: hidden;
}

.qty-btn {
  width: 36px;
  height: 36px;
  border: none;
  background: #f5f8fa;
  font-size: 18px;
  color: v.$color-text-regular;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition:
    background-color v.$duration-fast v.$ease-default,
    transform v.$duration-fast v.$ease-default;

  &:hover:not(:disabled) {
    background: v.$color-bg-card;
  }

  &:active:not(:disabled) {
    transform: scale(0.97);
  }

  &:disabled {
    color: v.$color-border;
    cursor: not-allowed;
  }
}

.qty-input {
  width: 52px;
  height: 36px;
  border: none;
  border-left: 1px solid v.$color-border;
  border-right: 1px solid v.$color-border;
  text-align: center;
  font-size: 14px;
  font-family: v.$font-body;
  color: v.$color-text-primary;
  background: #fff;
  outline: none;

  &::-webkit-inner-spin-button,
  &::-webkit-outer-spin-button {
    display: none;
  }
}

.stock-info {
  font-size: 12px;
  color: v.$color-text-secondary;
}

// 操作按钮
.action-row {
  display: flex;
  gap: v.$spacing-md;
  padding-top: v.$spacing-sm;
}

.btn-cart,
.btn-buy {
  flex: 1;
  height: 48px;
  border: none;
  border-radius: v.$radius-md;
  font-size: 16px;
  font-weight: 600;
  font-family: v.$font-body;
  cursor: pointer;
  transition:
    background-color v.$duration-normal v.$ease-default,
    transform v.$duration-fast v.$ease-default,
    box-shadow v.$duration-normal v.$ease-default;

  &:hover:not(:disabled) {
    transform: translateY(-1px);
  }

  &:active:not(:disabled) {
    transform: scale(0.97);
  }

  &:disabled {
    opacity: 0.45;
    cursor: not-allowed;
  }
}

.btn-cart {
  background: #22c55e;
  color: #fff;
  border: none;

  &:hover:not(:disabled) {
    background: #16a34a;
    box-shadow: v.$shadow-md;
  }
}

.btn-buy {
  background: v.$color-primary;
  color: #fff;

  &:hover:not(:disabled) {
    background: v.$color-primary-light;
    box-shadow: v.$shadow-md;
  }
}

// 服务承诺
.service-promises {
  display: flex;
  gap: v.$spacing-lg;
  padding-top: v.$spacing-sm;
}

.promise-item {
  font-size: 12px;
  color: v.$color-text-secondary;
}

.promise-check {
  color: #22C55E;
  font-weight: 700;
}

// ==================== 详情 Tabs ====================
.detail-tabs-section {
  grid-area: tabs;
  background: #fff;
  border-radius: v.$radius-lg;
  box-shadow: v.$shadow-sm;
  overflow: hidden;
}

.tab-header {
  display: flex;
  border-bottom: 1px solid v.$color-border;
}

.tab-btn {
  flex: 1;
  padding: v.$spacing-md v.$spacing-lg;
  border: none;
  background: transparent;
  font-size: 15px;
  font-weight: 500;
  font-family: v.$font-body;
  color: v.$color-text-secondary;
  cursor: pointer;
  position: relative;
  transition: color v.$duration-normal v.$ease-default;

  &::after {
    content: '';
    position: absolute;
    bottom: 0;
    left: 50%;
    transform: translateX(-50%) scaleX(0);
    width: 40px;
    height: 3px;
    background: v.$color-primary;
    border-radius: 2px;
    transition: transform v.$duration-normal v.$ease-default;
  }

  &:hover {
    color: v.$color-text-primary;
  }

  &--active {
    color: v.$color-primary;
    font-weight: 600;

    &::after {
      transform: translateX(-50%) scaleX(1);
    }
  }
}

.tab-content {
  padding: v.$spacing-xl;
  min-height: 200px;
}

.tab-panel {
  animation: fadeIn v.$duration-slow v.$ease-default;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

// 参数表格
.spec-list {
  display: grid;
  grid-template-columns: 100px 1fr;
  gap: 0;

  dt,
  dd {
    padding: v.$spacing-sm v.$spacing-md;
    border-bottom: 1px solid v.$color-bg;
    margin: 0;
  }

  dt {
    font-size: 13px;
    color: v.$color-text-secondary;
  }

  dd {
    font-size: 13px;
    color: v.$color-text-primary;
  }
}

// 售后说明
.after-sale-list {
  list-style: none;
  padding: 0;
  margin: 0;

  li {
    position: relative;
    padding: v.$spacing-sm 0 v.$spacing-sm v.$spacing-lg;
    font-size: 14px;
    color: v.$color-text-regular;
    line-height: 1.7;

    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 13px;
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background: v.$color-primary-light;
    }
  }
}

.empty-tab {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 160px;
  color: v.$color-text-secondary;
  font-size: 14px;
}

// ==================== 响应式：移动端 ====================
@media (max-width: 767px) {
  .detail-container {
    grid-template-columns: 1fr;
    grid-template-areas:
      'gallery'
      'info'
      'tabs';
    gap: v.$spacing-lg;
    padding: v.$spacing-md;
  }

  .gallery-section {
    position: static;
  }

  .main-image-wrapper {
    border-radius: 0;
    margin: -12px -12px 0 -12px;
    width: calc(100% + 24px);
    aspect-ratio: 1;
    box-shadow: none;
  }

  .thumbnail-list {
    margin-top: v.$spacing-sm;
    overflow-x: auto;
    justify-content: flex-start;
    padding-bottom: v.$spacing-xs;
  }

  .product-name {
    font-size: 24px;
  }

  .price-current {
    font-size: 20px;
  }

  .action-row {
    flex-direction: column;
  }

  .tab-content {
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
