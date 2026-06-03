<script setup lang="ts">
import { ref } from 'vue'
import { BannerLinkType } from '@/utils/enums/product.enum'

interface Banner {
  id: number
  image: string
  title: string
  linkType: BannerLinkType
  linkTarget: string
}

const banners = ref<Banner[]>([
  {
    id: 1,
    image: '/images/banner/banner-1.png',
    title: '春季焕新',
    linkType: BannerLinkType.CATEGORY,
    linkTarget: '1',
  },
  {
    id: 2,
    image: '/images/banner/banner-2.png',
    title: '数码狂欢',
    linkType: BannerLinkType.CATEGORY,
    linkTarget: '2',
  },
  {
    id: 3,
    image: '/images/banner/banner-3.png',
    title: '新品首发',
    linkType: BannerLinkType.PRODUCT,
    linkTarget: '1',
  },
])

function handleBannerClick(banner: Banner) {
  if (banner.linkType === BannerLinkType.CATEGORY) {
    window.location.href = `/categories/${banner.linkTarget}`
  } else if (banner.linkType === BannerLinkType.PRODUCT) {
    window.location.href = `/spus/${banner.linkTarget}`
  }
}
</script>

<template>
  <el-carousel class="banner-swiper" :interval="4000" arrow="always" height="360px">
    <el-carousel-item v-for="banner in banners" :key="banner.id">
      <div
        class="banner-slide"
        tabindex="0"
        @click="handleBannerClick(banner)"
        @keydown.enter="handleBannerClick(banner)"
        @keydown.space.prevent="handleBannerClick(banner)"
      >
        <img :src="banner.image" :alt="banner.title" />
        <div class="banner-overlay">
          <h2>{{ banner.title }}</h2>
        </div>
      </div>
    </el-carousel-item>
  </el-carousel>
</template>

<style lang="scss" scoped>
@use '@/assets/styles/variables' as *;

.banner-swiper {
  border-radius: $radius-lg;
  overflow: hidden;
  margin-bottom: $spacing-xl;

  @media (max-width: 768px) {
    display: none;
  }
}

.banner-slide {
  width: 100%;
  height: 100%;
  position: relative;
  cursor: pointer;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.banner-overlay {
  position: absolute;
  bottom: $spacing-lg;
  left: $spacing-lg;

  h2 {
    color: #fff;
    font-size: 32px;
    font-weight: 700;
    text-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  }
}
</style>
