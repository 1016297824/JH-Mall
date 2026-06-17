<template>
  <section class="gallery-section">
    <div class="main-image-wrapper">
      <img :src="currentImage" :alt="alt + ' 主图'" class="main-image" loading="eager" />
      <span v-if="soldOut" class="sold-out-overlay">已售罄</span>
    </div>
    <div v-if="images.length > 1" class="thumbnail-list">
      <button
        v-for="item in thumbnailList"
        :key="item.img"
        class="thumb-btn"
        :class="{ 'thumb-btn--active': activeIdx === item.idx }"
        :aria-label="'切换到第' + (item.idx + 1) + '张图'"
        @click="emit('select', item.idx)"
      >
        <img :src="item.img" :alt="'缩略图 ' + (item.idx + 1)" class="thumb-img" />
      </button>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  images: string[]
  fallback: string
  alt: string
  activeIdx: number
  soldOut: boolean
}>()

const emit = defineEmits<{
  select: [idx: number]
}>()

const currentImage = computed(() => props.images[props.activeIdx] ?? props.fallback)

const thumbnailList = computed(() => props.images.map((img, idx) => ({ img, idx })))
</script>

<style lang="scss" scoped>
@use '@/assets/styles/variables' as v;

.gallery-section {
  grid-area: gallery;
}

.main-image-wrapper {
  position: relative;
  width: 100%;
  aspect-ratio: 1;
  border-radius: v.$radius-lg;
  overflow: hidden;
  background: #fff;
  box-shadow: v.$shadow-md;
}

.main-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: opacity v.$duration-slow v.$ease-default;
}

.sold-out-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.75);
  font-size: 20px;
  font-weight: 700;
  color: v.$color-text-primary;
  font-family: v.$font-heading;
  letter-spacing: 0.05em;
}

.thumbnail-list {
  display: flex;
  gap: v.$spacing-sm;
  margin-top: v.$spacing-md;
  justify-content: center;
}

.thumb-btn {
  width: 64px;
  height: 64px;
  padding: 2px;
  border: 2px solid transparent;
  border-radius: v.$radius-sm;
  background: #fff;
  cursor: pointer;
  transition: border-color v.$duration-normal v.$ease-default;
  overflow: hidden;

  &:hover {
    border-color: v.$color-primary-light;
  }

  &--active {
    border-color: v.$color-primary;
  }
}

.thumb-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 2px;
}

@media (max-width: 767px) {
  .gallery-section {
    position: static;
  }

  .main-image-wrapper {
    border-radius: 0;
    margin: -12px -12px 0 -12px;
    width: calc(100% + 24px);
    aspect-ratio: 1;
    box-shadow: none;
  }

  .thumbnail-list {
    margin-top: v.$spacing-sm;
    overflow-x: auto;
    justify-content: flex-start;
    padding-bottom: v.$spacing-xs;
  }
}
</style>
