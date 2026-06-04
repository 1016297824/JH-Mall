<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import type { CategoryVO } from '@/types'

const props = defineProps<{
  categories: CategoryVO[]
  loading?: boolean
}>()

const expanded = ref(false)
const columnsPerRow = ref(5)
const gridRef = ref<HTMLElement | null>(null)
const gridGap = ref(12)

function updateColumns() {
  if (window.matchMedia('(max-width: 768px)').matches) {
    columnsPerRow.value = 3
  } else if (window.matchMedia('(max-width: 1024px)').matches) {
    columnsPerRow.value = 4
  } else {
    columnsPerRow.value = 5
  }
}

onMounted(() => {
  updateColumns()
  window.addEventListener('resize', updateColumns)
})

onUnmounted(() => {
  window.removeEventListener('resize', updateColumns)
})

const visibleRowCount = computed(() => columnsPerRow.value)

const visibleCategories = computed(() => {
  if (expanded.value) return props.categories
  return props.categories.slice(0, visibleRowCount.value)
})

const hasMore = computed(() => props.categories.length > visibleRowCount.value)

const totalRows = computed(() => Math.ceil(props.categories.length / columnsPerRow.value))

const needsScroll = computed(() => expanded.value && totalRows.value > 2)

const gridScrollStyle = computed(() => {
  const rows = totalRows.value
  if (!expanded.value) {
    return { overflow: 'hidden' as const }
  }
  if (rows <= 2) {
    return { overflow: 'hidden' as const }
  }
  // 显示 2 行高度 + 滚动条
  const rowHeight = gridRef.value?.firstElementChild?.clientHeight || 100
  const gap = gridGap.value
  return { maxHeight: `${2 * rowHeight + gap}px`, overflowY: 'auto' as const }
})

function toggleExpand() {
  expanded.value = !expanded.value
}
</script>

<template>
  <section class="category-section">
    <div class="section-header">
      <h2 class="section-title">全部分类</h2>
      <button v-if="hasMore" class="more-btn" :aria-expanded="expanded" @click="toggleExpand">
        <span>{{ expanded ? '收起' : '更多' }}</span>
        <svg
          class="toggle-icon"
          :class="{ expanded: expanded }"
          width="14"
          height="14"
          viewBox="0 0 16 16"
          fill="none"
        >
          <path
            d="M4 6L8 10L12 6"
            stroke="currentColor"
            stroke-width="1.5"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        </svg>
      </button>
    </div>

    <div v-if="props.loading" class="skeleton-grid">
      <div v-for="n in columnsPerRow" :key="n" class="skeleton-card" />
    </div>

    <template v-else-if="props.categories.length > 0">
      <div class="category-grid-scroll" :style="gridScrollStyle">
        <div ref="gridRef" class="category-grid">
          <router-link
            v-for="cat in visibleCategories"
            :key="cat.categoryId"
            :to="`/categories/${cat.categoryId}`"
            class="category-item"
          >
            <div class="category-icon">
              <img v-if="cat.icon" :src="cat.icon" :alt="cat.name" loading="lazy" />
              <span v-else class="category-icon-fallback">{{ cat.name.charAt(0) }}</span>
            </div>
            <span class="category-name">{{ cat.name }}</span>
          </router-link>
        </div>
        <div v-if="needsScroll" class="scroll-fade" />
      </div>
    </template>

    <div v-else class="empty-state">
      <p>暂无分类</p>
    </div>
  </section>
</template>

<style lang="scss" scoped>
@use '@/assets/styles/variables' as *;

.category-section {
  margin-bottom: $spacing-xl;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: $spacing-md;
}

.section-title {
  font-size: 22px;
  font-weight: 600;
  margin: 0;
}

.more-btn {
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 4px 0;
  border: none;
  background: none;
  color: $color-primary;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
  transition: color $duration-fast $ease-default;

  &:hover {
    color: $color-primary-dark;
  }
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: $spacing-md;

  @media (max-width: 1024px) {
    grid-template-columns: repeat(4, 1fr);
  }

  @media (max-width: 768px) {
    grid-template-columns: repeat(3, 1fr);
  }
}

.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: $spacing-md;

  @media (max-width: 1024px) {
    grid-template-columns: repeat(4, 1fr);
  }

  @media (max-width: 768px) {
    grid-template-columns: repeat(3, 1fr);
  }
}

.skeleton-card {
  aspect-ratio: 1;
  background: linear-gradient(90deg, #f3e8ff 25%, #ede9fe 50%, #f3e8ff 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: $radius-md;
}

.empty-state {
  text-align: center;
  padding: $spacing-2xl 0;
  color: var(--el-text-color-secondary);

  p {
    font-size: 14px;
  }
}

@keyframes shimmer {
  0% {
    background-position: -200% 0;
  }
  100% {
    background-position: 200% 0;
  }
}

.category-grid-scroll {
  transition: max-height $duration-slow $ease-default;
}

.scroll-fade {
  position: sticky;
  bottom: 0;
  left: 0;
  right: 0;
  height: 48px;
  background: linear-gradient(to bottom, transparent, rgba(255, 255, 255, 0.95));
  pointer-events: none;
}

.category-grid-scroll::-webkit-scrollbar {
  width: 5px;
}

.category-grid-scroll::-webkit-scrollbar-track {
  background: transparent;
}

.category-grid-scroll::-webkit-scrollbar-thumb {
  background: $color-border;
  border-radius: 3px;

  &:hover {
    background: #7dd3fc;
  }
}

.category-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 6px;
  background: #fff;
  border-radius: $radius-md;
  border: 1px solid $color-border;
  text-decoration: none;
  transition:
    transform $duration-normal $ease-default,
    box-shadow $duration-normal $ease-default;

  &:hover {
    transform: scale(1.02);
    box-shadow: $shadow-md;
  }
}

.category-icon {
  width: 48px;
  height: 48px;
  border-radius: $radius-md;
  background: $color-bg-card;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: $spacing-sm;
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.category-icon-fallback {
  font-size: 18px;
  font-weight: 700;
  color: $color-primary;
}

.category-name {
  font-size: 13px;
  color: var(--el-text-color-regular);
  text-align: center;
}

.toggle-icon {
  transition: transform $duration-normal $ease-default;

  &.expanded {
    transform: rotate(180deg);
  }
}
</style>
