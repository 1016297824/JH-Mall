<template>
  <div v-loading="loading" class="payment-page">
    <el-empty v-if="!loading && order === null" description="订单不存在">
      <el-button type="primary" @click="goOrderList">返回订单列表</el-button>
    </el-empty>

    <template v-else-if="order">
      <section class="payment-block payment-block--amount">
        <p class="payment-amount__label">待支付金额</p>
        <p class="payment-amount__value">{{ formatPrice(order.payAmount) }}</p>
        <p class="payment-amount__no">订单号：{{ order.orderNo }}</p>
      </section>

      <section v-if="order.waitPay" class="payment-block">
        <h2 class="payment-block__title">选择支付方式</h2>
        <el-radio-group v-model="channelCode">
          <el-radio
            v-for="option in PAYMENT_CHANNEL_OPTIONS"
            :key="option.code"
            :value="option.code"
            border
          >
            {{ option.label }}
          </el-radio>
        </el-radio-group>
      </section>

      <section v-else class="payment-block">
        <h2 class="payment-block__title">订单状态</h2>
        <p class="payment-block__hint">{{ order.orderStatusDesc }}</p>
      </section>

      <section v-if="payResult" class="payment-block">
        <h2 class="payment-block__title">支付单</h2>
        <p class="payment-block__hint">支付单号：{{ payResult.paymentNo }}</p>
        <p v-if="channelUnavailable" class="payment-block__warn">
          当前后端仅有模拟渠道适配器，未接入真实微信/支付宝 SDK，无法在浏览器调起收银台。
        </p>
        <p v-else class="payment-block__hint">
          支付参数已就绪，接入真实渠道后在此调起 SDK。
        </p>
      </section>

      <div class="payment-footer">
        <el-button @click="goOrderList">返回订单</el-button>
        <el-button v-if="order.waitPay && !payResult" type="primary" :loading="paying" @click="onPay">
          立即支付
        </el-button>
        <el-button v-if="order.waitPay && payResult" :loading="refreshing" @click="refreshOrder">
          我已完成支付
        </el-button>
        <el-button
          v-if="order.waitPay && payResult && isDev"
          type="success"
          :loading="mockPaying"
          @click="onMockPay"
        >
          模拟支付成功（仅开发环境）
        </el-button>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getOrderDetail, type Order } from '@/api/order'
import { buildMockPayCallback, postMockPayCallback, postPayment, type PayResult } from '@/api/payment'
import { PAYMENT_CHANNEL, PAYMENT_CHANNEL_OPTIONS } from '@/utils/constants'
import { formatPrice } from '@/utils/common/format'

const route = useRoute()
const router = useRouter()

const order = ref<Order | null>(null)
const loading = ref(false)
const paying = ref(false)
const refreshing = ref(false)
const mockPaying = ref(false)
const channelCode = ref<string>(PAYMENT_CHANNEL.WECHAT)
const payResult = ref<PayResult | null>(null)

const orderNo = computed(() => String(route.params.orderNo ?? ''))
/** 生产构建里不存在此入口，避免用户在真实环境触发渠道回调 */
const isDev = import.meta.env.DEV
/**
 * 后端当前仅启用模拟渠道适配器，浏览器无法真正调起收银台
 *
 * <p>真实渠道接入前，把「支付参数已就绪」与「无可用 SDK」区分展示，
 * 避免用户以为点了没反应。</p>
 */
const channelUnavailable = computed(() => payResult.value !== null && isDev === false)

onMounted(() => {
  void loadOrder()
})

async function loadOrder(): Promise<void> {
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

/** 刷新订单，用于支付完成后回读真实状态 */
async function refreshOrder(): Promise<void> {
  refreshing.value = true
  try {
    await loadOrder()
    if (order.value && !order.value.waitPay) {
      ElMessage.success('支付已完成')
      void router.replace({ path: '/payment/result', query: { orderNo: orderNo.value } })
    } else {
      ElMessage.info('尚未收到支付结果，请稍后再试')
    }
  } finally {
    refreshing.value = false
  }
}

/** 发起支付 */
async function onPay(): Promise<void> {
  paying.value = true
  try {
    payResult.value = await postPayment({
      orderNo: orderNo.value,
      channelCode: channelCode.value,
    })
    ElMessage.success('支付单已创建')
  } catch {
    // 提示已由响应拦截器统一弹出
  } finally {
    paying.value = false
  }
}

/** 触发模拟渠道回调并回读订单状态（仅开发环境按钮可见） */
async function onMockPay(): Promise<void> {
  if (!payResult.value || !order.value) {
    return
  }
  mockPaying.value = true
  try {
    await postMockPayCallback(
      channelCode.value,
      buildMockPayCallback(payResult.value.paymentNo, order.value.payAmount),
    )
    await loadOrder()
    if (order.value && !order.value.waitPay) {
      ElMessage.success('模拟支付成功')
      void router.replace({ path: '/payment/result', query: { orderNo: orderNo.value } })
    } else {
      ElMessage.warning('回调已处理，但订单状态未推进，请查看服务端日志')
    }
  } catch {
    // 提示已由响应拦截器统一弹出
  } finally {
    mockPaying.value = false
  }
}

function goOrderList(): void {
  void router.push({ path: '/orders' })
}
</script>

<style lang="scss" scoped>
.payment-page {
  max-width: 880px;
  margin: 0 auto;
  padding: 16px;
  min-height: 200px;
}

.payment-block {
  margin-bottom: 16px;
  padding: 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;

  &--amount {
    text-align: center;
  }

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

  &__warn {
    margin: 4px 0;
    font-size: 13px;
    color: var(--el-color-warning);
  }
}

.payment-amount {
  &__label {
    margin: 0;
    font-size: 13px;
    color: var(--el-text-color-secondary);
  }

  &__value {
    margin: 8px 0;
    font-size: 28px;
    font-weight: 600;
    color: var(--el-color-danger);
  }

  &__no {
    margin: 0;
    font-size: 13px;
    color: var(--el-text-color-regular);
  }
}

.payment-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 8px 0;
}
</style>
