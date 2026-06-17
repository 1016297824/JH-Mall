<script setup lang="ts">
import { computed } from 'vue'
import type { SpecGroupVO, SpecValueVO, SkuBriefVO } from '@/types'

const props = defineProps<{
  specGroups: SpecGroupVO[]
  skus: SkuBriefVO[]
  selectedSpecs: Record<string, string>
}>()

const emit = defineEmits<{
  select: [specName: string, value: string]
}>()

/** 是否有任意规格被选中 */
const hasSelection = computed(() => Object.keys(props.selectedSpecs).length > 0)

/** 是否已选完所有规格分组 */
const isSpecComplete = computed(() =>
  props.specGroups.every((g) => props.selectedSpecs[g.specName]),
)

function selectSpec(specName: string, val: SpecValueVO) {
  if (val.disabled) return
  emit('select', specName, val.value)
}

function specButtonClass(specName: string, val: SpecValueVO) {
  return {
    'sku-selector__value': true,
    'sku-selector__value--selected': props.selectedSpecs[specName] === val.value,
    'sku-selector__value--disabled': val.disabled,
  }
}
</script>

<template>
  <div class="sku-selector">
    <div v-for="group in specGroups" :key="group.specName" class="sku-selector__group">
      <span class="sku-selector__label">{{ group.specName }}</span>
      <div class="sku-selector__values" role="radiogroup" :aria-label="group.specName">
        <button
          v-for="val in group.values"
          :key="val.value"
          :class="specButtonClass(group.specName, val)"
          :disabled="val.disabled"
          :aria-checked="props.selectedSpecs[group.specName] === val.value"
          role="radio"
          @click="selectSpec(group.specName, val)"
        >
          {{ val.value }}
        </button>
      </div>
    </div>

    <div v-if="!isSpecComplete && hasSelection" class="sku-selector__hint">请选择完整规格</div>
  </div>
</template>

<style lang="scss" scoped>
@use '@/assets/styles/variables' as v;

.sku-selector {
  display: flex;
  flex-direction: column;
  gap: v.$spacing-md;
}

.sku-selector__group {
  display: flex;
  flex-direction: column;
  gap: v.$spacing-sm;
}

.sku-selector__label {
  font-family: v.$font-body;
  font-size: 13px;
  font-weight: 600;
  color: v.$color-text-regular;
}

.sku-selector__values {
  display: flex;
  flex-wrap: wrap;
  gap: v.$spacing-sm;
}

.sku-selector__value {
  min-width: 48px;
  height: 36px;
  padding: 0 v.$spacing-md;
  border: 1px solid v.$color-border;
  border-radius: v.$radius-md;
  background: #fff;
  font-family: v.$font-body;
  font-size: 13px;
  color: v.$color-text-primary;
  cursor: pointer;
  transition:
    border-color v.$duration-normal v.$ease-default,
    background-color v.$duration-normal v.$ease-default,
    transform v.$duration-fast v.$ease-default;

  &:hover:not(.sku-selector__value--disabled) {
    border-color: v.$color-primary;
  }

  &:active:not(.sku-selector__value--disabled) {
    transform: scale(0.97);
  }

  &--selected {
    border-color: v.$color-primary;
    background: v.$color-bg;
    color: v.$color-primary;
    font-weight: 600;
  }

  &--disabled {
    border-color: #e2e8f0;
    background: #eef2f5;
    color: #7a94a6;
    cursor: not-allowed;
  }
}

.sku-selector__hint {
  font-size: 12px;
  color: v.$color-text-secondary;
  padding: v.$spacing-sm 0;
}
</style>
