<template>
  <div v-loading="loading" class="order-detail-page">
    <el-empty v-if="!loading && order === null" description="订单不存在或已被删除">
      <el-button type="primary" @click="goList">返回订单列表</el-button>
    </el-empty>

    <template v-else-if="order">
      <section class="detail-block">
        <h2 class="detail-block__title">订单状态</h2>
        <div class="detail-status">
          <span class="detail-status__text">{{ statusText }}</span>
          <el-tag v-if="order.waitPay" size="small" type="warning">待支付</el-tag>
        </div>
        <p class="detail-block__hint">订单号：{{ order.orderNo }}</p>
        <p class="detail-block__hint">下单时间：{{ formatDateTime(order.createTime) }}</p>
        <p v-if="order.cancelReason" class="detail-block__hint">
          取消原因：{{ order.cancelReason }}
        </p>
      </section>

      <section class="detail-block">
        <h2 class="detail-block__title">商品清单</h2>
        <ul class="goods-list">
          <li v-for="item in order.items" :key="item.id" class="goods-item">
            <img class="goods-item__image" :src="item.mainImage" :alt="item.skuName" />
            <div class="goods-item__info">
              <p class="goods-item__name">{{ item.skuName }}</p>
              <p class="goods-item__unit">{{ formatPrice(item.price) }} × {{ item.quantity }}</p>
            </div>
            <span class="goods-item__total">{{ formatPrice(item.totalPrice) }}</span>
          </li>
        </ul>
      </section>

      <section class="detail-block">
        <h2 class="detail-block__title">金额</h2>
        <dl class="amount-list">
          <div class="amount-row">
            <dt>商品总金额</dt>
            <dd>{{ formatPrice(order.totalAmount) }}</dd>
          </div>
          <div class="amount-row">
            <dt>优惠金额</dt>
            <dd>-{{ formatPrice(order.discountAmount) }}</dd>
          </div>
          <div class="amount-row">
            <dt>运费</dt>
            <dd>{{ formatPrice(order.freightAmount) }}</dd>
          </div>
          <div class="amount-row amount-row--pay">
            <dt>实付金额</dt>
            <dd>{{ formatPrice(order.payAmount) }}</dd>
          </div>
        </dl>
      </section>

      <section v-if="order.logisticsNo" class="detail-block">
        <h2 class="detail-block__title">物流信息</h2>
        <p class="detail-block__hint">{{ order.logisticsCompany }}：{{ order.logisticsNo }}</p>
      </section>

      <section v-if="order.remark" class="detail-block">
        <h2 class="detail-block__title">订单备注</h2>
        <p class="detail-block__hint">{{ order.remark }}</p>
      </section>

      <div class="detail-footer">
        <el-button @click="goList">返回列表</el-button>
        <el-button v-if="canApplyAfterSale" @click="goAfterSale">申请售后</el-button>
        <el-button v-if="order.waitPay" type="primary" @click="goPay">去支付</el-button>
        <el-button v-if="order.actions.includes(USER_CANCEL)" @click="onCancel">取消订单</el-button>
        <el-button
          v-if="order.actions.includes(CONFIRM_RECEIPT)"
          type="primary"
          @click="onConfirm"
        >
          确认收货
        </el-button>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cancelOrder, confirmReceipt, getOrderDetail, type Order } from '@/api/order'
import { ORDER_ACTION, ORDER_STATUS, ORDER_STATUS_TEXT } from '@/utils/constants'
import { formatDateTime, formatPrice } from '@/utils/common/format'

const route = useRoute()
const router = useRouter()

const order = ref<Order | null>(null)
const loading = ref(false)

const orderNo = computed(() => String(route.params.orderNo ?? ''))
const statusText = computed(() =>
  order.value ? order.value.orderStatusDesc || ORDER_STATUS_TEXT[order.value.orderStatus] : '',
)

// 模板里只做取值，操作码用常量引用避免写错字符串
const USER_CANCEL = ORDER_ACTION.USER_CANCEL
const CONFIRM_RECEIPT = ORDER_ACTION.CONFIRM_RECEIPT

onMounted(() => {
  void load()
})

async function load(): Promise<void> {
  if (orderNo.value === '') {
    return
  }
  loading.value = true
  try {
    order.value = await getOrderDetail(orderNo.value)
  } catch {
    // 提示已由响应拦截器统一弹出
    order.value = null
  } finally {
    loading.value = false
  }
}

function goList(): void {
  void router.push({ path: '/orders' })
}

function goPay(): void {
  void router.push({ path: `/payment/${orderNo.value}` })
}

/**
 * 是否展示售后入口
 *
 * <p>已支付未发货（仅退款）、待收货/已完成（退货退款）可申请；
 * 具体是否受理由后端按订单状态与售后时限校验。</p>
 */
const canApplyAfterSale = computed(() => {
  if (!order.value) {
    return false
  }
  return (
    order.value.orderStatus === ORDER_STATUS.PAID ||
    order.value.orderStatus === ORDER_STATUS.WAIT_RECEIVE ||
    order.value.orderStatus === ORDER_STATUS.COMPLETED
  )
})

function goAfterSale(): void {
  void router.push({ path: '/after-sales', query: { orderNo: orderNo.value } })
}

async function onCancel(): Promise<void> {
  try {
    await ElMessageBox.confirm('取消后订单将无法恢复，确定取消吗？', '取消订单', {
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    await cancelOrder(orderNo.value)
    ElMessage.success('订单已取消')
    await load()
  } catch {
    // 提示已由响应拦截器统一弹出
  }
}

async function onConfirm(): Promise<void> {
  try {
    await ElMessageBox.confirm('确认已收到商品？确认后订单将完成。', '确认收货', {
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    await confirmReceipt(orderNo.value)
    ElMessage.success('已确认收货')
    await load()
  } catch {
    // 提示已由响应拦截器统一弹出
  }
}
</script>

<style lang="scss" scoped>
.order-detail-page {
  max-width: 880px;
  margin: 0 auto;
  padding: 16px;
  min-height: 200px;
}

.detail-block {
  margin-bottom: 16px;
  padding: 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;

  &__title {
    margin: 0 0 10px;
    font-size: 15px;
    font-weight: 600;
  }

  &__hint {
    margin: 4px 0;
    font-size: 13px;
    color: var(--el-text-color-regular);
  }
}

.detail-status {
  display: flex;
  align-items: center;
  gap: 8px;

  &__text {
    font-size: 18px;
    font-weight: 600;
    color: var(--el-color-primary);
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
  padding: 10px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);

  &:last-child {
    border-bottom: none;
  }

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

.amount-list {
  margin: 0;

  .amount-row {
    display: flex;
    justify-content: space-between;
    padding: 4px 0;
    font-size: 14px;

    dt {
      color: var(--el-text-color-regular);
    }

    dd {
      margin: 0;
    }

    &--pay {
      padding-top: 8px;
      border-top: 1px solid var(--el-border-color-lighter);
      font-size: 16px;

      dd {
        font-weight: 600;
        color: var(--el-color-danger);
      }
    }
  }
}

.detail-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 8px 0;
}
</style>
