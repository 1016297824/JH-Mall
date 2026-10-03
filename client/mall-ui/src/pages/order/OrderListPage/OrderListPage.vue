<template>
  <div class="order-list-page">
    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <el-tab-pane
        v-for="tab in ORDER_STATUS_TABS"
        :key="tab.label"
        :label="tab.label"
        :name="tab.value === null ? 'all' : String(tab.value)"
      />
    </el-tabs>

    <el-empty v-if="!loading && orders.length === 0" description="暂无相关订单">
      <el-button type="primary" @click="goShopping">去逛逛</el-button>
    </el-empty>

    <ul v-else v-loading="loading" class="order-list">
      <li v-for="order in orders" :key="order.orderNo" class="order-card">
        <div class="order-card__head">
          <span class="order-card__no">订单号：{{ order.orderNo }}</span>
          <span class="order-card__time">{{ formatDate(order.createTime) }}</span>
          <el-tag class="order-card__status" size="small">{{ statusText(order) }}</el-tag>
        </div>

        <ul class="order-card__goods">
          <li v-for="item in order.items" :key="item.id" class="order-goods">
            <img class="order-goods__image" :src="item.mainImage" :alt="item.skuName" />
            <span class="order-goods__name">{{ item.skuName }}</span>
            <span class="order-goods__qty">× {{ item.quantity }}</span>
          </li>
        </ul>

        <div class="order-card__foot">
          <span class="order-card__amount">
            实付：<em>{{ formatPrice(order.payAmount) }}</em>
          </span>
          <el-button link type="primary" @click="goDetail(order.orderNo)">查看详情</el-button>
          <el-button v-if="order.canCancel" @click="onCancel(order)">取消订单</el-button>
          <el-button v-if="order.canConfirm" type="primary" @click="onConfirm(order)">
            确认收货
          </el-button>
        </div>
      </li>
    </ul>

    <div v-if="orders.length > 0" class="order-list__more">
      <el-button v-if="hasMore" :loading="loading" @click="loadMore">加载更多</el-button>
      <span v-else class="order-list__end">没有更多了</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cancelOrder, confirmReceipt, getOrders, type Order } from '@/api/order'
import {
  ORDER_ACTION,
  ORDER_PAGE_SIZE,
  ORDER_STATUS_TEXT,
  ORDER_STATUS_TABS,
} from '@/utils/constants'
import { formatDate, formatPrice } from '@/utils/common/format'

/** 列表行：把 actions 预先摊平成布尔量，避免模板里写判断逻辑 */
interface OrderRow extends Order {
  canCancel: boolean
  canConfirm: boolean
}

const router = useRouter()
const orders = ref<OrderRow[]>([])
const loading = ref(false)
const activeTab = ref('all')
/** 已请求页码，从 1 开始；用于「加载更多」 */
const page = ref(1)
const hasMore = ref(true)

onMounted(() => {
  void loadPage(true)
})

function statusText(order: Order): string {
  return order.orderStatusDesc || ORDER_STATUS_TEXT[order.orderStatus] || '未知状态'
}

function toRow(order: Order): OrderRow {
  return {
    ...order,
    canCancel: order.actions.includes(ORDER_ACTION.USER_CANCEL),
    canConfirm: order.actions.includes(ORDER_ACTION.CONFIRM_RECEIPT),
  }
}

/** 当前标签对应的状态过滤值；「全部」不传 */
function currentStatus(): number | undefined {
  return activeTab.value === 'all' ? undefined : Number(activeTab.value)
}

/**
 * 加载一页订单
 *
 * @param reset 是否重置列表（切标签、首次进入时为 true）
 */
async function loadPage(reset: boolean): Promise<void> {
  if (loading.value) return
  loading.value = true
  const target = reset ? 1 : page.value
  try {
    const list = await getOrders(currentStatus(), target, ORDER_PAGE_SIZE)
    const rows = list.map(toRow)
    orders.value = reset ? rows : [...orders.value, ...rows]
    page.value = target + 1
    // 后端未返回总数，以「本页是否满页」判断是否还有下一页
    hasMore.value = rows.length === ORDER_PAGE_SIZE
  } catch {
    // 提示已由响应拦截器统一弹出
    if (reset) {
      orders.value = []
    }
    hasMore.value = false
  } finally {
    loading.value = false
  }
}

function loadMore(): void {
  void loadPage(false)
}

function onTabChange(): void {
  orders.value = []
  hasMore.value = true
  void loadPage(true)
}

function goDetail(orderNo: string): void {
  void router.push({ path: `/orders/${orderNo}` })
}

function goShopping(): void {
  void router.push({ path: '/' })
}

/** 取消订单（二次确认，避免误触不可撤销操作） */
async function onCancel(order: OrderRow): Promise<void> {
  try {
    await ElMessageBox.confirm('取消后订单将无法恢复，确定取消吗？', '取消订单', {
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    await cancelOrder(order.orderNo)
    ElMessage.success('订单已取消')
    await loadPage(true)
  } catch {
    // 提示已由响应拦截器统一弹出
  }
}

/** 确认收货 */
async function onConfirm(order: OrderRow): Promise<void> {
  try {
    await ElMessageBox.confirm('确认已收到商品？确认后订单将完成。', '确认收货', {
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    await confirmReceipt(order.orderNo)
    ElMessage.success('已确认收货')
    await loadPage(true)
  } catch {
    // 提示已由响应拦截器统一弹出
  }
}
</script>

<style lang="scss" scoped>
.order-list-page {
  max-width: 1080px;
  margin: 0 auto;
  padding: 16px;
}

.order-list {
  margin: 0;
  padding: 0;
  list-style: none;

  &__more {
    display: flex;
    justify-content: center;
    padding: 16px 0;
  }

  &__end {
    font-size: 13px;
    color: var(--el-text-color-secondary);
  }
}

.order-card {
  margin-bottom: 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  overflow: hidden;

  &__head {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 10px 12px;
    background: var(--el-fill-color-light);
    font-size: 13px;
  }

  &__time {
    color: var(--el-text-color-secondary);
  }

  &__status {
    margin-left: auto;
  }

  &__goods {
    margin: 0;
    padding: 0 12px;
    list-style: none;
  }

  &__foot {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 10px 12px;
  }

  &__amount {
    margin-left: auto;
    font-size: 14px;

    em {
      font-style: normal;
      font-weight: 600;
      color: var(--el-color-danger);
    }
  }
}

.order-goods {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);

  &:last-child {
    border-bottom: none;
  }

  &__image {
    width: 48px;
    height: 48px;
    object-fit: cover;
    border-radius: 4px;
    flex-shrink: 0;
  }

  &__name {
    flex: 1;
    min-width: 0;
    font-size: 14px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__qty {
    font-size: 13px;
    color: var(--el-text-color-secondary);
  }
}
</style>
