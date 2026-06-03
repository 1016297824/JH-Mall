<script setup lang="ts">
import { BaseNavBar, BaseTabBar, BaseFooter } from '@/components/base'
</script>

<template>
  <div class="app-layout">
    <BaseNavBar />
    <main class="layout-content">
      <router-view />
    </main>
    <BaseTabBar class="layout-tabbar" />
    <BaseFooter class="layout-footer" />
  </div>
</template>

<style lang="scss" scoped>
@use '@/assets/styles/variables' as *;

.app-layout {
  min-height: 100vh;
  padding-bottom: 56px;

  @media (min-width: 769px) {
    padding-bottom: 0;
  }
}

.layout-content {
  min-height: calc(100vh - 56px);

  @media (min-width: 769px) {
    min-height: auto;
  }
}

.layout-tabbar {
  @media (min-width: 769px) {
    display: none;
  }
}

.layout-footer {
  display: none;

  @media (min-width: 769px) {
    display: block;
  }
}
</style>
