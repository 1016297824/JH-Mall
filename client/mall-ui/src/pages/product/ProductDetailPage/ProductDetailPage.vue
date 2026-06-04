<template>
  <div class="product-detail-page">
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="error" class="error">{{ error }}</div>
    <div v-else-if="detail" class="detail-container">
      <div class="image-section">
        <img :src="detail.mainImage" :alt="detail.spuName" class="main-image" />
        <div class="image-list" v-if="detail.images?.length">
          <img v-for="(img, idx) in detail.images" :key="img" :src="img" :alt="`商品缩略图 ${idx + 1}`" class="thumb" />
        </div>
      </div>
      <div class="info-section">
        <h1 class="title">{{ detail.spuName }}</h1>
        <p class="description" v-if="detail.description">{{ detail.description }}</p>
        <div class="price">
          <span class="price-label">价格</span>
          <span class="price-min">¥{{ (detail.priceMin / 100).toFixed(2) }}</span>
          <span v-if="detail.priceMin !== detail.priceMax">- ¥{{ (detail.priceMax / 100).toFixed(2) }}</span>
        </div>
        <div class="stats">
          <span>已售 {{ detail.salesCount || 0 }} 件</span>
          <span v-if="detail.reviewCount"> · {{ detail.reviewCount }} 条评价</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getSpuDetail } from '@/api/product'
import type { SpuDetailVO } from '@/types'

const route = useRoute()
const detail = ref<SpuDetailVO | null>(null)
const loading = ref(true)
const error = ref('')

onMounted(async () => {
  try {
    const spuId = route.params.id as string
    detail.value = await getSpuDetail(spuId)
  } catch {
    error.value = '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.product-detail-page { padding: 12px; }
.loading, .error { text-align: center; padding: 60px 0; color: var(--el-text-color-regular); }
.error { color: var(--el-color-danger); }
.detail-container { display: flex; gap: 24px; max-width: 1200px; margin: 0 auto; }
.image-section { flex: 1; }
.main-image { width: 100%; border-radius: 8px; }
.thumb { width: 60px; height: 60px; object-fit: cover; border-radius: 4px; margin: 4px; cursor: pointer; }
.info-section { flex: 1; }
.title { font-size: 22px; margin: 0 0 12px; }
.description { color: var(--el-text-color-regular); line-height: 1.6; }
.price { margin: 16px 0; }
.price-label { color: var(--el-text-color-secondary); margin-right: 8px; }
.price-min { font-size: 28px; color: var(--el-color-danger); font-weight: bold; }
.stats { color: var(--el-text-color-regular); font-size: 14px; }
</style>
