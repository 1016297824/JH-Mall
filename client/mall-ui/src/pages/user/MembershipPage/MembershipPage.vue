<template>
  <div v-loading="loading" class="membership-page">
    <section class="level-card">
      <div class="level-card__badge">
        <img v-if="levelIcon" class="level-card__icon" :src="levelIcon" :alt="levelName" />
        <p class="level-card__name">{{ levelName }}</p>
      </div>
      <div class="level-card__growth">
        <p class="level-card__growth-value">
          成长值 <em>{{ membership?.growth ?? 0 }}</em>
          <span v-if="membership?.totalGrowth">（累计 {{ membership.totalGrowth }}）</span>
        </p>
        <el-progress
          v-if="membership?.nextLevel"
          :percentage="progressPercent"
          :stroke-width="10"
        />
        <p class="level-card__hint">{{ progressHint }}</p>
      </div>
    </section>

    <section class="benefit-block">
      <h3 class="benefit-block__title">当前权益</h3>
      <el-empty v-if="benefits.length === 0" :image-size="60" description="暂无会员权益" />
      <ul v-else class="benefit-list">
        <li v-for="benefit in benefits" :key="benefit" class="benefit-item">
          <el-icon class="benefit-item__icon"><Select /></el-icon>
          <span>{{ benefit }}</span>
        </li>
      </ul>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Select } from '@element-plus/icons-vue'
import { getMembership, type Membership } from '@/api/user'

const membership = ref<Membership | null>(null)
const loading = ref(false)

const levelName = computed(() => membership.value?.currentLevel?.levelName ?? '普通会员')
const levelIcon = computed(() => membership.value?.currentLevel?.icon ?? '')
const benefits = computed(() => membership.value?.benefits ?? [])

/**
 * 距离下一级的进度百分比
 *
 * <p>后端只给成长值与等级值，没有「本级起点/下一级门槛」，故以等级值区间近似：
 * 无下一级时按 100% 展示。</p>
 */
const progressPercent = computed(() => {
  const current = membership.value
  if (!current?.nextLevel) {
    return 100
  }
  const nextValue = current.nextLevel.levelValue
  if (!nextValue || nextValue <= 0) {
    return 0
  }
  const ratio = (current.totalGrowth ?? 0) / nextValue
  return Math.min(100, Math.max(0, Math.round(ratio * 100)))
})

const progressHint = computed(() => {
  const current = membership.value
  if (!current) {
    return ''
  }
  if (!current.nextLevel) {
    return '已是最高等级'
  }
  return `再获得成长值可升级为「${current.nextLevel.levelName}」`
})

onMounted(() => {
  void load()
})

async function load(): Promise<void> {
  loading.value = true
  try {
    membership.value = await getMembership()
  } catch {
    // 提示已由响应拦截器统一弹出
    membership.value = null
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.membership-page {
  max-width: 720px;
  margin: 0 auto;
  padding: 16px;
  min-height: 200px;
}

.level-card {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 20px;
  border-radius: 8px;
  background: linear-gradient(135deg, #fdf6ec, #fde2e2);

  &__badge {
    text-align: center;
    min-width: 90px;
  }

  &__icon {
    width: 48px;
    height: 48px;
  }

  &__name {
    margin: 6px 0 0;
    font-size: 15px;
    font-weight: 600;
    color: #8a5a2b;
  }

  &__growth {
    flex: 1;
    min-width: 0;
  }

  &__growth-value {
    margin: 0 0 8px;
    font-size: 14px;

    em {
      font-style: normal;
      font-size: 20px;
      font-weight: 600;
      color: var(--el-color-danger);
    }

    span {
      font-size: 12px;
      color: var(--el-text-color-secondary);
    }
  }

  &__hint {
    margin: 8px 0 0;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }
}

.benefit-block {
  margin-top: 16px;

  &__title {
    margin: 0 0 12px;
    font-size: 15px;
  }
}

.benefit-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.benefit-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  font-size: 14px;
  border-bottom: 1px solid var(--el-border-color-lighter);

  &__icon {
    color: var(--el-color-success);
  }
}
</style>
