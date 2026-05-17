# 项目编码规范（完整版）
> 基于阿里巴巴前端规约，结合本项目 ESLint/Oxlint/Prettier 配置。

## 通用原则
- 严格遵循本项目 ESLint、Oxlint、Prettier 配置
- TypeScript 优先，禁止 `any`（特殊情况需加注释说明）
- 命名清晰有语义，避免缩写和歧义
- 代码自解释优先，必要时补充注释

## Vue 3 强制规范
| # | 规范 |
|---|------|
| 1 | 必须使用 `<script setup lang="ts">` 语法糖 |
| 2 | 页面组件以 `Page` 结尾，如 `LoginPage.vue` |
| 3 | 基础组件以 `Base` 开头，放在 `components/base/`，如 `BaseButton.vue` |
| 4 | 业务组件大驼峰，放在 `components/business/`，如 `SkuSelector.vue` |
| 5 | 所有 Props 必须类型注解，非必填项设置默认值 |
| 6 | 所有 Emits 必须显式声明类型 |
| 7 | `v-for` 必须绑定唯一 key，**禁止使用数组索引** |
| 8 | **禁止 `v-for` 与 `v-if` 同级共存** |
| 9 | 模板中禁止复杂表达式，抽离到 `computed` |
| 10 | 样式必须添加 `scoped` 属性 |
| 11 | 类名使用 `kebab-case`，遵循 BEM 规范：`block__element--modifier` |
| 12 | 单文件组件代码行数不超过 500 行，超过必须拆分 |

## TypeScript 强制规范
| # | 规范 | 反例 |
|---|------|------|
| 1 | 使用 `const` / `let`，禁止使用 `var` | `var x = 1` |
| 2 | 变量/函数使用 `camelCase`，常量使用 `UPPER_SNAKE_CASE` | `const maxCount = 10` |
| 3 | 优先使用 `??` 空值合并运算符 | `val || 0` → `val ?? 0` |
| 4 | 优先使用解构赋值 | `const name = user.name` → `const { name } = user` |
| 5 | 使用可选链操作符 `?.` | `user && user.address && user.address.city` |
| 6 | 字符串使用单引号，模板字符串使用反引号 | `"hello"` → `'hello'` |
| 7 | 函数入参和返回值必须添加类型注解 | `function add(a, b) { return a + b }` |

## 组合式函数（Composables）强制规范
| # | 规范 | 反例 |
|---|------|------|
| 1 | 命名以 `use` 开头 + 驼峰，如 `useUserInfo.ts` | `userInfo.ts`、`getUser.ts` |
| 2 | 单一职责，一个组合式函数仅处理一类逻辑 | `useUserAndCart.ts`（同时处理用户+购物车） |
| 3 | 入参和返回值必须添加完整类型注解 | `const useUser = (id) => { ... }` |
| 4 | 内部副作用（请求、定时器、监听）需提供停止方法 | 未清理定时器导致内存泄漏 |
| 5 | 导出函数作为默认导出，便于统一导入 | `export const useUser = () => {}` 无默认导出 |

## Pinia 状态管理强制规范
| # | 规范 | 反例 |
|---|------|------|
| 1 | 模块文件命名：`xxx.store.ts`，如 `user.store.ts` | `user.ts`、`userModule.ts` |
| 2 | State 使用驼峰命名，Action 以动词开头 | `action: { userInfo() }` → `action: { fetchUserInfo() }` |
| 3 | 禁止外部直接修改 State，统一通过 Action 修改 | `store.userInfo = {}` |
| 4 | 异步 Action 必须处理异常并返回 Promise | 无 `try/catch` 的异步函数 |
| 5 | 按业务域拆分 Store，避免单一 Store 过大 | `app.store.ts` 包含所有业务状态 |

## API 层强制规范
| # | 规范 | 反例 |
|---|------|------|
| 1 | 按业务模块拆分文件，如 `api/user.ts`、`api/order.ts` | 所有接口写在 `api/index.ts` |
| 2 | 接口函数命名：`[请求方式][模块][操作]` | `userInfo()` → `getUserInfo()` |
| 3 | 请求参数和响应数据必须定义 TypeScript 类型 | 无类型注解的接口函数 |
| 4 | 统一使用封装后的请求实例，禁止直接使用 axios | `axios.get('/user')` |
| 5 | 接口错误统一在响应拦截器中处理 | 每个接口单独写错误处理逻辑 |

## Sass/SCSS 强制规范
> 统一使用 SCSS 语法（CSS兼容风格），禁止使用缩进式 Sass 语法

| # | 规范 | 反例 |
|---|------|------|
| 1 | 所有组件样式必须添加 `lang="scss"` 和 `scoped` 属性 | `<style>`、`<style lang="css">` |
| 2 | 嵌套层级**不超过3层**，避免样式权重过高 | `.a .b .c .d { ... }` |
| 3 | 类名遵循 BEM 规范，使用 `&` 符号简化嵌套 | 手动重复书写完整类名 |
| 4 | 全局变量/混入统一放在 `@/assets/styles/` 目录下 | 组件内硬编码魔法值 |
| 5 | 变量和函数使用 `kebab-case` 命名 | `$primaryColor`、`@mixin flexCenter` |
| 6 | 优先使用 `@mixin` 复用样式，禁止滥用 `@extend` | `@extend .btn;` |
| 7 | 禁止使用 `!important`（特殊情况需加注释说明） | `color: red !important;` |
| 8 | 数值为0时省略单位 | `margin: 0px;` → `margin: 0;` |

## 注释规范
| # | 规范 | 示例 |
|---|------|------|
| 1 | 组件注释：使用 JSDoc 说明用途、Props、Emits | `/** 用户卡片组件 @props {User} user - 用户信息 @emits {click} - 点击触发 */` |
| 2 | 复杂函数：注释"为什么做"而非"做了什么" | `// 兼容老接口，需将userId转为字符串（张三 2024-05-01）` |
| 3 | 特殊例外：注释原因 + 责任人 + 计划修复时间 | `color: red !important; // 紧急修复IE11样式，待v2.0重构（李四）` |
| 4 | TODO 注释：包含责任人 + 截止时间 | `// TODO: 王五 2024-06-01 优化查询性能` |

## 项目结构

```
src/
├── pages/ # 页面组件（Page.vue）
├── components/
│    ├── base/ # 基础组件（Base.vue）
│    └── business/ # 业务组件
├── composables/ # 组合式函数（use*.ts）
├── api/ # API 接口（按模块分文件）
├── stores/
│    └── modules/ # Pinia 状态管理（*.store.ts）
├── router/ # 路由配置
├── utils/ # 工具函数
├── assets/
│    ├── images/ # 图片资源
│    └── styles/ # 全局样式
│    ├── variables.scss # 全局变量（颜色、间距、字体）
│    ├── mixins.scss # 全局混入（flex、响应式、文本截断）
│    ├── reset.scss # CSS 重置样式
│    └── index.scss # 全局入口（仅导入其他文件）
├── layouts/ # 布局组件
├── App.vue
└── main.ts
```

## 代码示例
```vue
<template>
  <div class="user-card">
    <h2 class="user-card__name">{{ user.name }}</h2>
    <p class="user-card__email">{{ user.email }}</p>
    <BaseButton
      type="primary"
      :disabled="disabled"
      @click="handleClick"
    >
      查看详情
    </BaseButton>
  </div>
</template>

<script setup lang="ts">
import { BaseButton } from '@/components/base'

/**
 * 用户卡片组件
 * @props user - 用户信息对象
 * @props disabled - 是否禁用交互
 * @emits click - 点击卡片时触发，返回用户信息
 */
interface User {
  id: number
  name: string
  email: string
}

const props = withDefaults(defineProps<{
  user: User
  disabled?: boolean
}>(), {
  disabled: false,
})

const emit = defineEmits<{
  (e: 'click', user: User): void
}>()

const handleClick = () => {
  emit('click', props.user)
}
</script>

<style scoped lang="scss">
.user-card {
  padding: $spacing-base;
  border: 1px solid $border-color;
  border-radius: 4px;
  
  &__name {
    margin: 0 0 8px 0;
    font-size: 18px;
    font-weight: bold;
    color: $text-color-primary;
    @include text-ellipsis;
  }
  
  &__email {
    margin: 0;
    color: $text-color-secondary;
  }
}
</style>
```
