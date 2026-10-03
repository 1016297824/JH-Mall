<template>
  <div v-loading="loading" class="payment-result-page">
    <el-result v-if="order" :icon="icon" :title="title" :sub-title="subTitle">
      <template #extra>
        <el-button @click="goOrderList">查看订单</el-button>
        <el-button v-if="!isPaid" type="primary" @click="goPay">继续支付</el-button>
        <el-button v-else type="primary" @click="goShopping">继续购物</el-button>
      </template>
    </el-result>

    <el-empty v-else-if="!loading" description="没有可展示的支付结果">
      <el-button type="primary" @click="goOrderList">返回订单列表</el-button>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getOrderDetail, type Order } from '@/api/order'
import { formatPrice } from '@/utils/common/format'

const route = useRoute()
const router = useRouter()

const order = ref<Order | null>(null)
const loading = ref(false)

const orderNo = computed(() => String(route.query.orderNo ?? ''))
/** 支付成功以订单是否还处于待支付为准，而不是以跳转来源的标记为准 */
const isPaid = computed(() => order.value !== null && !order.value.waitPay)

const icon = computed(() => (isPaid.value ? 'success' : 'warning'))
const title = computed(() => (isPaid.value ? '支付成功' : '尚未完成支付'))
const subTitle = computed(() => {
  if (!order.value) {
    return ''
  }
  return isPaid.value
    ? `订单 ${order.value.orderNo} 实付 ${formatPrice(order.value.payAmount)} 元，当前状态：${order.value.orderStatusDesc}`
    : `订单 ${order.value.orderNo} 仍处于待支付，请尽快完成支付以免超时关闭`
})

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

function goOrderList(): void {
  void router.push({ path: '/orders' })
}

function goPay(): void {
  void router.push({ path: `/payment/${orderNo.value}` })
}

function goShopping(): void {
  void router.push({ path: '/' })
}
</script>

<style lang="scss" scoped>
.payment-result-page {
  max-width: 720px;
  margin: 0 auto;
  padding: 16px;
  min-height: 200px;
}
</style>
