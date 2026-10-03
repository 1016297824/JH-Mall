<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { BannerLinkType } from '@/utils/enums/product.enum'

interface Banner {
  id: number
  image: string
  title: string
  linkType: BannerLinkType
  linkTarget: string
}

const router = useRouter()

/** 与 CategoryGrid 保持同样的断点判断风格：小屏（≤768px）单独一套布局 */
const MOBILE_QUERY = '(max-width: 768px)'
const DESKTOP_BANNER_HEIGHT = '280px'
const isMobile = ref(false)

function updateViewport() {
  isMobile.value = window.matchMedia(MOBILE_QUERY).matches
}

onMounted(() => {
  updateViewport()
  window.addEventListener('resize', updateViewport)
})

onUnmounted(() => {
  window.removeEventListener('resize', updateViewport)
})

/**
 * 小屏把指示器移到图片下方（outside），避免压在图片自带的副标题上；
 * 桌面端维持默认的图片内指示器与 280px 固定高度。
 */
const indicatorPosition = computed(() => (isMobile.value ? 'outside' : ''))

/**
 * el-carousel 通过内联 height 钉住容器高度（样式表压不过内联），
 * 所以移动端高度必须由绑定值给出：按 2:1 还原图片比例，保证不裁掉图内副标题。
 * 上下限避免极端窄/宽屏下过高或过矮。
 */
const MOBILE_BANNER_MIN_HEIGHT = 130
const MOBILE_BANNER_MAX_HEIGHT = 210

const carouselHeight = computed(() => {
  if (!isMobile.value) {
    return DESKTOP_BANNER_HEIGHT
  }
  const viewportWidth = window.innerWidth
  const target = Math.round(viewportWidth / 2)
  return `${Math.min(MOBILE_BANNER_MAX_HEIGHT, Math.max(MOBILE_BANNER_MIN_HEIGHT, target))}px`
})

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
    router.push(`/categories/${banner.linkTarget}`)
  } else if (banner.linkType === BannerLinkType.PRODUCT) {
    router.push(`/products/${banner.linkTarget}`)
  }
}
</script>

<template>
  <el-carousel
    class="banner-swiper"
    :interval="4000"
    arrow="always"
    :height="carouselHeight"
    :indicator-position="indicatorPosition"
  >
    <el-carousel-item v-for="banner in banners" :key="banner.id">
      <div
        class="banner-swiper__slide"
        tabindex="0"
        @click="handleBannerClick(banner)"
        @keydown.enter="handleBannerClick(banner)"
        @keydown.space.prevent="handleBannerClick(banner)"
      >
        <img :src="banner.image" :alt="banner.title" />
        <div class="banner-swiper__overlay">
          <h2>{{ banner.title }}</h2>
        </div>
      </div>
    </el-carousel-item>
  </el-carousel>
</template>

<style lang="scss" scoped>
@use '@/assets/styles/variables' as v;

.banner-swiper {
  border-radius: v.$radius-lg;
  overflow: hidden;
  margin-bottom: v.$spacing-xl;

  // 移动端：图片按 2:1 呈现，指示器移到图片下方，避免压住图片自带的副标题
  @media (max-width: 768px) {
    margin-bottom: v.$spacing-md;
    border-radius: v.$radius-md;

    // 小屏隐藏左右箭头，改由底部指示器操作
    :deep(.el-carousel__arrow) {
      display: none;
    }

    // 外置指示器位于图片下方，与图片留出变量化间距
    :deep(.el-carousel__indicators--outside) {
      margin-top: v.$spacing-sm;

      .el-carousel__button {
        // 图片内指示器用白色，外置后背景是浅色页面，改用主色系保证对比度
        background-color: v.$color-primary;
        // 非活动态也保持可辨识，避免与浅色底几乎融为一体
        opacity: 0.4;
      }

      .el-carousel__indicator.is-active .el-carousel__button {
        background-color: v.$color-primary;
        opacity: 1;
      }
    }

    // 图片自带文案是图片像素，窄屏缩放后对比度会下降，补一层极轻的投影提升可读性
    :deep(.el-carousel__item div) {
      text-shadow: 0 1px 3px rgba(0, 0, 0, 0.18);
    }
  }

  // 指示器热区放大（视觉尺寸不变，仅扩大可点区域），桌面与移动端通用
  :deep(.el-carousel__button) {
    width: 24px;
    height: 6px;
    border-radius: v.$radius-full;
    background-color: #fff;
    opacity: 0.6;
    position: relative;

    &::after {
      content: '';
      position: absolute;
      inset: -12px -6px;
    }
  }

  :deep(.el-carousel__indicator.is-active .el-carousel__button) {
    opacity: 1;
    background-color: v.$color-primary-light;
  }

  &__slide {
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

  // 说明：三张 banner 图片内部已自带主标题文案，此 overlay 属重复文本层。
  // 移动端画面小、该层会与图片插画碰撞，故小屏隐藏；桌面端保持原样不动。
  &__overlay {
    position: absolute;
    bottom: v.$spacing-lg;
    left: v.$spacing-lg;

    h2 {
      color: #fff;
      font-size: 28px;
      font-weight: 700;
      text-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
    }

    @media (max-width: 768px) {
      display: none;
    }
  }
}
</style>
