<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Search, ShoppingCartFull, User } from '@element-plus/icons-vue'
import { storeToRefs } from 'pinia'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth.store'
import { useOrderStore } from '@/stores/order.store'
import { LOGIN_PATH } from '@/utils/constants'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const orderStore = useOrderStore()
// 角标只读地取用 store 状态；购物车内容由 CartPage / CheckoutPage 负责拉取
const { totalCount } = storeToRefs(orderStore)

const keyword = ref('')

const isSearchPage = computed(() => route.path === '/search')
const isLoggedIn = computed(() => authStore.isLoggedIn)

onMounted(() => {
  // 未登录时不请求购物车：接口会返回 401，拦截器随即把用户弹到登录页
  if (authStore.isLoggedIn) {
    void orderStore.fetchCart().catch(() => undefined)
  }
})

function handleSearch() {
  if (keyword.value.trim()) {
    router.push(`/search?keyword=${encodeURIComponent(keyword.value.trim())}`)
  }
}

function goLogin(): void {
  void router.push({ path: LOGIN_PATH })
}

function goCart(): void {
  void router.push({ path: '/cart' })
}

function goPath(path: string): void {
  void router.push({ path })
}

async function onLogout(): Promise<void> {
  await authStore.logout()
  ElMessage.success('已退出登录')
  void router.push({ path: '/' })
}
</script>

<template>
  <header class="navbar">
    <div class="navbar-inner">
      <router-link to="/" class="logo">JH-Mall</router-link>

      <div v-if="!isSearchPage" class="search-bar">
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
        <el-button class="nav-actions__coupon" link @click="goPath('/coupons')">领券中心</el-button>
        <el-badge :value="totalCount" :max="99" :hidden="totalCount === 0" class="cart-badge">
          <el-button circle :icon="ShoppingCartFull" aria-label="购物车" @click="goCart" />
        </el-badge>

        <el-button v-if="!isLoggedIn" type="primary" size="default" round @click="goLogin">
          登录 / 注册
        </el-button>

        <el-dropdown v-else>
          <el-button round :icon="User">我的</el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="goPath('/coupons')">领券中心</el-dropdown-item>
              <el-dropdown-item @click="goPath('/profile')">个人中心</el-dropdown-item>
              <el-dropdown-item @click="goPath('/orders')">我的订单</el-dropdown-item>
              <el-dropdown-item @click="goPath('/after-sales')">退款/售后</el-dropdown-item>
              <el-dropdown-item @click="goPath('/coupons/mine')">我的优惠券</el-dropdown-item>
              <el-dropdown-item @click="goPath('/points')">我的积分</el-dropdown-item>
              <el-dropdown-item @click="goPath('/membership')">会员中心</el-dropdown-item>
              <el-dropdown-item @click="goPath('/address')">收货地址</el-dropdown-item>
              <el-dropdown-item divided @click="onLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
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
  /* 关键：flex 子项默认 min-width:auto，输入框的 min-content 宽度会把导航栏撑超出视口 */
  min-width: 0;
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
  .navbar {
    padding: 0 12px;
  }

  .navbar-inner {
    /* 移动端两行：第一行 logo + 操作，第二行整宽搜索框。
       单行放不下 logo/搜索/领券中心/购物车/我的五项，会把整页 min-content 宽度撑超视口 */
    flex-wrap: wrap;
    height: auto;
    padding: 8px 0;
    gap: 8px;
  }

  .logo {
    font-size: 18px;
    order: 1;
  }

  .nav-actions {
    order: 2;
    margin-left: auto;
    gap: 8px;
  }

  /* 领券中心收进「我的」下拉，避免挤占单行空间 */
  .nav-actions__coupon {
    display: none;
  }

  .search-bar {
    order: 3;
    flex: 1 1 100%;
    max-width: none;
  }
}
</style>
