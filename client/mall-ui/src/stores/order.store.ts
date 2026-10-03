import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import {
  deleteCartItem,
  deleteCartItems,
  getCartItems,
  putCartItemQuantity,
  putCartItemSelected,
  type CartItem,
} from '@/api/order'

/**
 * 订单 / 购物车 store
 *
 * <p>购物车的状态与操作集中在此，页面只负责展示与交互。
 * 所有写操作后重新拉取列表，避免前后端状态不一致（改量/勾选会影响合计与可购买性）。</p>
 */
export const useOrderStore = defineStore('order', () => {
  const cartItems = ref<CartItem[]>([])
  const loading = ref(false)

  /** 已勾选且可购买的项 —— 结算以它为准 */
  const selectedItems = computed(() =>
    cartItems.value.filter((item) => item.isSelected === 1 && item.purchasable),
  )

  /** 已勾选合计（单位：分） */
  const selectedAmount = computed(() =>
    selectedItems.value.reduce((sum, item) => sum + item.price * item.quantity, 0),
  )

  /** 购物车总件数（导航角标用） */
  const totalCount = computed(() =>
    cartItems.value.reduce((sum, item) => sum + item.quantity, 0),
  )

  /** 是否全部勾选（仅统计可购买项） */
  const allSelected = computed(() => {
    const purchasable = cartItems.value.filter((item) => item.purchasable)
    return purchasable.length > 0 && purchasable.every((item) => item.isSelected === 1)
  })

  /** 拉取购物车 */
  async function fetchCart() {
    loading.value = true
    try {
      cartItems.value = await getCartItems()
    } finally {
      loading.value = false
    }
  }

  /** 改数量 */
  async function updateQuantity(id: string, quantity: number) {
    await putCartItemQuantity(id, quantity)
    await fetchCart()
  }

  /** 勾选 / 取消勾选单项 */
  async function toggleSelected(id: string, isSelected: boolean) {
    await putCartItemSelected(id, isSelected)
    await fetchCart()
  }

  /** 全选 / 全不选（后端无批量接口，逐项提交） */
  async function toggleAll(isSelected: boolean) {
    await Promise.all(
      cartItems.value
        .filter((item) => item.purchasable)
        .map((item) => putCartItemSelected(item.id, isSelected)),
    )
    await fetchCart()
  }

  /** 删除单项 */
  async function removeItem(id: string) {
    await deleteCartItem(id)
    await fetchCart()
  }

  /** 清空购物车 */
  async function clearCart() {
    await deleteCartItems()
    cartItems.value = []
  }

  return {
    cartItems,
    loading,
    selectedItems,
    selectedAmount,
    totalCount,
    allSelected,
    fetchCart,
    updateQuantity,
    toggleSelected,
    toggleAll,
    removeItem,
    clearCart,
  }
})
