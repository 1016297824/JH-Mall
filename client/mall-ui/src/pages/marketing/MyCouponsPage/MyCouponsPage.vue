<template>
  <div class="my-coupons-page">
    <h2 class="my-coupons-page__title">我的优惠券</h2>

    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <el-tab-pane
        v-for="tab in COUPON_STATUS_TABS"
        :key="tab.label"
        :label="tab.label"
        :name="tab.value === null ? 'all' : String(tab.value)"
      />
    </el-tabs>

    <el-empty v-if="!loading && coupons.length === 0" description="这里还没有优惠券">
      <el-button type="primary" @click="goCouponCenter">去领券</el-button>
    </el-empty>

    <ul v-else v-loading="loading" class="coupon-list">
      <li v-for="coupon in coupons" :key="coupon.id" class="coupon-card">
        <div class="coupon-card__value">
          <em>{{ formatPrice(coupon.faceValue) }}</em>
          <span class="coupon-card__unit">元</span>
        </div>
        <div class="coupon-card__info">
          <p class="coupon-card__name">{{ coupon.couponName }}</p>
          <p class="coupon-card__rule">{{ thresholdText(coupon.minOrderAmount) }}</p>
          <p class="coupon-card__time">{{ expireText(coupon) }}</p>
        </div>
        <div class="coupon-card__action">
          <el-tag :type="statusTagType(coupon.recordStatus)" size="small">
            {{ statusText(coupon) }}
          </el-tag>
          <el-button
            v-if="coupon.recordStatus === COUPON_RECORD_STATUS.AVAILABLE"
            link
            type="primary"
            @click="goShopping"
          >
            去使用
          </el-button>
        </div>
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getMyCoupons, type CouponRecord } from '@/api/marketing'
import { COUPON_RECORD_STATUS, COUPON_RECORD_STATUS_TEXT, COUPON_STATUS_TABS } from '@/utils/constants'
import { formatPrice } from '@/utils/common/format'

const router = useRouter()
const coupons = ref<CouponRecord[]>([])
const loading = ref(false)
const activeTab = ref('all')

onMounted(() => {
  void load()
})

/** 当前标签对应的状态过滤值；「全部」不传 */
function currentStatus(): number | undefined {
  return activeTab.value === 'all' ? undefined : Number(activeTab.value)
}

async function load(): Promise<void> {
  loading.value = true
  try {
    coupons.value = await getMyCoupons(currentStatus())
  } catch {
    // 提示已由响应拦截器统一弹出
    coupons.value = []
  } finally {
    loading.value = false
  }
}

function onTabChange(): void {
  void load()
}

function statusText(coupon: CouponRecord): string {
  return coupon.recordStatusDesc || COUPON_RECORD_STATUS_TEXT[coupon.recordStatus] || '未知'
}

/** 只有可用券是绿色，其余用中性色，避免「已过期」也被当成好消息 */
function statusTagType(recordStatus: number): 'success' | 'info' {
  return recordStatus === COUPON_RECORD_STATUS.AVAILABLE ? 'success' : 'info'
}

function thresholdText(minOrderAmount: number): string {
  return minOrderAmount > 0 ? `满 ${formatPrice(minOrderAmount)} 元可用` : '无门槛'
}

function expireText(coupon: CouponRecord): string {
  if (coupon.expireTime === null || coupon.expireTime === '') {
    return '长期有效'
  }
  return `有效期至 ${coupon.expireTime.slice(0, 10)}`
}

function goCouponCenter(): void {
  void router.push({ path: '/coupons' })
}

function goShopping(): void {
  void router.push({ path: '/' })
}
</script>

<style lang="scss" scoped>
.my-coupons-page {
  max-width: 880px;
  margin: 0 auto;
  padding: 16px;

  &__title {
    margin: 0 0 12px;
    font-size: 18px;
  }
}

.coupon-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.coupon-card {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
  padding: 12px 16px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;

  &__value {
    display: flex;
    align-items: baseline;
    min-width: 100px;
    color: var(--el-color-danger);

    em {
      font-size: 24px;
      font-style: normal;
      font-weight: 600;
    }
  }

  &__unit {
    margin-left: 2px;
    font-size: 13px;
  }

  &__info {
    flex: 1;
    min-width: 0;
  }

  &__name {
    margin: 0;
    font-size: 15px;
    font-weight: 600;
  }

  &__rule,
  &__time {
    margin: 4px 0 0;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }

  &__action {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 6px;
  }
}
</style>
