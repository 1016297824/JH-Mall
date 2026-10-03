<template>
  <div class="category-page">
    <h2 class="category-page__title">全部分类</h2>

    <el-empty v-if="!loading && categories.length === 0" description="暂无分类" />

    <div v-else v-loading="loading" class="category-list">
      <section v-for="group in categories" :key="group.categoryId" class="category-group">
        <h3 class="category-group__name">{{ group.name }}</h3>
        <ul v-if="group.children && group.children.length > 0" class="category-group__items">
          <li v-for="child in group.children" :key="child.categoryId">
            <button type="button" class="category-chip" @click="goCategory(child.categoryId)">
              {{ child.name }}
            </button>
          </li>
        </ul>
        <p v-else class="category-group__empty">该分类下暂无子分类</p>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getCategoryTree } from '@/api/product'
import type { CategoryVO } from '@/types'

const router = useRouter()
const categories = ref<CategoryVO[]>([])
const loading = ref(false)

onMounted(() => {
  void load()
})

async function load(): Promise<void> {
  loading.value = true
  try {
    categories.value = await getCategoryTree()
  } catch {
    // 提示已由响应拦截器统一弹出
    categories.value = []
  } finally {
    loading.value = false
  }
}

function goCategory(categoryId: string): void {
  void router.push({ path: `/categories/${categoryId}` })
}
</script>

<style lang="scss" scoped>
.category-page {
  max-width: 880px;
  margin: 0 auto;
  padding: 16px;

  &__title {
    margin: 0 0 12px;
    font-size: 18px;
  }
}

.category-group {
  margin-bottom: 20px;

  &__name {
    margin: 0 0 10px;
    font-size: 15px;
    font-weight: 600;
  }

  &__items {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    margin: 0;
    padding: 0;
    list-style: none;
  }

  &__empty {
    margin: 0;
    font-size: 13px;
    color: var(--el-text-color-secondary);
  }
}

.category-chip {
  padding: 8px 16px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 16px;
  background: transparent;
  font-size: 13px;
  color: var(--el-text-color-primary);
  cursor: pointer;

  &:hover {
    border-color: var(--el-color-primary);
    color: var(--el-color-primary);
  }
}
</style>
