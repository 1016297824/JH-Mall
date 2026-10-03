<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()

const tabs = [
  {
    path: '/',
    label: '首页',
    icon: 'home',
  },
  {
    path: '/categories',
    label: '分类',
    icon: 'categories',
  },
  {
    path: '/cart',
    label: '购物车',
    icon: 'cart',
  },
  {
    path: '/profile',
    label: '我的',
    icon: 'profile',
  },
] as const

function isActive(path: string): boolean {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}
</script>

<template>
  <nav class="tab-bar">
    <router-link
      v-for="tab in tabs"
      :key="tab.path"
      :to="tab.path"
      class="tab-item"
      :class="{ active: isActive(tab.path) }"
    >
      <svg v-if="tab.icon === 'home'" class="tab-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
        <polyline points="9 22 9 12 15 12 15 22" />
      </svg>
      <svg v-else-if="tab.icon === 'categories'" class="tab-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <rect x="3" y="3" width="7" height="7" rx="1" />
        <rect x="14" y="3" width="7" height="7" rx="1" />
        <rect x="3" y="14" width="7" height="7" rx="1" />
        <rect x="14" y="14" width="7" height="7" rx="1" />
      </svg>
      <svg v-else-if="tab.icon === 'cart'" class="tab-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <circle cx="9" cy="21" r="1" />
        <circle cx="20" cy="21" r="1" />
        <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6" />
      </svg>
      <svg v-else-if="tab.icon === 'profile'" class="tab-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
        <circle cx="12" cy="7" r="4" />
      </svg>
      <span class="tab-label">{{ tab.label }}</span>
    </router-link>
  </nav>
</template>

<style lang="scss" scoped>
@use '@/assets/styles/variables' as *;

.tab-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  justify-content: space-around;
  align-items: center;
  // 基础高度 + 刘海屏底部安全区，避免被 Home Indicator 遮挡
  height: calc(56px + env(safe-area-inset-bottom, 0px));
  background: #FFF;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  // 内边距把内容顶到安全区之上
  padding-bottom: env(safe-area-inset-bottom, 0);
  box-sizing: border-box;
  z-index: 100;
}

.tab-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  flex: 1 1 0;
  // 5 个入口等宽、不换行；min-width:0 防止长文案把 flex 项撑开
  min-width: 0;
  height: 56px;
  text-decoration: none;
  color: var(--el-text-color-secondary);
  transition: color $duration-fast $ease-default;
  // 触摸热区足够大
  -webkit-tap-highlight-color: transparent;
}

.tab-item.active {
  color: $color-primary;
}

.tab-icon {
  width: 22px;
  height: 22px;
  flex-shrink: 0;
}

.tab-label {
  font-size: 11px;
  line-height: 1.1;
  white-space: nowrap;
}
</style>
