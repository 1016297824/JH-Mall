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
        >
          <template #suffix>
            <span class="search-submit" @click="handleSearch" title="搜索">
              <el-icon :size="20"><Search /></el-icon>
            </span>
          </template>
        </el-input>
      </div>

      <div class="nav-actions">
        <el-badge :value="0" :max="99" class="cart-badge">
          <el-button circle :icon="ShoppingCartFull" aria-label="购物车" />
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

.search-submit {
  display: inline-flex;
  align-items: center;
  padding-right: 4px;
  color: $color-primary;
  cursor: pointer;
  transition: transform 0.15s ease, opacity 0.15s ease;

  &:hover {
    transform: scale(1.12);
  }

  &:active {
    transform: scale(1.0);
    opacity: 0.8;
  }
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
