<script setup lang="ts">
import type { SpuVO } from '@/types'
import { formatPrice } from '@/utils/common/format'

defineProps<{
  product: SpuVO
}>()
</script>

<template>
  <router-link :to="`/products/${product.spuId}`" class="product-card-link">
    <el-card class="product-card" shadow="hover">
      <div class="product-card__image">
        <img
          :src="product.mainImage"
          :alt="product.spuName"
          loading="lazy"
        />
      </div>
      <div class="product-card__info">
        <h3 class="product-card__name">{{ product.spuName }}</h3>
        <div class="product-card__price">
          <span class="product-card__price-symbol">¥</span>
          <span class="product-card__price-value">{{ formatPrice(product.priceMin) }}</span>
        </div>
        <span class="product-card__sales">已售 {{ product.salesCount }}</span>
      </div>
    </el-card>
  </router-link>
</template>

<style lang="scss" scoped>
@use '@/assets/styles/variables' as v;

.product-card-link {
  text-decoration: none;
  color: inherit;
  display: block;
  height: 100%;
}

.product-card {
  cursor: pointer;
  height: 100%;

  :deep(.el-card__body) {
    padding: 12px;
  }

  &__image {
    aspect-ratio: 1;
    overflow: hidden;
    border-radius: v.$radius-md;
    background: v.$color-bg-card;
    margin-bottom: v.$spacing-sm;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      transition: transform v.$duration-normal v.$ease-default;
    }
  }

  &:hover &__image img {
    transform: scale(1.02);
  }

  &__name {
    font-size: 14px;
    font-weight: 500;
    line-height: 1.4;
    color: var(--el-text-color-primary);
    margin-bottom: v.$spacing-sm;
    // 固定占两行高度，保证同排卡片价格行对齐
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    min-height: 2.8em;
    word-break: break-all;
  }

  &__price {
    display: flex;
    align-items: baseline;
    margin-bottom: v.$spacing-xs;
  }

  &__price-symbol {
    font-size: 14px;
    font-weight: 700;
    color: v.$color-primary;
  }

  &__price-value {
    font-size: 20px;
    font-weight: 700;
    font-family: v.$font-heading;
    color: v.$color-primary;
    line-height: 1.1;
  }

  &__sales {
    font-size: 13px;
    // 弱化：销售数据用次级色，不与价格争夺注意力
    color: var(--el-text-color-placeholder);
    line-height: 1.2;
  }

  @media (max-width: 768px) {
    :deep(.el-card__body) {
      // 卡片内边距适当加大，缓解拥挤
      padding: v.$spacing-md;
    }

    &__image {
      margin-bottom: v.$spacing-md;
    }

    &__name {
      font-size: 14px;
      margin-bottom: v.$spacing-sm;
    }

    &__price-symbol {
      font-size: 13px;
    }

    &__price-value {
      // 价格比商品名更突出，小屏也不低于 18px
      font-size: 19px;
    }

    &:hover &__image img {
      // 触屏无 hover，避免误触放大
      transform: none;
    }
  }
}
</style>
