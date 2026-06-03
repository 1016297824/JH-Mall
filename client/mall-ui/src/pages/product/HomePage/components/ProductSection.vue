<script setup lang="ts">
import { computed } from 'vue'
import { HOME_SKELETON_COUNT, ROUTE_SEARCH } from '@/utils/constants/product'
import type { SpuVO } from '@/types'
import ProductCard from './ProductCard.vue'

const props = defineProps<{
  products: SpuVO[]
  loading: boolean
  loadingMore?: boolean
}>()

const safeProducts = computed(() => props.products ?? [])
const isEmpty = computed(() => safeProducts.value.length === 0)
</script>

<template>
  <section class="product-section">
    <h2 class="section-title">热销推荐</h2>

    <div v-if="props.loading" class="skeleton-grid">
      <div v-for="n in HOME_SKELETON_COUNT" :key="n" class="skeleton-card">
        <div class="skeleton-image" />
        <div class="skeleton-line skeleton-line--long" />
        <div class="skeleton-line skeleton-line--short" />
      </div>
    </div>

    <div v-else-if="isEmpty" class="empty-state">
      <p>暂无商品</p>
      <el-button type="primary" @click="$router.push(ROUTE_SEARCH)">去逛逛</el-button>
    </div>

    <div v-else class="product-grid">
      <ProductCard
        v-for="product in safeProducts"
        :key="product.spuId"
        :product="product"
      />
    </div>
  </section>
</template>

<style lang="scss" scoped>
@use '@/assets/styles/variables' as *;

.product-section {
  margin-bottom: $spacing-xl;
}

.section-title {
  font-size: 24px;
  font-weight: 600;
  margin-bottom: $spacing-md;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: $spacing-md;
}

.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: $spacing-md;
}

.skeleton-card {
  background: #FFF;
  border-radius: $radius-lg;
  padding: $spacing-sm;
  border: 1px solid $color-border;
}

.skeleton-image {
  aspect-ratio: 1;
  background: linear-gradient(90deg, #F3E8FF 25%, #EDE9FE 50%, #F3E8FF 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: $radius-md;
  margin-bottom: 12px;
}

.skeleton-line {
  height: 14px;
  background: linear-gradient(90deg, #F3E8FF 25%, #EDE9FE 50%, #F3E8FF 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: 4px;
  margin-bottom: 8px;

  &--long {
    width: 80%;
  }

  &--short {
    width: 50%;
  }
}

@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

.empty-state {
  text-align: center;
  padding: $spacing-2xl 0;
  color: $color-text-secondary;

  p {
    margin-bottom: $spacing-md;
    font-size: 16px;
  }
}

@media (max-width: 1024px) {
  .product-grid,
  .skeleton-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .product-grid,
  .skeleton-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
