<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { getCategoryTree, getSpuList } from '@/api/product'
import type { CategoryVO, SpuVO } from '@/types/product'
import BannerSwiper from '@/components/business/BannerSwiper.vue'
import CategoryGrid from '@/components/business/CategoryGrid.vue'
import ProductSection from '@/components/business/ProductSection.vue'

const PAGE_SIZE = 8
const MAX_TOTAL = 100

const categories = ref<CategoryVO[]>([])
const hotProducts = ref<SpuVO[]>([])
const loading = ref(true)
const isLoadingMore = ref(false)
const totalLoaded = ref(0)
const noMore = ref(false)
const sentinelRef = ref<HTMLElement | null>(null)

let observer: IntersectionObserver | null = null

async function loadPage(page: number) {
  const result = await getSpuList({ sort: 'sales_desc', page, size: PAGE_SIZE })
  const rows = result.rows
  hotProducts.value.push(...rows)
  totalLoaded.value += rows.length
  if (rows.length < PAGE_SIZE || totalLoaded.value >= MAX_TOTAL) {
    noMore.value = true
  }
}

async function loadMore() {
  if (isLoadingMore.value || noMore.value) return
  isLoadingMore.value = true
  const nextPage = Math.floor(totalLoaded.value / PAGE_SIZE) + 1
  try {
    await loadPage(nextPage)
  } catch {
    ElMessage.error('加载失败')
  } finally {
    isLoadingMore.value = false
    await nextTick()
    observeSentinel()
  }
}

function setupObserver() {
  if (!sentinelRef.value) return
  observer = new IntersectionObserver(
    (entries) => {
      if (entries[0]?.isIntersecting) {
        loadMore()
      }
    },
    { rootMargin: '200px' },
  )
  observer.observe(sentinelRef.value)
}

function observeSentinel() {
  if (!observer || !sentinelRef.value) return
  observer.disconnect()
  if (!noMore.value) {
    observer.observe(sentinelRef.value)
  }
}

function teardownObserver() {
  observer?.disconnect()
  observer = null
}

onMounted(async () => {
  try {
    const [cats] = await Promise.all([
      getCategoryTree(),
      loadPage(1),
    ])
    categories.value = cats.filter((c) => c.level === 1)
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
      <ProductSection :products="hotProducts" :loading="loading" :loading-more="isLoadingMore" />

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
