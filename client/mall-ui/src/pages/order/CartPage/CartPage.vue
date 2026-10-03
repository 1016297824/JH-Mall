<template>
  <div class="cart-page">
    <el-empty v-if="!loading && cartItems.length === 0" description="购物车还是空的">
      <el-button type="primary" @click="goShopping">去逛逛</el-button>
    </el-empty>

    <template v-else>
      <ul v-loading="loading" class="cart-list">
        <li v-for="item in cartItems" :key="item.id" class="cart-item">
          <el-checkbox
            :model-value="item.isSelected === 1"
            :disabled="!item.purchasable"
            @change="(val: boolean) => onToggle(item, val)"
          />
          <img class="cart-item__image" :src="item.mainImage" :alt="item.skuName" />
          <div class="cart-item__info">
            <p class="cart-item__name">{{ item.skuName }}</p>
            <p v-if="!item.purchasable" class="cart-item__tip">已下架或缺货，暂不可结算</p>
          </div>
          <span class="cart-item__price">{{ formatPrice(item.price) }}</span>
          <el-input-number
            :model-value="item.quantity"
            :min="1"
            :max="Math.max(1, item.availableQty)"
            :disabled="!item.purchasable"
            size="small"
            @change="(val: number | undefined) => onQuantityChange(item, val)"
          />
          <span class="cart-item__subtotal">{{ formatPrice(item.price * item.quantity) }}</span>
          <el-button link type="danger" @click="onRemove(item)">删除</el-button>
        </li>
      </ul>

      <div class="cart-footer">
        <el-checkbox :model-value="allSelected" @change="onToggleAll">全选</el-checkbox>
        <span class="cart-footer__total">
          合计：<em class="cart-footer__amount">{{ formatPrice(selectedAmount) }}</em>
        </span>
        <el-button type="primary" :disabled="selectedItems.length === 0" @click="goCheckout">
          去结算（{{ selectedItems.length }}）
        </el-button>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { storeToRefs } from 'pinia'
import { useOrderStore } from '@/stores/order.store'
import { formatPrice } from '@/utils/common/format'
import type { CartItem } from '@/api/order'

const router = useRouter()
const orderStore = useOrderStore()
// 只读地取用 store 状态；所有修改都经 action，符合"禁止外部直改 state"
const { cartItems, loading, selectedItems, selectedAmount, allSelected } = storeToRefs(orderStore)

onMounted(() => {
  void orderStore.fetchCart()
})

/** 勾选 / 取消勾选单项 */
async function onToggle(item: CartItem, isSelected: boolean): Promise<void> {
  await orderStore.toggleSelected(item.id, isSelected)
}

/** 全选 / 全不选 */
async function onToggleAll(val: boolean): Promise<void> {
  await orderStore.toggleAll(val)
}

/** 改数量 */
async function onQuantityChange(item: CartItem, val: number | undefined): Promise<void> {
  if (val === undefined || val === item.quantity) return
  await orderStore.updateQuantity(item.id, val)
}

/** 删除单项（二次确认） */
async function onRemove(item: CartItem): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定从购物车移除「${item.skuName}」？`, '提示', { type: 'warning' })
  } catch {
    // 用户取消
    return
  }
  await orderStore.removeItem(item.id)
  ElMessage.success('已移除')
}

function goShopping(): void {
  void router.push({ path: '/' })
}

function goCheckout(): void {
  void router.push({ path: '/checkout' })
}
</script>

<style lang="scss" scoped>
.cart-page {
  max-width: 1080px;
  margin: 0 auto;
  padding: 16px;
}

.cart-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.cart-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);

  &__image {
    width: 72px;
    height: 72px;
    object-fit: cover;
    border-radius: 4px;
    flex-shrink: 0;
  }

  &__info {
    flex: 1;
    min-width: 0;
  }

  &__name {
    margin: 0;
    font-size: 14px;
    line-height: 1.4;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__tip {
    margin: 4px 0 0;
    font-size: 12px;
    color: var(--el-color-warning);
  }

  &__price,
  &__subtotal {
    width: 96px;
    text-align: right;
    font-size: 14px;
  }

  &__subtotal {
    color: var(--el-color-danger);
    font-weight: 600;
  }
}

.cart-footer {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 0;
  position: sticky;
  bottom: 0;
  background: var(--el-bg-color);

  &__total {
    margin-left: auto;
    font-size: 14px;
  }

  &__amount {
    font-size: 20px;
    font-style: normal;
    font-weight: 600;
    color: var(--el-color-danger);
  }
}
</style>

