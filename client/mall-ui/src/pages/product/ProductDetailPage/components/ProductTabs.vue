<template>
  <section class="detail-tabs-section">
    <div class="tab-header">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        class="tab-btn"
        :class="{ 'tab-btn--active': modelValue === tab.key }"
        @click="emit('update:modelValue', tab.key)"
      >
        {{ tab.label }}
      </button>
    </div>
    <div class="tab-content">
      <div v-if="modelValue === 'specs'" class="tab-panel">
        <dl class="spec-list">
          <template v-for="group in specGroups" :key="group.specName">
            <dt>{{ group.specName }}</dt>
            <dd>{{ joinSpecs(group.values) }}</dd>
          </template>
          <dt>品牌</dt>
          <dd>{{ brandName || 'JH-Mall' }}</dd>
          <dt>货号</dt>
          <dd>{{ skuCode || '—' }}</dd>
        </dl>
      </div>

      <div v-else-if="modelValue === 'reviews'" class="tab-panel">
        <div class="empty-tab">
          <p>暂无评价，成为第一个评价的人吧</p>
        </div>
      </div>

      <div v-else-if="modelValue === 'afterSale'" class="tab-panel">
        <ul class="after-sale-list">
          <li>支持 7 天无理由退货（商品完好、不影响二次销售）</li>
          <li>签收 15 天内如有质量问题可申请换货</li>
          <li>未发货订单可随时取消，已发货订单需签收后退货</li>
          <li>退款将在收到退货商品后 3 个工作日内原路返还</li>
        </ul>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { SpecGroupVO } from '@/types'

defineProps<{
  modelValue: 'specs' | 'reviews' | 'afterSale'
  specGroups: SpecGroupVO[]
  brandName: string
  skuCode: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: 'specs' | 'reviews' | 'afterSale']
}>()

const tabs = [
  { key: 'specs' as const, label: '商品参数' },
  { key: 'reviews' as const, label: '用户评价' },
  { key: 'afterSale' as const, label: '售后说明' },
]

function joinSpecs(values: { value: string }[]): string {
  return values.map((v) => v.value).join('、')
}
</script>

<style lang="scss" scoped>
@use '@/assets/styles/variables' as v;

.detail-tabs-section {
  grid-area: tabs;
  background: #fff;
  border-radius: v.$radius-lg;
  box-shadow: v.$shadow-sm;
  overflow: hidden;
}

.tab-header {
  display: flex;
  border-bottom: 1px solid v.$color-border;
}

.tab-btn {
  flex: 1;
  padding: v.$spacing-md v.$spacing-lg;
  border: none;
  background: transparent;
  font-size: 15px;
  font-weight: 500;
  font-family: v.$font-body;
  color: v.$color-text-secondary;
  cursor: pointer;
  position: relative;
  transition: color v.$duration-normal v.$ease-default;

  &::after {
    content: '';
    position: absolute;
    bottom: 0;
    left: 50%;
    transform: translateX(-50%) scaleX(0);
    width: 40px;
    height: 3px;
    background: v.$color-primary;
    border-radius: 2px;
    transition: transform v.$duration-normal v.$ease-default;
  }

  &:hover {
    color: v.$color-text-primary;
  }

  &--active {
    color: v.$color-primary;
    font-weight: 600;

    &::after {
      transform: translateX(-50%) scaleX(1);
    }
  }
}

.tab-content {
  padding: v.$spacing-xl;
  min-height: 200px;
}

.tab-panel {
  animation: fadeIn v.$duration-slow v.$ease-default;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.spec-list {
  display: grid;
  grid-template-columns: 100px 1fr;
  gap: 0;

  dt,
  dd {
    padding: v.$spacing-sm v.$spacing-md;
    border-bottom: 1px solid v.$color-bg;
    margin: 0;
  }

  dt {
    font-size: 13px;
    color: v.$color-text-secondary;
  }

  dd {
    font-size: 13px;
    color: v.$color-text-primary;
  }
}

.after-sale-list {
  list-style: none;
  padding: 0;
  margin: 0;

  li {
    position: relative;
    padding: v.$spacing-sm 0 v.$spacing-sm v.$spacing-lg;
    font-size: 14px;
    color: v.$color-text-regular;
    line-height: 1.7;

    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 13px;
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background: v.$color-primary-light;
    }
  }
}

.empty-tab {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 160px;
  color: v.$color-text-secondary;
  font-size: 14px;
}

@media (max-width: 767px) {
  .tab-content {
    padding: v.$spacing-md;
  }
}
</style>
