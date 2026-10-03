<template>
  <div class="checkout-page">
    <section class="checkout-block">
      <h2 class="checkout-block__title">收货地址</h2>

      <el-empty
        v-if="!addressLoading && addresses.length === 0"
        :image-size="80"
        description="还没有收货地址"
      >
        <el-button type="primary" @click="goAddressManage">去添加</el-button>
      </el-empty>

      <ul v-else v-loading="addressLoading" class="address-list">
        <li
          v-for="addr in addresses"
          :key="addr.addressId"
          class="address-card"
          :class="{ 'address-card--active': addr.addressId === selectedAddressId }"
          @click="selectAddress(addr.addressId)"
        >
          <div class="address-card__head">
            <span class="address-card__name">{{ addr.receiverName }}</span>
            <span class="address-card__phone">{{ addr.receiverPhone }}</span>
            <el-tag v-if="addr.isDefault" size="small" type="danger">默认</el-tag>
            <el-tag v-else-if="addr.label" size="small">{{ addr.label }}</el-tag>
          </div>
          <p class="address-card__detail">{{ fullAddress(addr) }}</p>
        </li>
      </ul>
    </section>

    <section class="checkout-block">
      <h2 class="checkout-block__title">商品清单</h2>

      <el-empty v-if="selectedItems.length === 0" :image-size="80" description="没有可结算的商品">
        <el-button type="primary" @click="goCart">返回购物车</el-button>
      </el-empty>

      <ul v-else class="goods-list">
        <li v-for="item in selectedItems" :key="item.id" class="goods-item">
          <img class="goods-item__image" :src="item.mainImage" :alt="item.skuName" />
          <div class="goods-item__info">
            <p class="goods-item__name">{{ item.skuName }}</p>
            <p class="goods-item__unit">{{ formatPrice(item.price) }} × {{ item.quantity }}</p>
          </div>
          <span class="goods-item__total">{{ formatPrice(item.price * item.quantity) }}</span>
        </li>
      </ul>
    </section>

    <section class="checkout-block">
      <h2 class="checkout-block__title">订单备注</h2>
      <el-input
        v-model="remark"
        type="textarea"
        :rows="2"
        maxlength="200"
        show-word-limit
        placeholder="选填，如对配送时间的要求"
      />
    </section>

    <div class="checkout-footer">
      <span class="checkout-footer__label">商品金额</span>
      <em class="checkout-footer__amount">{{ formatPrice(selectedAmount) }}</em>
      <span class="checkout-footer__hint">优惠与运费以提交后后端计算结果为准</span>
      <el-button
        type="primary"
        size="large"
        :loading="submitting"
        :disabled="!canSubmit"
        @click="onSubmit"
      >
        提交订单
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { storeToRefs } from 'pinia'
import { createOrder } from '@/api/order'
import { getAddresses, type UserAddress } from '@/api/user'
import { useOrderStore } from '@/stores/order.store'
import { formatPrice } from '@/utils/common/format'

/** 列表中的地址必然已由后端落库，addressId 不为空；用更严格的类型表达该前提 */
interface CheckoutAddress extends UserAddress {
  addressId: string
}

const router = useRouter()
const orderStore = useOrderStore()
const { selectedItems, selectedAmount } = storeToRefs(orderStore)

const addresses = ref<CheckoutAddress[]>([])
const addressLoading = ref(false)
const selectedAddressId = ref<string | null>(null)
const remark = ref('')
const submitting = ref(false)

/**
 * 本次下单的幂等键
 *
 * <p>必须在页面内持有：提交失败后用户重试时沿用同一个键，后端才会返回同一张订单；
 * 下单成功后才更换新键，让下一次下单成为一笔新业务。放到 createOrder 内部生成
 * 会让「重试」变成「重复下单」。</p>
 */
const idempotentKey = ref(createIdempotentKey())

const canSubmit = computed(
  () => selectedAddressId.value !== null && selectedItems.value.length > 0,
)

onMounted(() => {
  void loadAddresses()
  // 结算依据是后端已勾选的购物车项，必须重新拉取而不能直接信任本地状态
  void orderStore.fetchCart().catch(() => undefined)
})

/** 生成幂等键，优先用 crypto.randomUUID（仅安全上下文可用） */
function createIdempotentKey(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  return `${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
}

/** 拉取收货地址，默认选中默认地址，否则选第一条 */
async function loadAddresses(): Promise<void> {
  addressLoading.value = true
  try {
    const list = await getAddresses()
    addresses.value = list.filter((item): item is CheckoutAddress => item.addressId !== null)
    const preferred = addresses.value.find((item) => item.isDefault) ?? addresses.value[0]
    selectedAddressId.value = preferred ? preferred.addressId : null
  } catch {
    // 提示已由响应拦截器统一弹出，这里只需保证页面处于空态而非卡在 loading
    addresses.value = []
  } finally {
    addressLoading.value = false
  }
}

function selectAddress(addressId: string): void {
  selectedAddressId.value = addressId
}

function fullAddress(addr: UserAddress): string {
  return `${addr.province}${addr.city}${addr.district}${addr.detailAddress}`
}

function goAddressManage(): void {
  void router.push({ path: '/address' })
}

function goCart(): void {
  void router.push({ path: '/cart' })
}

/** 提交订单 */
async function onSubmit(): Promise<void> {
  if (!canSubmit.value || selectedAddressId.value === null) {
    ElMessage.warning('请选择收货地址并确认购物车中有已勾选商品')
    return
  }
  submitting.value = true
  try {
    const orderNo = await createOrder(
      {
        addressId: selectedAddressId.value,
        remark: remark.value.trim() === '' ? undefined : remark.value.trim(),
      },
      idempotentKey.value,
    )
    // 成功才换键：否则重试会因为新键而真的下出第二单
    idempotentKey.value = createIdempotentKey()
    ElMessage.success(`下单成功，订单号 ${orderNo}`)
    // 后端已把下单商品移出购物车，刷新以同步列表与角标
    await orderStore.fetchCart().catch(() => undefined)
    await router.push({ path: '/orders' })
  } catch {
    // 提示已由响应拦截器统一弹出；保留幂等键供用户重试
  } finally {
    submitting.value = false
  }
}
</script>

<style lang="scss" scoped>
.checkout-page {
  max-width: 1080px;
  margin: 0 auto;
  padding: 16px;
}

.checkout-block {
  margin-bottom: 16px;

  &__title {
    margin: 0 0 12px;
    font-size: 16px;
    font-weight: 600;
  }
}

.address-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.address-card {
  flex: 1 1 320px;
  padding: 12px;
  border: 1px solid var(--el-border-color);
  border-radius: 6px;
  cursor: pointer;

  &--active {
    border-color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
  }

  &__head {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  &__name {
    font-weight: 600;
  }

  &__phone {
    color: var(--el-text-color-regular);
  }

  &__detail {
    margin: 8px 0 0;
    font-size: 13px;
    color: var(--el-text-color-regular);
  }
}

.goods-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.goods-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);

  &__image {
    width: 64px;
    height: 64px;
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
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__unit {
    margin: 4px 0 0;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }

  &__total {
    font-weight: 600;
    color: var(--el-color-danger);
  }
}

.checkout-footer {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 0;
  position: sticky;
  bottom: 0;
  background: var(--el-bg-color);

  &__label {
    margin-left: auto;
    font-size: 14px;
  }

  &__amount {
    font-size: 20px;
    font-style: normal;
    font-weight: 600;
    color: var(--el-color-danger);
  }

  &__hint {
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }
}
</style>
