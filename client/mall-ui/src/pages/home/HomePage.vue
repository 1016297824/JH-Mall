<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getCategoryTree, getSpuList } from '@/api/product'
import type { CategoryVO, SpuVO } from '@/types/product'
import BannerSwiper from '@/components/business/BannerSwiper.vue'
import CategoryGrid from '@/components/business/CategoryGrid.vue'
import ProductSection from '@/components/business/ProductSection.vue'

const categories = ref<CategoryVO[]>([])
const hotProducts = ref<SpuVO[]>([])
const loading = ref(true)

onMounted(async () => {
  try {
    const [cats, productPage] = await Promise.all([
      getCategoryTree(),
      getSpuList({ sort: 'sales_desc', size: 8 }),
    ])
    categories.value = cats.filter((c) => c.level === 1)
    hotProducts.value = productPage.rows
  } catch {
    ElMessage.error('加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="home-page">
    <main class="home-content">
      <BannerSwiper />
      <CategoryGrid :categories="categories" :loading="loading" />
      <ProductSection :products="hotProducts" :loading="loading" />
    </main>
  </div>
</template>

<style lang="scss" scoped>
@use '@/assets/styles/variables' as *;

.home-page {
  min-height: 100vh;
}

.home-content {
  max-width: 1280px;
  margin: 0 auto;
  padding: $spacing-lg $spacing-lg $spacing-3xl;

  @media (max-width: 768px) {
    padding: 16px 16px 48px;
  }
}
</style>
