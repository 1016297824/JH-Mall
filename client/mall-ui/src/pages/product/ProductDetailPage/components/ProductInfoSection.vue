<template>
  <section class="info-section">
    <div class="info-header">
      <h1 class="product-name">{{ spu.spuName }}</h1>
      <p v-if="spu.description" class="product-desc">{{ spu.description }}</p>
    </div>

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

    <div class="sku-section" v-if="specGroups.length">
      <SkuSelector
        :spec-groups="enrichedSpecGroups"
        :skus="spu.skus"
        :selected-specs="selectedSpecs"
        @select="onSelect"
      />
    </div>

    <div class="quantity-section">
      <label for="qty-input" class="qty-label">数量</label>
      <div class="quantity-control">
        <button class="qty-btn" :disabled="quantity <= 1" @click="setQuantity(Math.max(1, quantity - 1))">-</button>
        <input
          id="qty-input" class="qty-input" type="number" inputmode="numeric"
          :value="quantity" :min="1" :max="maxQuantity"
          @input="onQuantityInput"
        />
        <button class="qty-btn" :disabled="quantity >= maxQuantity" @click="setQuantity(Math.min(maxQuantity, quantity + 1))">+</button>
      </div>
      <span v-if="isSpecComplete" class="stock-info" aria-live="polite">
        <template v-if="skuLoading">查询库存中...</template>
        <template v-else>库存 {{ stock }} 件</template>
      </span>
    </div>

    <div class="action-row">
      <button class="action__btn--cart" :disabled="!canBuy" @click="emit('addToCart')">加入购物车</button>
      <button class="action__btn--buy" :disabled="!canBuy" @click="emit('buyNow')">立即购买</button>
    </div>

    <div class="service-promises">
      <span class="promise-item"><span aria-hidden="true" class="promise-check">✓ </span>7天无理由退换</span>
      <span class="promise-item"><span aria-hidden="true" class="promise-check">✓ </span>正品保证</span>
      <span class="promise-item"><span aria-hidden="true" class="promise-check">✓ </span>满99包邮</span>
    </div>
  </section>
</template>

<script setup lang="ts">
import { SkuSelector } from '@/components/business/product'
import { formatPrice } from '@/utils/common/format'
import type { SpuDetailVO, SpecGroupVO } from '@/types'

const props = defineProps<{
  spu: SpuDetailVO
  specGroups: SpecGroupVO[]
  enrichedSpecGroups: SpecGroupVO[]
  selectedSpecs: Record<string, string>
  quantity: number
  maxQuantity: number
  currentPrice: number
  currentOriginalPrice: number | undefined
  stock: number
  isSpecComplete: boolean
  hasSelection: boolean
  canBuy: boolean
  skuLoading: boolean
}>()

const emit = defineEmits<{
  select: [specName: string, value: string]
  addToCart: []
  buyNow: []
  'update:quantity': [value: number]
}>()

function onSelect(specName: unknown, value: unknown) {
  emit('select', specName as string, value as string)
}

function setQuantity(v: number) {
  emit('update:quantity', v)
}

function onQuantityInput(e: Event) {
  const input = e.target as HTMLInputElement
  let v = parseInt(input.value, 10)
  if (isNaN(v) || v < 1) v = 1
  if (v > props.maxQuantity) v = props.maxQuantity
  emit('update:quantity', v)
}
</script>

<style lang="scss" scoped>
@use '@/assets/styles/variables' as v;

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

.sku-section {
  padding: v.$spacing-md 0;
}

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

.action-row {
  display: flex;
  gap: v.$spacing-md;
  padding-top: v.$spacing-sm;
}

.action__btn--cart,
.action__btn--buy {
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

.action__btn--cart {
  background: #22C55E;
  color: #fff;

  &:hover:not(:disabled) {
    background: #16A34A;
    box-shadow: v.$shadow-md;
  }
}

.action__btn--buy {
  background: v.$color-primary;
  color: #fff;

  &:hover:not(:disabled) {
    background: v.$color-primary-light;
    box-shadow: v.$shadow-md;
  }
}

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

@media (max-width: 767px) {
  .action-row {
    flex-direction: column;
  }
}
</style>
