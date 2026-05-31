<script setup lang="ts">
import type { SpuVO } from '@/types/product'
import { formatPrice } from '@/utils/format'

defineProps<{
  product: SpuVO
}>()
</script>

<template>
  <router-link :to="`/products/${product.spuId}`" class="product-card-link">
    <el-card class="product-card" shadow="hover" :body-style="{ padding: '12px' }">
      <div class="card-image">
        <img
          :src="product.mainImage"
          :alt="product.spuName"
          loading="lazy"
        />
      </div>
      <div class="card-info">
        <h3 class="card-name">{{ product.spuName }}</h3>
        <div class="card-price">
          <span class="price-symbol">¥</span>
          <span class="price-value">{{ formatPrice(product.priceMin) }}</span>
        </div>
        <span class="card-sales">已售 {{ product.salesCount }}</span>
      </div>
    </el-card>
  </router-link>
</template>

<style lang="scss" scoped>
@use '@/assets/styles/variables' as *;

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
}

.card-image {
  aspect-ratio: 1;
  overflow: hidden;
  border-radius: $radius-md;
  background: $color-bg-card;
  margin-bottom: $spacing-sm;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform $duration-normal $ease-default;
  }
}

.product-card:hover .card-image img {
  transform: scale(1.02);
}

.card-name {
  font-size: 14px;
  font-weight: 500;
  line-height: 1.4;
  margin-bottom: $spacing-sm;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-price {
  display: flex;
  align-items: baseline;
  margin-bottom: $spacing-xs;
}

.price-symbol {
  font-size: 14px;
  font-weight: 700;
  color: #EF4444;
}

.price-value {
  font-size: 22px;
  font-weight: 700;
  font-family: $font-heading;
  color: #EF4444;
}

.card-sales {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
