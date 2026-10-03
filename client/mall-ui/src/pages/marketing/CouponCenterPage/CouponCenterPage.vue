<template>
  <div class="coupon-center-page">
    <h2 class="coupon-center-page__title">领券中心</h2>

    <el-empty v-if="!loading && coupons.length === 0" description="暂无可领取的优惠券" />

    <ul v-else v-loading="loading" class="coupon-list">
      <li v-for="coupon in coupons" :key="coupon.id" class="coupon-card">
        <div class="coupon-card__value">
          <template v-if="coupon.discountRate">
            <em>{{ discountText(coupon.discountRate) }}</em>
          </template>
          <template v-else>
            <em>{{ formatPrice(coupon.faceValue) }}</em>
            <span class="coupon-card__unit">元</span>
          </template>
        </div>
        <div class="coupon-card__info">
          <p class="coupon-card__name">{{ coupon.couponName }}</p>
          <p class="coupon-card__rule">{{ thresholdText(coupon.minOrderAmount) }}</p>
          <p class="coupon-card__time">{{ validityText(coupon) }}</p>
        </div>
        <div class="coupon-card__action">
          <span class="coupon-card__remain">剩余 {{ coupon.remainCount }} 张</span>
          <el-button
            type="primary"
            :disabled="coupon.remainCount <= 0"
            :loading="claimingId === coupon.id"
            @click="onClaim(coupon)"
          >
            {{ coupon.remainCount > 0 ? '立即领取' : '已领完' }}
          </el-button>
        </div>
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getAvailableCoupons, postClaimCoupon, type CouponDef } from '@/api/marketing'
import { formatPrice } from '@/utils/common/format'

const coupons = ref<CouponDef[]>([])
const loading = ref(false)
/** 正在领取的券 ID，用于按钮级 loading 而非整页 loading */
const claimingId = ref<string | null>(null)

/** 折扣率转文案：85 → 8.5 折 */
function discountText(rate: number): string {
  return `${(rate / 10).toFixed(1)} 折`
}

onMounted(() => {
  void load()
})

async function load(): Promise<void> {
  loading.value = true
  try {
    coupons.value = await getAvailableCoupons()
  } catch {
    // 提示已由响应拦截器统一弹出
    coupons.value = []
  } finally {
    loading.value = false
  }
}

/** 使用门槛文案 */
function thresholdText(minOrderAmount: number): string {
  return minOrderAmount > 0 ? `满 ${formatPrice(minOrderAmount)} 元可用` : '无门槛'
}

/** 有效期文案，后端给的是有效期区间 */
function validityText(coupon: CouponDef): string {
  const start = formatDatePart(coupon.useStartTime)
  const end = formatDatePart(coupon.useEndTime)
  return start && end ? `${start} 至 ${end}` : '长期有效'
}

/** 只取日期部分，列表里不需要精确到时分秒 */
function formatDatePart(value: string): string {
  if (value === null || value === undefined || value === '') {
    return ''
  }
  return value.slice(0, 10)
}

async function onClaim(coupon: CouponDef): Promise<void> {
  claimingId.value = coupon.id
  try {
    await postClaimCoupon(coupon.id)
    ElMessage.success('领取成功，可在「我的优惠券」查看')
    await load()
  } catch {
    // 提示已由响应拦截器统一弹出
  } finally {
    claimingId.value = null
  }
}
</script>

<style lang="scss" scoped>
.coupon-center-page {
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
    min-width: 110px;
    color: var(--el-color-danger);

    em {
      font-size: 26px;
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

  &__remain {
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }
}
</style>
