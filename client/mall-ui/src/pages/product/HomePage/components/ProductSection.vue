<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import type { SpuVO } from '@/types'
import ProductCard from './ProductCard.vue'

const router = useRouter()

const props = withDefaults(defineProps<{
  products: SpuVO[]
  loading: boolean
  loadingMore?: boolean
}>(), {
  loadingMore: false,
})

const safeProducts = computed(() => props.products ?? [])
const isEmpty = computed(() => safeProducts.value.length === 0)

function goSearch() {
  router.push('/search')
}
</script>

<template>
  <section class="product-section">
    <h2 class="product-section__title">热销推荐</h2>

    <div v-if="props.loading" class="product-section__skeleton-grid">
      <div v-for="n in 4" :key="n" class="product-section__skeleton-card">
        <div class="product-section__skeleton-image" />
        <div class="product-section__skeleton-line product-section__skeleton-line--long" />
        <div class="product-section__skeleton-line product-section__skeleton-line--short" />
      </div>
    </div>

    <div v-else-if="isEmpty" class="product-section__empty">
      <p>暂无商品</p>
      <el-button type="primary" @click="goSearch">去逛逛</el-button>
    </div>

    <div v-else class="product-section__grid">
      <ProductCard
        v-for="product in safeProducts"
        :key="product.spuId"
        :product="product"
      />
    </div>
  </section>
</template>

<style lang="scss" scoped>
@use '@/assets/styles/variables' as v;

.product-section {
  margin-bottom: v.$spacing-xl;

  &__title {
    font-size: 22px;
    font-weight: 600;
    margin-bottom: v.$spacing-md;
  }

  &__grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: v.$spacing-md;
  }

  &__skeleton-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: v.$spacing-md;
  }

  &__skeleton-card {
    background: #fff;
    border-radius: v.$radius-lg;
    padding: v.$spacing-sm;
    border: 1px solid v.$color-border;
  }

  &__skeleton-image {
    aspect-ratio: 1;
    background: linear-gradient(90deg, v.$color-bg-card 25%, v.$color-border 50%, v.$color-bg-card 75%);
    background-size: 200% 100%;
    animation: shimmer 1.5s infinite;
    border-radius: v.$radius-md;
    margin-bottom: 12px;
  }

  &__skeleton-line {
    height: 14px;
    background: linear-gradient(90deg, v.$color-bg-card 25%, v.$color-border 50%, v.$color-bg-card 75%);
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

  &__empty {
    text-align: center;
    padding: v.$spacing-2xl 0;
    color: v.$color-text-secondary;

    p {
      margin-bottom: v.$spacing-md;
      font-size: 15px;
    }
  }
}

@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

@media (max-width: 1024px) {
  .product-section {
    &__grid,
    &__skeleton-grid {
      grid-template-columns: repeat(3, 1fr);
    }
  }
}

@media (max-width: 768px) {
  .product-section {
    &__grid,
    &__skeleton-grid {
      grid-template-columns: repeat(2, 1fr);
    }
  }
}
</style>
