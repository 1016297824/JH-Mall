<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { getCategoryTree, getHotList } from '@/api/product'
import type { CategoryVO, SpuVO } from '@/types/product'
import BannerSwiper from '@/components/business/BannerSwiper.vue'
import CategoryGrid from '@/components/business/CategoryGrid.vue'
import ProductSection from '@/components/business/ProductSection.vue'

const DISPLAY_INITIAL = 8
const DISPLAY_STEP = 8
const HOT_LIMIT = 50

const categories = ref<CategoryVO[]>([])
const hotProducts = ref<SpuVO[]>([])
const displayCount = ref(DISPLAY_INITIAL)
const loading = ref(true)
const isLoadingMore = ref(false)
const sentinelRef = ref<HTMLElement | null>(null)

let observer: IntersectionObserver | null = null

const visibleProducts = computed(() => hotProducts.value.slice(0, displayCount.value))
const noMore = computed(() => displayCount.value >= hotProducts.value.length && !loading.value)

function showMore() {
  if (displayCount.value >= hotProducts.value.length) return
  isLoadingMore.value = true
  setTimeout(() => {
    displayCount.value = Math.min(displayCount.value + DISPLAY_STEP, hotProducts.value.length)
    isLoadingMore.value = false
  }, 300)
}

function setupObserver() {
  if (!sentinelRef.value) return
  observer = new IntersectionObserver(
    (entries) => {
      if (entries[0]?.isIntersecting && !noMore.value) {
        showMore()
      }
    },
    { rootMargin: '200px' },
  )
  observer.observe(sentinelRef.value)
}

function teardownObserver() {
  observer?.disconnect()
  observer = null
}

onMounted(async () => {
  try {
    const [cats, hot] = await Promise.all([
      getCategoryTree(),
      getHotList(HOT_LIMIT),
    ])
    categories.value = cats.filter((c) => c.level === 1)
    hotProducts.value = hot
  } catch {
    ElMessage.error('加载失败，请稍后重试')
  } finally {
    loading.value = false
    await nextTick()
    setupObserver()
  }
})

onUnmounted(() => {
  teardownObserver()
})
</script>

<template>
  <div class="home-page">
    <main class="home-content">
      <BannerSwiper />
      <CategoryGrid :categories="categories" :loading="loading" />
      <ProductSection :products="visibleProducts" :loading="loading" :loading-more="isLoadingMore" />

      <div class="load-sentinel">
        <div v-if="isLoadingMore" class="loading-more">
          <span class="spinner" />
          <span>正在加载更多</span>
        </div>
        <div v-else-if="noMore && hotProducts.length > 0" class="no-more">
          已经到底了
        </div>
        <div ref="sentinelRef" class="sentinel-trigger" />
      </div>
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

.load-sentinel {
  padding: $spacing-lg 0;
  text-align: center;
}

.loading-more {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: $spacing-sm;
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

.spinner {
  display: inline-block;
  width: 18px;
  height: 18px;
  border: 2px solid $color-border;
  border-top-color: $color-primary;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

.no-more {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.sentinel-trigger {
  height: 1px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
