<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import type { CategoryVO } from '@/types'

const props = withDefaults(defineProps<{
  categories: CategoryVO[]
  loading?: boolean
}>(), {
  loading: false,
})

/** 移动端折叠时展示 2 行，共 8 个分类 */
const MOBILE_COLLAPSED_ROWS = 2

const expanded = ref(false)
const columnsPerRow = ref(5)
const gridRef = ref<HTMLElement | null>(null)

function updateColumns() {
  if (window.matchMedia('(max-width: 768px)').matches) {
    columnsPerRow.value = 4
  } else if (window.matchMedia('(max-width: 1024px)').matches) {
    columnsPerRow.value = 4
  } else {
    columnsPerRow.value = 5
  }
}

const isMobile = ref(false)

function updateViewport() {
  isMobile.value = window.matchMedia('(max-width: 768px)').matches
  updateColumns()
}

onMounted(() => {
  updateViewport()
  window.addEventListener('resize', updateViewport)
})

onUnmounted(() => {
  window.removeEventListener('resize', updateViewport)
})

/** 折叠时展示的个数：移动端 2 行 × 4 列 = 8，桌面端 1 行 */
const collapsedCount = computed(() => (isMobile.value ? columnsPerRow.value * MOBILE_COLLAPSED_ROWS : columnsPerRow.value))

const visibleCategories = computed(() => {
  if (expanded.value) return props.categories
  return props.categories.slice(0, collapsedCount.value)
})

const hasMore = computed(() => props.categories.length > collapsedCount.value)

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
  const rowHeight = gridRef.value?.firstElementChild?.clientHeight || 100
  const gap = 12
  return { maxHeight: `${2 * rowHeight + gap}px`, overflowY: 'auto' as const }
})

function toggleExpand() {
  expanded.value = !expanded.value
}
</script>

<template>
  <section class="category-section">
    <div class="category-section__header">
      <h2 class="category-section__title">全部分类</h2>
      <button
        v-if="hasMore"
        class="category-section__more-btn"
        :aria-expanded="expanded"
        @click="toggleExpand"
      >
        <span>{{ expanded ? '收起' : '更多' }}</span>
        <svg
          class="category-section__toggle-icon"
          :class="{ 'category-section__toggle-icon--expanded': expanded }"
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

    <div v-if="props.loading" class="category-section__skeleton-grid">
      <div v-for="n in columnsPerRow" :key="n" class="category-section__skeleton-card" />
    </div>

    <template v-else-if="props.categories.length > 0">
      <div class="category-section__scroll" :style="gridScrollStyle">
        <div ref="gridRef" class="category-section__grid">
          <router-link
            v-for="cat in visibleCategories"
            :key="cat.categoryId"
            :to="`/categories/${cat.categoryId}`"
            class="category-section__item"
          >
            <div class="category-section__icon">
              <img v-if="cat.icon" :src="cat.icon" :alt="cat.name" loading="lazy" />
              <span v-else class="category-section__icon-fallback">{{ cat.name.charAt(0) }}</span>
            </div>
            <span class="category-section__name">{{ cat.name }}</span>
          </router-link>
        </div>
        <div v-if="needsScroll" class="category-section__scroll-fade" />
      </div>
    </template>

    <div v-else class="category-section__empty">
      <p>暂无分类</p>
    </div>
  </section>
</template>

<style lang="scss" scoped>
@use '@/assets/styles/variables' as v;

.category-section {
  margin-bottom: v.$spacing-xl;

  @media (max-width: 768px) {
    // 与 ProductSection 的区块间距统一
    margin-bottom: v.$spacing-lg;
  }

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: v.$spacing-md;
  }

  &__title {
    font-size: 22px;
    font-weight: 600;
    margin: 0;

    @media (max-width: 768px) {
      font-size: 18px;
    }
  }

  &__more-btn {
    display: flex;
    align-items: center;
    gap: 2px;
    padding: 4px 0;
    border: none;
    background: none;
    color: v.$color-primary;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    white-space: nowrap;
    transition: color v.$duration-fast v.$ease-default;

    &:hover {
      color: v.$color-primary-dark;
    }

    @media (max-width: 768px) {
      // 保留入口但弱化视觉，避免挤占主视觉；热区仍撑到 44px 高
      min-height: 44px;
      min-width: 44px;
      justify-content: flex-end;
      font-size: 13px;
      font-weight: 400;
      color: var(--el-text-color-secondary);
    }
  }

  &__toggle-icon {
    transition: transform v.$duration-normal v.$ease-default;

    &--expanded {
      transform: rotate(180deg);
    }
  }

  &__grid {
    display: grid;
    grid-template-columns: repeat(5, 1fr);
    gap: v.$spacing-md;

    @media (max-width: 1024px) {
      grid-template-columns: repeat(4, 1fr);
    }

    @media (max-width: 768px) {
      grid-template-columns: repeat(4, 1fr);
      // 缩小列间距，把宽度让给图标与文字，保证 320px 下仍能放下 4 列
      gap: v.$spacing-sm;
    }
  }

  &__skeleton-grid {
    display: grid;
    grid-template-columns: repeat(5, 1fr);
    gap: v.$spacing-md;

    @media (max-width: 1024px) {
      grid-template-columns: repeat(4, 1fr);
    }

    @media (max-width: 768px) {
      grid-template-columns: repeat(4, 1fr);
      gap: v.$spacing-sm;
    }
  }

  &__skeleton-card {
    aspect-ratio: 1;
    background: linear-gradient(90deg, v.$color-bg-card 25%, v.$color-border 50%, v.$color-bg-card 75%);
    background-size: 200% 100%;
    animation: shimmer 1.5s infinite;
    border-radius: v.$radius-md;
  }

  &__empty {
    text-align: center;
    padding: v.$spacing-2xl 0;
    color: var(--el-text-color-secondary);

    p {
      font-size: 14px;
    }
  }

  &__scroll {
    transition: max-height v.$duration-slow v.$ease-default;
  }

  &__scroll-fade {
    position: sticky;
    bottom: 0;
    left: 0;
    right: 0;
    height: 48px;
    background: linear-gradient(to bottom, transparent, rgba(255, 255, 255, 0.95));
    pointer-events: none;
  }

  &__item {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    // 保证可点热区不小于 44×44
    min-height: 44px;
    padding: 12px 6px;
    background: #fff;
    border-radius: v.$radius-md;
    border: 1px solid v.$color-border;
    text-decoration: none;
    transition:
      transform v.$duration-normal v.$ease-default,
      box-shadow v.$duration-normal v.$ease-default;

    &:hover {
      transform: scale(1.02);
      box-shadow: v.$shadow-md;
    }

    &:active {
      transform: scale(0.97);
    }

    @media (max-width: 768px) {
      padding: v.$spacing-md v.$spacing-xs;
      // 触屏无 hover，用按压反馈替代
      &:hover {
        transform: none;
        box-shadow: none;
      }
    }
  }

  &__icon {
    width: 48px;
    height: 48px;
    border-radius: v.$radius-md;
    background: v.$color-bg-card;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: v.$spacing-sm;
    overflow: hidden;
    flex-shrink: 0;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    @media (max-width: 768px) {
      width: 44px;
      height: 44px;
      // 图标与文字间距加大，避免拥挤
      margin-bottom: v.$spacing-md;
    }
  }

  &__icon-fallback {
    font-size: 18px;
    font-weight: 700;
    color: v.$color-primary;
  }

  &__name {
    font-size: 13px;
    color: var(--el-text-color-regular);
    text-align: center;
    line-height: 1.3;
    // 长分类名单行省略，避免撑破 4 列网格
    max-width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

.category-section__scroll::-webkit-scrollbar {
  width: 5px;
}

.category-section__scroll::-webkit-scrollbar-track {
  background: transparent;
}

.category-section__scroll::-webkit-scrollbar-thumb {
  background: v.$color-border;
  border-radius: 3px;

  &:hover {
    background: v.$color-primary-light;
  }
}
</style>
