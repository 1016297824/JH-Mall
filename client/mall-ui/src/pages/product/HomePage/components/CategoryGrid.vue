<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import type { CategoryVO } from '@/types'

const props = withDefaults(defineProps<{
  categories: CategoryVO[]
  loading?: boolean
}>(), {
  loading: false,
})

const expanded = ref(false)
const columnsPerRow = ref(5)
const gridRef = ref<HTMLElement | null>(null)

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
      grid-template-columns: repeat(3, 1fr);
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
      grid-template-columns: repeat(3, 1fr);
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

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
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
