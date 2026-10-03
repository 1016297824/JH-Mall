<template>
  <div class="points-page">
    <h2 class="points-page__title">我的积分</h2>

    <div v-loading="loadingAccount" class="points-summary">
      <div class="points-summary__item points-summary__item--main">
        <p class="points-summary__label">可用积分</p>
        <p class="points-summary__value">{{ account?.availablePoints ?? 0 }}</p>
      </div>
      <div class="points-summary__item">
        <p class="points-summary__label">累计积分</p>
        <p class="points-summary__value">{{ account?.totalPoints ?? 0 }}</p>
      </div>
      <div class="points-summary__item">
        <p class="points-summary__label">已使用</p>
        <p class="points-summary__value">{{ account?.usedPoints ?? 0 }}</p>
      </div>
      <div class="points-summary__item">
        <p class="points-summary__label">已过期</p>
        <p class="points-summary__value">{{ account?.expiredPoints ?? 0 }}</p>
      </div>
    </div>

    <h3 class="points-page__subtitle">积分明细</h3>

    <el-empty v-if="!loadingRecords && records.length === 0" description="暂无积分流水" />

    <ul v-else v-loading="loadingRecords" class="record-list">
      <li v-for="record in records" :key="record.id" class="record-item">
        <div class="record-item__main">
          <p class="record-item__biz">{{ record.bizTypeName || record.bizType }}</p>
          <p class="record-item__time">{{ formatDateTime(record.createTime) }}</p>
          <p v-if="record.remark" class="record-item__remark">{{ record.remark }}</p>
        </div>
        <div class="record-item__amount">
          <span :class="changeClass(record.changeType)">{{ changeText(record) }}</span>
          <span class="record-item__balance">余额 {{ record.afterPoints }}</span>
        </div>
      </li>
    </ul>

    <div v-if="total > 0" class="points-page__pager">
      <el-pagination
        layout="prev, pager, next, total"
        :total="total"
        :page-size="POINTS_PAGE_SIZE"
        :current-page="page"
        @current-change="onPageChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getPoints, getPointsRecords, type PointsAccount, type PointsRecord } from '@/api/user'
import { POINTS_CHANGE_TYPE, POINTS_PAGE_SIZE } from '@/utils/constants'
import { formatDateTime } from '@/utils/common/format'

const account = ref<PointsAccount | null>(null)
const records = ref<PointsRecord[]>([])
const total = ref(0)
const page = ref(1)
const loadingAccount = ref(false)
const loadingRecords = ref(false)

onMounted(() => {
  void loadAccount()
  void loadRecords()
})

async function loadAccount(): Promise<void> {
  loadingAccount.value = true
  try {
    account.value = await getPoints()
  } catch {
    // 提示已由响应拦截器统一弹出
    account.value = null
  } finally {
    loadingAccount.value = false
  }
}

async function loadRecords(): Promise<void> {
  loadingRecords.value = true
  try {
    const result = await getPointsRecords(page.value, POINTS_PAGE_SIZE)
    records.value = result.records
    total.value = result.total
  } catch {
    // 提示已由响应拦截器统一弹出
    records.value = []
    total.value = 0
  } finally {
    loadingRecords.value = false
  }
}

function onPageChange(next: number): void {
  page.value = next
  void loadRecords()
}

/** 收入显示 +N，支出显示 -N */
function changeText(record: PointsRecord): string {
  const sign = record.changeType === POINTS_CHANGE_TYPE.INCOME ? '+' : '-'
  return `${sign}${record.points}`
}

function changeClass(changeType: number): string {
  return changeType === POINTS_CHANGE_TYPE.INCOME
    ? 'points-change points-change--income'
    : 'points-change points-change--expense'
}
</script>

<style lang="scss" scoped>
.points-page {
  max-width: 880px;
  margin: 0 auto;
  padding: 16px;

  &__title {
    margin: 0 0 12px;
    font-size: 18px;
  }

  &__subtitle {
    margin: 20px 0 12px;
    font-size: 15px;
  }

  &__pager {
    display: flex;
    justify-content: flex-end;
    padding: 12px 0;
  }
}

.points-summary {
  display: flex;
  gap: 12px;
  min-height: 80px;

  &__item {
    flex: 1;
    padding: 12px;
    text-align: center;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 6px;

    &--main {
      border-color: var(--el-color-primary-light-5);
      background: var(--el-color-primary-light-9);
    }
  }

  &__label {
    margin: 0;
    font-size: 13px;
    color: var(--el-text-color-secondary);
  }

  &__value {
    margin: 8px 0 0;
    font-size: 22px;
    font-weight: 600;
  }
}

:deep(.points-change) {
  font-weight: 600;
}

:deep(.points-change--income) {
  color: var(--el-color-success);
}

:deep(.points-change--expense) {
  color: var(--el-color-danger);
}

.record-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.record-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);

  &__main {
    flex: 1;
    min-width: 0;
  }

  &__biz {
    margin: 0;
    font-size: 14px;
  }

  &__time,
  &__remark {
    margin: 4px 0 0;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }

  &__amount {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 4px;
  }

  &__balance {
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }
}
</style>
