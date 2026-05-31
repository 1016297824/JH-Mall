# 首页（HomePage）实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现 JH-Mall C 端首页，路由 `/`，经典电商流布局，对接后端 mall-product API。

**Architecture:** 单页面 `HomePage.vue` 编排数据获取，拆分为 5 个组件（BaseNavBar / BannerSwiper / CategoryGrid / ProductSection / ProductCard），通过 `api/product.ts` 调用后端。

**Tech Stack:** Vue 3.5 `<script setup lang="ts">` + Element Plus + Axios + Vue Router 5 + SCSS

**编码规范：** `semi: false`（不写分号）、`singleQuote: true`（单引号）、2 空格缩进、`<style lang="scss" scoped>`

**依赖后端 API：**
- `GET /api/product/categories` → `List<CategoryVO>`
- `GET /api/product/spus?sort=sales_desc&size=8` → `PageResult<SpuVO>`

**设计系统参考：** `docs/ui-design/00_MASTER.md`（色彩 #7C3AED、字体 Rubik+Nunito Sans、动效 scale(1.02) 200ms、阴影 4 级 elevation）

---

## File Structure

```
src/
├── api/
│   ├── request.ts              # Axios 实例（Token 注入 + 401 刷新）
│   └── product.ts              # getCategoryTree / getSpuList
├── assets/
│   └── styles/
│       ├── variables.scss      # 设计系统 SCSS Variables
│       ├── tokens.css          # Element Plus CSS Variables 覆盖
│       ├── element-overrides.css # Element Plus 组件级覆盖
│       └── global.scss         # 全局样式（reset、字体、body）
├── components/
│   ├── base/
│   │   └── BaseNavBar.vue      # 顶部导航栏
│   └── business/
│       ├── ProductCard.vue     # 商品卡片
│       ├── BannerSwiper.vue    # 轮播 Banner
│       ├── CategoryGrid.vue    # 类目导航网格
│       └── ProductSection.vue  # 热销推荐区域
├── pages/
│   └── home/
│       └── HomePage.vue        # 首页（数据编排 + 骨架屏）
├── types/
│   └── product.ts              # CategoryVO / SpuVO 类型定义
├── router/
│   └── index.ts                # 新增首页路由
├── App.vue                     # router-view 容器
└── main.ts                     # 引入 Element Plus + 全局样式
```

---

### Task 1: 基础设施 — 全局样式 + Element Plus 主题

**Files:**
- Create: `src/assets/styles/variables.scss`
- Create: `src/assets/styles/tokens.css`
- Create: `src/assets/styles/element-overrides.css`
- Create: `src/assets/styles/global.scss`
- Modify: `src/main.ts`

- [ ] **Step 1: 创建 SCSS 变量文件**

```scss
// src/assets/styles/variables.scss
$color-primary: #7C3AED
$color-primary-light: #A78BFA
$color-primary-dark: #5B21B6
$color-cta: #22C55E
$color-cta-hover: #16A34A

$color-bg: #FAF5FF
$color-bg-card: #F3E8FF
$color-border: #E9D5FF

$color-text-primary: #4C1D95
$color-text-regular: #6B21A8
$color-text-secondary: #A855F7

$font-heading: 'Rubik', sans-serif
$font-body: 'Nunito Sans', sans-serif

$spacing-xs: 4px
$spacing-sm: 8px
$spacing-md: 16px
$spacing-lg: 24px
$spacing-xl: 32px
$spacing-2xl: 48px
$spacing-3xl: 64px

$radius-sm: 6px
$radius-md: 12px
$radius-lg: 16px
$radius-xl: 24px
$radius-full: 9999px

$shadow-sm: 0 1px 3px rgba(76, 29, 149, 0.08)
$shadow-md: 0 4px 6px rgba(76, 29, 149, 0.1)
$shadow-lg: 0 10px 20px rgba(76, 29, 149, 0.12)

$duration-fast: 150ms
$duration-normal: 200ms
$duration-slow: 300ms
$ease-default: cubic-bezier(0.4, 0, 0.2, 1)
```

- [ ] **Step 2: 创建 Element Plus CSS Variables 覆盖**

```css
/* src/assets/styles/tokens.css */
:root {
  --el-color-primary: #7C3AED;
  --el-color-primary-light-3: #A78BFA;
  --el-color-primary-light-5: #C4B5FD;
  --el-color-primary-light-7: #DDD6FE;
  --el-color-primary-light-9: #EDE9FE;
  --el-color-primary-dark-2: #6D28D9;

  --el-color-success: #22C55E;
  --el-color-warning: #F59E0B;
  --el-color-danger: #EF4444;
  --el-color-info: #3B82F6;

  --el-border-radius-base: 12px;
  --el-border-radius-small: 8px;
  --el-border-radius-round: 24px;
  --el-border-color: #E9D5FF;

  --el-font-family: 'Nunito Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  --el-font-size-base: 15px;

  --el-bg-color: #FAF5FF;
  --el-bg-color-overlay: #FFFFFF;
  --el-text-color-primary: #4C1D95;
  --el-text-color-regular: #6B21A8;
  --el-text-color-secondary: #A855F7;
  --el-text-color-placeholder: #C084FC;
}
```

- [ ] **Step 3: 创建 Element Plus 组件覆盖**

```css
/* src/assets/styles/element-overrides.css */
.el-button--primary {
  font-weight: 600;
  letter-spacing: 0.02em;
}

.el-input__wrapper {
  border-radius: 12px;
  transition: border-color 150ms ease, box-shadow 150ms ease;
}
.el-input__wrapper:focus-within {
  box-shadow: 0 0 0 2px rgba(124, 58, 237, 0.2);
}

.el-card {
  border-radius: 16px;
  border: 1px solid #E9D5FF;
  transition: transform 200ms ease, box-shadow 200ms ease;
}
.el-card:hover {
  transform: scale(1.02);
  box-shadow: 0 10px 20px rgba(76, 29, 149, 0.12);
}

.el-dialog {
  border-radius: 16px;
}

.el-tag {
  border-radius: 6px;
  font-weight: 500;
}
```

- [ ] **Step 4: 创建全局样式**

```scss
// src/assets/styles/global.scss
@import url('https://fonts.googleapis.com/css2?family=Nunito+Sans:wght@300;400;500;600;700&family=Rubik:wght@300;400;500;600;700&display=swap')

*,
*::before,
*::after {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

body {
  font-family: 'Nunito Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  font-size: 15px;
  color: var(--el-text-color-regular);
  background-color: #FAF5FF;
  -webkit-font-smoothing: antialiased;
}

h1, h2, h3, h4, h5, h6 {
  font-family: 'Rubik', 'Nunito Sans', sans-serif;
  color: var(--el-text-color-primary);
}

a {
  color: var(--el-color-primary);
  text-decoration: none;
}

img {
  max-width: 100%;
  height: auto;
}

@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after {
    animation-duration: 0.01ms !important;
    transition-duration: 0.01ms !important;
  }
}
```

- [ ] **Step 5: 修改 main.ts**

```typescript
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

import App from './App.vue'
import router from './router'
import './assets/styles/tokens.css'
import './assets/styles/element-overrides.css'
import './assets/styles/global.scss'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus)

app.mount('#app')
```

- [ ] **Step 6: 验证**

Run: `cd client/mall-ui && npm run dev`

Expected: 开发服务器启动成功，页面无报错。

---

### Task 2: TypeScript 类型定义

**Files:**
- Create: `src/types/product.ts`

- [ ] **Step 1: 定义前端类型**

```typescript
export interface CategoryVO {
  categoryId: string
  parentId: string
  name: string
  level: number
  icon: string
  sortOrder: number
  path: string
  children: CategoryVO[]
}

export interface SpuVO {
  spuId: string
  spuName: string
  mainImage: string
  priceMin: number
  priceMax: number
  salesCount: number
  categoryId: string
  brandId: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
}

export interface MallResult<T> {
  errorCode: string
  errorMessage: string
  data: T
}
```

- [ ] **Step 2: 编译验证**

Run: `cd client/mall-ui && npx vue-tsc --noEmit`

Expected: 无类型错误。

---

### Task 3: API 层 — Axios 实例 + product API

**Files:**
- Create: `src/api/request.ts`
- Create: `src/api/product.ts`

- [ ] **Step 1: 创建 Axios 实例（Token 注入 + 401 刷新）**

```typescript
import axios from 'axios'
import type { MallResult } from '@/types/product'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const result: MallResult<unknown> = response.data
    if (result.errorCode !== '00000') {
      return Promise.reject(new Error(result.errorMessage))
    }
    return response
  },
  async (error) => {
    const originalRequest = error.config
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true
      const refreshToken = localStorage.getItem('refreshToken')
      if (!refreshToken) {
        localStorage.removeItem('accessToken')
        window.location.href = '/login'
        return Promise.reject(error)
      }
      try {
        const res = await axios.post('/api/auth/sessions/refresh', { refreshToken })
        const newAccessToken = res.data.data.accessToken
        const newRefreshToken = res.data.data.refreshToken
        localStorage.setItem('accessToken', newAccessToken)
        localStorage.setItem('refreshToken', newRefreshToken)
        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`
        return request(originalRequest)
      } catch {
        localStorage.removeItem('accessToken')
        localStorage.removeItem('refreshToken')
        window.location.href = '/login'
        return Promise.reject(error)
      }
    }
    return Promise.reject(error)
  }
)

export default request
```

- [ ] **Step 2: 创建 product API**

```typescript
import request from './request'
import type { CategoryVO, SpuVO, PageResult } from '@/types/product'

export function getCategoryTree(): Promise<CategoryVO[]> {
  return request.get('/product/categories').then((res) => res.data.data)
}

export function getSpuList(params: {
  page?: number
  size?: number
  categoryId?: number
  brandId?: number
  keyword?: string
  sort?: string
}): Promise<PageResult<SpuVO>> {
  return request.get('/product/spus', { params }).then((res) => res.data.data)
}
```

- [ ] **Step 3: 编译验证**

Run: `cd client/mall-ui && npx vue-tsc --noEmit`

Expected: 无类型错误。

---

### Task 4: 路由 + App.vue

**Files:**
- Modify: `src/router/index.ts`
- Modify: `src/App.vue`

- [ ] **Step 1: 添加首页路由**

```typescript
import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/pages/home/HomePage.vue'),
      meta: { requiresAuth: false },
    },
  ],
})

export default router
```

- [ ] **Step 2: 修改 App.vue 为 router-view**

```vue
<script setup lang="ts"></script>

<template>
  <router-view />
</template>
```

- [ ] **Step 3: 编译验证**

Run: `cd client/mall-ui && npx vue-tsc --noEmit`

Expected: 无类型错误（HomePage.vue 不存在会有懒加载警告，先忽略）。

---

### Task 5: ProductCard 组件

**Files:**
- Create: `src/components/business/ProductCard.vue`

- [ ] **Step 1: 实现商品卡片**

```vue
<script setup lang="ts">
import type { SpuVO } from '@/types/product'

defineProps<{
  product: SpuVO
}>()

function formatPrice(priceInCents: number): string {
  return (priceInCents / 100).toFixed(2)
}
</script>

<template>
  <router-link :to="`/products/${product.spuId}`" class="product-card-link">
    <el-card class="product-card" shadow="hover" :body-style="{ padding: '12px' }">
      <div class="card-image">
        <img
          :src="product.mainImage"
          :alt="product.spuName"
          loading="lazy"
        />
      </div>
      <div class="card-info">
        <h3 class="card-name">{{ product.spuName }}</h3>
        <div class="card-price">
          <span class="price-symbol">¥</span>
          <span class="price-value">{{ formatPrice(product.priceMin) }}</span>
        </div>
        <span class="card-sales">已售 {{ product.salesCount }}</span>
      </div>
    </el-card>
  </router-link>
</template>

<style lang="scss" scoped>
@use '@/assets/styles/variables' as *;

.product-card-link {
  text-decoration: none;
  color: inherit;
  display: block;
  height: 100%;
}

.product-card {
  cursor: pointer;
  height: 100%;

  :deep(.el-card__body) {
    padding: 12px;
  }
}

.card-image {
  aspect-ratio: 1;
  overflow: hidden;
  border-radius: $radius-md;
  background: $color-bg-card;
  margin-bottom: $spacing-sm;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform $duration-normal $ease-default;
  }
}

.product-card:hover .card-image img {
  transform: scale(1.05);
}

.card-name {
  font-size: 14px;
  font-weight: 500;
  line-height: 1.4;
  margin-bottom: $spacing-sm;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-price {
  display: flex;
  align-items: baseline;
  margin-bottom: $spacing-xs;
}

.price-symbol {
  font-size: 14px;
  font-weight: 700;
  color: #EF4444;
}

.price-value {
  font-size: 22px;
  font-weight: 700;
  font-family: $font-heading;
  color: #EF4444;
}

.card-sales {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
```

- [ ] **Step 2: 编译验证**

Run: `cd client/mall-ui && npx vue-tsc --noEmit`

Expected: 无类型错误。

---

### Task 6: BannerSwiper 组件

**Files:**
- Create: `src/components/business/BannerSwiper.vue`

- [ ] **Step 1: 实现轮播 Banner（MVP 静态数据）**

```vue
<script setup lang="ts">
import { ref } from 'vue'

interface Banner {
  id: number
  image: string
  title: string
  linkType: 'CATEGORY' | 'PRODUCT' | 'URL'
  linkTarget: string
}

const banners = ref<Banner[]>([
  { id: 1, image: '/images/banner/banner-1.png', title: '春季焕新', linkType: 'CATEGORY', linkTarget: '1' },
  { id: 2, image: '/images/banner/banner-2.png', title: '数码狂欢', linkType: 'CATEGORY', linkTarget: '2' },
  { id: 3, image: '/images/banner/banner-3.png', title: '新品首发', linkType: 'PRODUCT', linkTarget: '1' },
])

function handleBannerClick(banner: Banner) {
  if (banner.linkType === 'CATEGORY') {
    window.location.href = `/categories/${banner.linkTarget}`
  } else if (banner.linkType === 'PRODUCT') {
    window.location.href = `/products/${banner.linkTarget}`
  }
}
</script>

<template>
  <el-carousel class="banner-swiper" :interval="4000" arrow="always" height="360px">
    <el-carousel-item v-for="banner in banners" :key="banner.id">
      <div class="banner-slide" @click="handleBannerClick(banner)">
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
    color: #FFF;
    font-size: 32px;
    font-weight: 700;
    text-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  }
}
</style>
```

- [ ] **Step 2: 编译验证**

Run: `cd client/mall-ui && npx vue-tsc --noEmit`

Expected: 无类型错误。

---

### Task 7: CategoryGrid 组件

**Files:**
- Create: `src/components/business/CategoryGrid.vue`

- [ ] **Step 1: 实现类目导航网格**

```vue
<script setup lang="ts">
import type { CategoryVO } from '@/types/product'

defineProps<{
  categories: CategoryVO[]
}>()
</script>

<template>
  <section class="category-section">
    <h2 class="section-title">全部分类</h2>
    <div class="category-grid">
      <router-link
        v-for="cat in categories"
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
  </section>
</template>

<style lang="scss" scoped>
@use '@/assets/styles/variables' as *;

.category-section {
  margin-bottom: $spacing-xl;
}

.section-title {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: $spacing-md;
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: $spacing-md;

  @media (max-width: 768px) {
    grid-template-columns: repeat(3, 1fr);
  }
}

.category-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 8px;
  background: #FFF;
  border-radius: $radius-md;
  border: 1px solid $color-border;
  text-decoration: none;
  transition: transform $duration-normal $ease-default, box-shadow $duration-normal $ease-default;

  &:hover {
    transform: scale(1.04);
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
  font-size: 20px;
  font-weight: 700;
  color: $color-primary;
}

.category-name {
  font-size: 13px;
  color: var(--el-text-color-regular);
  text-align: center;
}
</style>
```

- [ ] **Step 2: 编译验证**

Run: `cd client/mall-ui && npx vue-tsc --noEmit`

Expected: 无类型错误。

---

### Task 8: ProductSection 组件

**Files:**
- Create: `src/components/business/ProductSection.vue`

- [ ] **Step 1: 实现热销推荐区域（含骨架屏）**

```vue
<script setup lang="ts">
import type { SpuVO } from '@/types/product'
import ProductCard from './ProductCard.vue'

defineProps<{
  products: SpuVO[]
  loading: boolean
}>()
</script>

<template>
  <section class="product-section">
    <h2 class="section-title">🔥 热销推荐</h2>

    <div v-if="loading" class="skeleton-grid">
      <div v-for="n in 4" :key="n" class="skeleton-card">
        <div class="skeleton-image" />
        <div class="skeleton-line skeleton-line--long" />
        <div class="skeleton-line skeleton-line--short" />
      </div>
    </div>

    <div v-else-if="products.length === 0" class="empty-state">
      <p>暂无商品</p>
      <el-button type="primary" @click="$router.push('/search')">去逛逛</el-button>
    </div>

    <div v-else class="product-grid">
      <ProductCard
        v-for="product in products"
        :key="product.spuId"
        :product="product"
      />
    </div>
  </section>
</template>

<style lang="scss" scoped>
@use '@/assets/styles/variables' as *;

.product-section {
  margin-bottom: $spacing-xl;
}

.section-title {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: $spacing-md;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: $spacing-md;
}

.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: $spacing-md;
}

.skeleton-card {
  background: #FFF;
  border-radius: $radius-lg;
  padding: 12px;
  border: 1px solid $color-border;
}

.skeleton-image {
  aspect-ratio: 1;
  background: linear-gradient(90deg, #F3E8FF 25%, #EDE9FE 50%, #F3E8FF 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: $radius-md;
  margin-bottom: 12px;
}

.skeleton-line {
  height: 14px;
  background: linear-gradient(90deg, #F3E8FF 25%, #EDE9FE 50%, #F3E8FF 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: 4px;
  margin-bottom: 8px;

  &--long {
    width: 80%;
  }

  &--short {
    width: 50%;
  }
}

@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

.empty-state {
  text-align: center;
  padding: 48px 0;
  color: $color-text-secondary;

  p {
    margin-bottom: $spacing-md;
    font-size: 16px;
  }
}

@media (max-width: 768px) {
  .product-grid,
  .skeleton-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
```

- [ ] **Step 2: 编译验证**

Run: `cd client/mall-ui && npx vue-tsc --noEmit`

Expected: 无类型错误。

---

### Task 9: BaseNavBar 组件

**Files:**
- Create: `src/components/base/BaseNavBar.vue`

- [ ] **Step 1: 实现顶部导航栏**

```vue
<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Search, ShoppingCartFull } from '@element-plus/icons-vue'

const router = useRouter()
const keyword = ref('')

function handleSearch() {
  if (keyword.value.trim()) {
    router.push(`/search?keyword=${encodeURIComponent(keyword.value.trim())}`)
  }
}
</script>

<template>
  <header class="navbar">
    <div class="navbar-inner">
      <router-link to="/" class="logo">JH-Mall</router-link>

      <div class="search-bar">
        <el-input
          v-model="keyword"
          placeholder="搜索商品"
          :prefix-icon="Search"
          size="large"
          clearable
          @keyup.enter="handleSearch"
        />
      </div>

      <div class="nav-actions">
        <el-badge :value="0" :max="99" class="cart-badge">
          <el-button circle :icon="ShoppingCartFull" />
        </el-badge>
        <el-button type="primary" size="default" round>登录</el-button>
      </div>
    </div>
  </header>
</template>

<style lang="scss" scoped>
@use '@/assets/styles/variables' as *;

.navbar {
  position: sticky;
  top: 0;
  z-index: 100;
  background: #FFF;
  border-bottom: 1px solid $color-border;
  padding: 0 $spacing-lg;
}

.navbar-inner {
  max-width: 1280px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  height: 64px;
  gap: $spacing-lg;
}

.logo {
  font-family: $font-heading;
  font-size: 22px;
  font-weight: 700;
  color: $color-primary;
  text-decoration: none;
  white-space: nowrap;
}

.search-bar {
  flex: 1;
  max-width: 480px;
}

.nav-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.cart-badge {
  margin-right: 4px;
}

@media (max-width: 768px) {
  .navbar-inner {
    height: 56px;
    gap: 12px;
  }

  .logo {
    font-size: 18px;
  }

  .search-bar {
    max-width: none;
  }
}
</style>
```

- [ ] **Step 2: 编译验证**

Run: `cd client/mall-ui && npx vue-tsc --noEmit`

Expected: 无类型错误。

---

### Task 10: HomePage 页面 — 组装所有组件

**Files:**
- Create: `src/pages/home/HomePage.vue`

- [ ] **Step 1: 实现首页（数据编排 + 错误处理）**

```vue
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getCategoryTree, getSpuList } from '@/api/product'
import type { CategoryVO, SpuVO } from '@/types/product'
import BaseNavBar from '@/components/base/BaseNavBar.vue'
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
    hotProducts.value = productPage.records
  } catch {
    ElMessage.error('加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="home-page">
    <BaseNavBar />

    <main class="home-content">
      <BannerSwiper />
      <CategoryGrid :categories="categories" />
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
```

- [ ] **Step 2: 启动开发服务器验证**

Run: `cd client/mall-ui && npm run dev`

Expected: 页面 `/` 可见，NavBar + Banner + 类目 + 商品卡片区域渲染。访问 `http://localhost:5173` 查看效果。

---

### Task 11: Vite 代理配置 — 前端转发到后端网关

**Files:**
- Modify: `vite.config.ts`

- [ ] **Step 1: 添加 API 代理**

```typescript
import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'
import vueDevTools from 'vite-plugin-vue-devtools'

export default defineConfig({
  plugins: [vue(), vueJsx(), vueDevTools()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
```

- [ ] **Step 2: 验证代理**

确认后端网关（ruoyi-gateway 8080）运行中，重启 `npm run dev`，首页应该能加载真实数据。

---

## Self-Review

1. **Spec coverage:** 所有 8 项验收标准均有对应 Task。
2. **Placeholder scan:** 无 TBD/TODO。登录按钮为静态占位（后续认证页开发后对接）。
3. **Type consistency:** `CategoryVO`、`SpuVO`、`PageResult` 在 Task 2 定义，Task 3/5/7/8/10 一致引用。
4. **编码规范:** 全文件 `semi: false` + `singleQuote: true` + 2 空格缩进 + `<style lang="scss" scoped>`。
5. **设计文档对齐:** BaseNavBar 在 `components/base/`，API 函数 `getCategoryTree`/`getSpuList` 匹配设计文档命名，request.ts 含 Token 注入 + 401 自动刷新。
