# 首页（HomePage）设计规格

> 日期：2026-05-31 | 状态：已确认

## 1 概述

JH-Mall C 端首页，路由 `/`，无需登录。采用经典电商流布局：导航 → Banner → 类目 → 热销。

## 2 数据源

### 2.1 类目树

```
GET /api/product/categories
→ List<CategoryVO>   // 三级类目树（children 嵌套）
```

[CategoryVO](file:///e:/Workspace/AI/JH-Mall/server/mall/mall-product/src/main/java/com/mall/product/VO/CategoryVO.java) 字段：`categoryId, parentId, name, level, icon, sortOrder, path, children`

首页只取一级类目（level=1），展示图标 + 名称，点击跳转 `/categories/{categoryId}`。

### 2.2 热销商品

```
GET /api/product/spus?sort=sales_desc&size=8
→ PageResult<SpuVO>  // records[] 取前 8 条
```

[SpuVO](file:///e:/Workspace/AI/JH-Mall/server/mall/mall-product/src/main/java/com/mall/product/VO/SpuVO.java) 字段：`spuId, spuName, mainImage, priceMin, priceMax, salesCount, categoryId, brandId`

### 2.3 Banner（MVP 静态数据）

组件内 hardcode 3 条，不调后端：

```typescript
const banners = [
  { id: 1, image: '/images/banner/banner-1.png', title: '春季焕新', linkType: 'CATEGORY', linkTarget: '1' },
  { id: 2, image: '/images/banner/banner-2.png', title: '数码狂欢', linkType: 'CATEGORY', linkTarget: '2' },
  { id: 3, image: '/images/banner/banner-3.png', title: '新品首发', linkType: 'PRODUCT', linkTarget: '1' },
]
```

> 后续营销模块开发后替换为后端 API。

## 3 组件树

```
HomePage.vue                          // 页面容器，数据获取 + 分发
  ├── NavBar.vue                      // Logo + 搜索框 + 购物车图标 + 登录入口
  ├── BannerSwiper.vue                // el-carousel 轮播
  ├── CategoryGrid.vue                // 一级类目图标网格
  └── ProductSection.vue              // "🔥 热销推荐" 标题 + 商品网格
       └── ProductCard.vue (x8)       // 单个商品卡片
```

## 4 数据流

```
HomePage.vue onMounted()
  │
  ├── fetchCategories()
  │     └── GET /api/product/categories
  │           └── filter level=1 → reactive(categories)
  │
  ├── fetchHotProducts()
  │     └── GET /api/product/spus?sort=sales_desc&size=8
  │           └── records → reactive(hotProducts)
  │
  └── Template 使用 categories / hotProducts / banners(hardcode)
```

## 5 状态覆盖

| 状态 | 条件 | 表现 |
|------|------|------|
| 加载中 | `loading=true` | 骨架屏占位（Banner/类目/商品三区统一 animate-pulse） |
| 加载成功 | `loading=false, data.length>0` | 内容淡入（transition opacity） |
| 类目为空 | `categories.length===0` | 隐藏 CategoryGrid 区域 |
| 商品为空 | `hotProducts.length===0` | 展示"暂无商品"占位（引导去搜索） |
| 接口失败 | `error!=null` | ElMessage.error("加载失败，请稍后重试")，保留上次缓存 |

## 6 路由

```typescript
// router/index.ts
{
  path: '/',
  name: 'home',
  component: () => import('@/pages/home/HomePage.vue'),
  meta: { requiresAuth: false }
}
```

## 7 响应式行为

| 断点 | 导航栏 | Banner | 类目网格 | 商品网格 |
|------|--------|--------|:--:|:--:|
| ≥1440px | 全宽 + 居中容器 | 1280px 宽 | 5 列 | 4 列 |
| 768-1439px | 全宽 | 100% | 4 列 | 3 列 |
| <768px | 紧凑模式 | 100% 全宽 | 3 列 | 2 列 |

## 8 验收标准

- [ ] 页面 `/` 可访问，无需登录
- [ ] 类目树从 `/api/product/categories` 加载，点击跳转 `/categories/:id`
- [ ] 热销商品从 `/api/product/spus?sort=sales_desc&size=8` 加载
- [ ] Banner 轮播可滑动，3 张占位图
- [ ] 骨架屏在加载时显示，加载完成后淡入真实内容
- [ ] 商品卡片 hover 符合设计系统规范（scale 1.02 + shadow-lg, 200ms）
- [ ] 响应式：PC/Tablet/Mobile 三种断点正常
- [ ] 接口失败有 Toast 提示，不白屏
