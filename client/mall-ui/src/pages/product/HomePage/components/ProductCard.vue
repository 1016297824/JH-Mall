<script setup lang="ts">
import type { SpuVO } from '@/types'
import { formatPrice } from '@/utils/common/format'

defineProps<{
  product: SpuVO
}>()
</script>

<template>
  <router-link :to="`/products/${product.spuId}`" class="product-card-link">
    <el-card class="product-card" shadow="hover" :body-style="{ padding: '12px' }">
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
    margin-bottom: v.$spacing-sm;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
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
  }

  &__sales {
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }
}
</style>
