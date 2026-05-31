# JH-Mall C 端设计系统（Master）

> 生成工具：ui-ux-pro-max | 日期：2026-05-31
> 本文件为 C 端所有页面的设计基准，页面级覆盖文件存放于 `docs/ui-design/pages/`。

---

## 1 品牌调性与视觉策略

### 1.1 产品定位

| 维度 | 决策 |
|------|------|
| 产品类型 | 综合电商 B2C（多类目、高频交易） |
| 目标用户 | 全年龄段消费者（18-55 岁） |
| 品牌人格 | 活力、可信赖、现代、亲近 |
| 设计独立性 | 与 RuoYi 管理端完全隔离，无任何视觉共享 |

### 1.2 设计策略

```
以搜索为 CTA 核心的搜索驱动型电商首页。
降低搜索摩擦 → 推动用户快速触达商品 → 促进转化。

页面节奏：
  Hero搜索区（高对比度）
  → 分类导航网格（视觉图标 + 名称）
  → 推荐商品流（卡片式、悬停动效）
  → 新品/促销专区（信任锚点）
```

### 1.3 风格方向：Vibrant & Block-based

- **关键词**：大胆、活力、块状布局、几何图形、高对比度、双色调
- **适用性**：消费类产品、年轻人群体、社交媒体风格电商
- **组合策略**：Bento Grid 产品卡片 + Dimensional Layering 层级阴影

---

## 2 色彩系统

### 2.1 品牌色盘

| 角色 | 色值 | 用途 |
|------|------|------|
| **Primary（主色）** | `#7C3AED` | 主按钮、链接、品牌标识、重点元素 |
| **Primary Light** | `#A78BFA` | 辅助背景、hover 态、次要高亮 |
| **Primary Dark** | `#5B21B6` | 激活态、深色背景下的主色 |
| **CTA（行动色）** | `#22C55E` | 加入购物车、立即购买、支付确认 |
| **CTA Hover** | `#16A34A` | CTA 按钮 hover 态 |

### 2.2 中性色

| 色阶 | 色值 | 用途 |
|------|------|------|
| `--color-neutral-50` | `#FAF5FF` | 页面背景 |
| `--color-neutral-100` | `#F3E8FF` | 卡片背景、输入框背景 |
| `--color-neutral-200` | `#E9D5FF` | 边框、分割线 |
| `--color-neutral-300` | `#D8B4FE` | 禁用态边框 |
| `--color-neutral-500` | `#A855F7` | 次要文字 |
| `--color-neutral-700` | `#7E22CE` | 正文文字 |
| `--color-neutral-900` | `#4C1D95` | 标题文字 |

### 2.3 语义色

| 角色 | 色值 | 用途 |
|------|------|------|
| **Success** | `#22C55E` | 成功提示、库存充足 |
| **Warning** | `#F59E0B` | 库存紧张、即将过期 |
| **Error** | `#EF4444` | 错误提示、支付失败、售罄 |
| **Info** | `#3B82F6` | 信息提示、物流状态 |

### 2.4 Element Plus CSS Variables 覆盖

```css
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
  --el-font-size-base: 15px;
}
```

---

## 3 字体层级

### 3.1 字体选型

| 角色 | 字体 | 类别 | 备选 |
|------|------|------|------|
| **Heading** | Rubik | Sans-serif | Outfit |
| **Body** | Nunito Sans | Sans-serif | Work Sans |
| **Monospace** | JetBrains Mono | Monospace | — |

### 3.2 Google Fonts 引入

```css
@import url('https://fonts.googleapis.com/css2?family=Nunito+Sans:wght@300;400;500;600;700&family=Rubik:wght@300;400;500;600;700&family=JetBrains+Mono:wght@400;500&display=swap');
```

### 3.3 字体层级表

| 层级 | 标签 | 字号 | 字重 | 行高 | 用途 |
|:----:|------|:----:|:----:|:----:|------|
| H1 | `<h1>` | `32px` | `700` | `1.2` | 页面标题、商品名（详情页） |
| H2 | `<h2>` | `24px` | `600` | `1.3` | 区块标题（Card 标题） |
| H3 | `<h3>` | `20px` | `600` | `1.35` | 小标题、面板标题 |
| Body L | `<p>` | `16px` | `400` | `1.6` | 正文、商品描述 |
| Body M | `<p>` | `15px` | `400` | `1.5` | 列表项、表单标签（Element Plus 基准） |
| Body S | `<span>` | `13px` | `400` | `1.4` | 辅助信息、时间戳、销量 |
| Caption | `<small>` | `12px` | `400` | `1.35` | 免责声明、次要标注 |
| Price | `<strong>` | `22px` | `700` | `1` | 售价（使用 Rubik） |

### 3.4 Element Plus 字体覆盖

```css
:root {
  --el-font-family: 'Nunito Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

h1, h2, h3, h4, h5, h6,
.el-card__title,
.el-dialog__title {
  font-family: 'Rubik', 'Nunito Sans', sans-serif;
}
```

---

## 4 间距与栅格系统

### 4.1 间距 Tokens（8px 基准）

| Token | 值 | 用途 |
|:------|:--:|------|
| `--spacing-xs` | `4px` | 图标与文字间距 |
| `--spacing-sm` | `8px` | 标签内边距、紧凑元素间距 |
| `--spacing-md` | `16px` | 卡片内边距、表单项间距 |
| `--spacing-lg` | `24px` | 区块内间距 |
| `--spacing-xl` | `32px` | 区块间间距 |
| `--spacing-2xl` | `48px` | 大区块间距 |
| `--spacing-3xl` | `64px` | 页面级间距 |

### 4.2 栅格系统

| 断点 | 容器宽度 | 列数 | 列间距 | 应用场景 |
|:----:|:--------:|:----:|:----:|------|
| **PC（≥1440px）** | `max-w-7xl`（1280px） | 12 列 | `24px` | 主力 C 端 |
| **Tablet（≥768px）** | 100% - 32px padding | 8 列 | `16px` | 平板 |
| **Mobile（≥375px）** | 100% - 16px padding | 4 列 | `12px` | 手机 |

### 4.3 响应式布局策略

```
PC 端（1440px+）：全宽 -> 搜索栏居中 + 侧边筛选 + 商品网格
Tablet（768-1439px）：自适应 -> 筛选折叠为顶部横向栏
Mobile（375-767px）：流式 -> 筛选侧滑弹出 + 两列商品网格
```

---

## 5 阴影、圆角、边框

### 5.1 阴影层级（Elevation）

为 Element Plus 定制阴影系统，来源：Dimensional Layering。

| Token | CSS 值 | 用途 |
|:------|--------|------|
| `--shadow-none` | `none` | 扁平元素 |
| `--shadow-sm` | `0 1px 3px rgba(76, 29, 149, 0.08)` | 卡片默认、输入框 |
| `--shadow-md` | `0 4px 6px rgba(76, 29, 149, 0.1)` | 悬浮卡片、下拉菜单 |
| `--shadow-lg` | `0 10px 20px rgba(76, 29, 149, 0.12)` | 模态框、弹出层 |
| `--shadow-xl` | `0 20px 40px rgba(76, 29, 149, 0.15)` | 最高优先级浮层 |

### 5.2 圆角

| Token | 值 | 用途 |
|:------|:--:|------|
| `--radius-sm` | `6px` | 标签、Badge、小型按钮 |
| `--radius-md` | `12px` | 按钮、输入框、表格单元格 |
| `--radius-lg` | `16px` | 卡片（Bento Grid 保留 24px）、模态框 |
| `--radius-xl` | `24px` | 大卡片、图片容器 |
| `--radius-full` | `9999px` | 药丸按钮、头像 |

### 5.3 边框

| Token | 用途 |
|------|------|
| `border: 1px solid var(--color-neutral-200)` | 默认卡片边框 |
| `border: 1px solid var(--color-neutral-300)` | 输入框边框 |
| `border: 2px solid var(--el-color-primary)` | 聚焦态边框 |

---

## 6 动效规范

### 6.1 过渡曲线

| Token | 值 | 用途 |
|------|------|------|
| `--ease-default` | `cubic-bezier(0.4, 0, 0.2, 1)` | 通用过渡（Material Design 标准） |
| `--ease-out` | `cubic-bezier(0, 0, 0.2, 1)` | 元素进入 |
| `--ease-in` | `cubic-bezier(0.4, 0, 1, 1)` | 元素退出 |
| `--ease-spring` | `cubic-bezier(0.34, 1.56, 0.64, 1)` | 弹入效果（评分、徽章） |

### 6.2 时长

| Token | 值 | 用途 |
|:------|:--:|------|
| `--duration-fast` | `150ms` | 微交互（hover 颜色变化） |
| `--duration-normal` | `200ms` | 标准过渡（hover 阴影、展开） |
| `--duration-slow` | `300ms` | 页面切换、模态框进出 |
| `--duration-page` | `400ms` | 路由转场 |

### 6.3 微交互规范

| 元素 | 效果 | 时长 |
|------|------|:--:|
| 商品卡片 hover | `scale(1.02)` + `shadow-lg` | 200ms |
| 按钮 hover | 背景色 10% 加深 | 150ms |
| 按钮 active | `scale(0.97)` | 100ms |
| CTA 按钮 hover | 背景色变换 + 轻微发光 | 200ms |
| SKU 选择器 | 选中态边框变色 + 缩放 | 200ms |
| 数量选择器 | 按钮按下反馈 | 100ms |

### 6.4 页面切换

```
路由切换：opacity + translateY(8px) 淡入
  - 进入：400ms ease-out
  - 退出：200ms ease-in

列表加载：
  - 骨架屏（skeleton）→ 内容淡入（staggered 50ms/item）
```

### 6.5 无障碍动效

```css
@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after {
    animation-duration: 0.01ms !important;
    transition-duration: 0.01ms !important;
  }
}
```

---

## 7 Element Plus 主题定制方案

### 7.1 定制策略

采用 **CSS Variables 覆盖** 方式（非 SCSS 变量），完全通过 `:root` 覆盖 Element Plus 默认变量。

### 7.2 完整 Element Plus 覆盖变量

```css
:root {
  /* ===== 主色 ===== */
  --el-color-primary: #7C3AED;
  --el-color-primary-light-3: #A78BFA;
  --el-color-primary-light-5: #C4B5FD;
  --el-color-primary-light-7: #DDD6FE;
  --el-color-primary-light-8: #EDE9FE;
  --el-color-primary-light-9: #F5F3FF;
  --el-color-primary-dark-2: #6D28D9;

  /* ===== 语义色 ===== */
  --el-color-success: #22C55E;
  --el-color-success-light-3: #4ADE80;
  --el-color-success-light-5: #86EFAC;
  --el-color-warning: #F59E0B;
  --el-color-warning-light-3: #FBBF24;
  --el-color-warning-light-5: #FDE68A;
  --el-color-danger: #EF4444;
  --el-color-danger-light-3: #F87171;
  --el-color-info: #3B82F6;

  /* ===== 边框 & 圆角 ===== */
  --el-border-radius-base: 12px;
  --el-border-radius-small: 8px;
  --el-border-radius-round: 24px;
  --el-border-color: #E9D5FF;
  --el-border-color-light: #F3E8FF;
  --el-border-color-lighter: #FAF5FF;

  /* ===== 字体 ===== */
  --el-font-family: 'Nunito Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  --el-font-size-base: 15px;
  --el-font-size-small: 13px;
  --el-font-size-large: 16px;

  /* ===== 背景 & 文字 ===== */
  --el-bg-color: #FAF5FF;
  --el-bg-color-overlay: #FFFFFF;
  --el-text-color-primary: #4C1D95;
  --el-text-color-regular: #6B21A8;
  --el-text-color-secondary: #A855F7;
  --el-text-color-placeholder: #C084FC;

  /* ===== 填充色 ===== */
  --el-fill-color: #F5F3FF;
  --el-fill-color-light: #FAF5FF;
  --el-fill-color-blank: #FFFFFF;
}
```

### 7.3 Element Plus 组件级覆盖

```css
/* 按钮 */
.el-button--primary {
  --el-button-font-weight: 600;
  letter-spacing: 0.02em;
}

/* 输入框 */
.el-input__wrapper {
  border-radius: 12px;
  transition: border-color 150ms ease, box-shadow 150ms ease;
}
.el-input__wrapper:focus-within {
  box-shadow: 0 0 0 2px rgba(124, 58, 237, 0.2);
}

/* 卡片 */
.el-card {
  border-radius: 16px;
  border: 1px solid #E9D5FF;
  transition: transform 200ms ease, box-shadow 200ms ease;
}
.el-card:hover {
  transform: scale(1.02);
  box-shadow: 0 10px 20px rgba(76, 29, 149, 0.12);
}

/* 对话框 */
.el-dialog {
  border-radius: 16px;
}

/* 标签/Tag */
.el-tag {
  border-radius: 6px;
  font-weight: 500;
}
```

---

## 8 暗黑模式策略

### 8.1 策略选择

**一期不做暗黑模式**。原因：
- 电商 C 端用户以白天浏览为主
- Element Plus 暗黑模式对电商场景适配工作量极大（商品图片白色底在暗色背景下的视觉处理）
- 优先保障亮色模式的极致体验

### 8.2 为后续预留

```css
/* 所有颜色通过 CSS Variables 引用，暗黑模式时切换 :root[data-theme="dark"] */
:root[data-theme="dark"] {
  --color-neutral-50: #1A0A2E;
  --color-neutral-100: #2D1B4E;
  --color-neutral-200: #3B2468;
  --color-neutral-900: #F5F3FF;
  --el-bg-color: #1A0A2E;
  --el-text-color-primary: #F5F3FF;
  /* ... */
}
```

---

## 9 可访问性检查清单（WCAG 2.1 AA）

| # | 检查项 | 规范 |
|:--|------|------|
| 1 | 颜色对比度 ≥ 4.5:1（正文）/ 3:1（大文本 18px+） | 所有文字-背景组合必须通过 |
| 2 | 焦点可见 | `focus-visible:ring-2` + 对比色，**绝不** `outline: none` 无替代 |
| 3 | 键盘可操作 | 所有可交互元素支持 Tab 导航 + Enter/Space 触发 |
| 4 | 表单错误提示 | 使用 `role="alert"` 或 `aria-live` 播报，非仅红色边框 |
| 5 | 图片 alt 文本 | 商品图、Banner 必须有描述性 alt |
| 6 | 语义化 HTML | 正确使用 `<main>`、`<nav>`、`<h1>-<h6>` 层级 |
| 7 | ARIA 标签 | 图标按钮、自定义组件补充 `aria-label` |
| 8 | 动画尊重用户偏好 | `prefers-reduced-motion: reduce` 禁用动画 |
| 9 | 表单标签关联 | 所有 `<input>` 关联 `<label for="...">` |
| 10 | 跳过导航 | PC 端提供"跳到主要内容"链接 |

---

## 10 UX 反模式清单（电商常见陷阱）

| # | 反模式 | 正确做法 |
|:--|------|---------|
| 1 | 搜索结果空白页无引导 | 显示"未找到" + 推荐商品 + 修改搜索建议 |
| 2 | 购物车为空只显示文字 | 推荐热门商品卡片 + 引导去首页 |
| 3 | 表单提交无反馈 | Loading → Success/Error 明确状态转换 |
| 4 | 只提交时校验表单 | onBlur 即时校验 + 提交时兜底 |
| 5 | 删除操作无二次确认 | 购物车删除、订单取消等需弹窗确认 |
| 6 | 无限滚动加载 | 使用分页或"加载更多"按钮，保留 footer 可达性 |
| 7 | 价格变动无提示 | 购物车中价格变更需 toast 提示 |
| 8 | 登录态过期静默失败 | 401 → 自动 refreshToken → 失败则跳登录页并提示 |
| 9 | 支付按钮可重复点击 | 提交后立即 disabled + loading，带幂等 key |
| 10 | 图片不设尺寸占位 | 所有商品图设固定宽高比容器 + 骨架屏，防止 CLS |

---

## 附录 A：技术栈参考

| 层 | 选型 | 版本 |
|----|------|:----:|
| 框架 | Vue | 3.5 |
| 语言 | TypeScript | 6.0 |
| 构建 | Vite | 8 |
| UI 库 | Element Plus | 最新 |
| 路由 | Vue Router | 5 |
| 状态管理 | Pinia | 3 |
| HTTP | Axios | 最新 |
| 表单验证 | VeeValidate（推荐） | 最新 |
| Lint | ESLint 10 + Oxlint 1.60 | — |

---

## 附录 B：Vue 最佳实践速查

| 规则 | 做法 |
|------|------|
| 组件风格 | `<script setup lang="ts">` Composition API |
| 状态管理 | Pinia `defineStore`（setup 语法），不用 Vuex |
| 表单验证 | VeeValidate `useField` / `useForm`，不手写校验 |
| 全局状态 | 跨组件共享用 Pinia store，不依赖 props 深层传递 |
| 懒加载 | 图片 `loading="lazy"`，路由 `() => import(...)` |
| 骨架屏 | 加载态用 animate-pulse skeleton，不空白屏幕 |
