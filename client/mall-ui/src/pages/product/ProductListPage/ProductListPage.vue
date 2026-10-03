<template>
  <div class="product-list-page">
    <h2 class="product-list-page__title">{{ pageTitle }}</h2>

    <el-empty v-if="!loading && products.length === 0" description="该分类下暂无商品">
      <el-button type="primary" @click="goHome">回到首页</el-button>
    </el-empty>

    <ul v-else v-loading="loading" class="product-grid">
      <li
        v-for="product in products"
        :key="product.spuId"
        class="product-card"
        @click="goDetail(product.spuId)"
      >
        <img class="product-card__image" :src="product.mainImage" :alt="product.spuName" />
        <p class="product-card__name">{{ product.spuName }}</p>
        <p class="product-card__price">{{ priceText(product) }}</p>
        <p class="product-card__sales">已售 {{ product.salesCount }}</p>
      </li>
    </ul>

    <div v-if="total > 0" class="product-list-page__pager">
      <el-pagination
        layout="prev, pager, next, total"
        :total="total"
        :page-size="PAGE_SIZE"
        :current-page="page"
        @current-change="onPageChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getSpuList } from '@/api/product'
import { formatPrice } from '@/utils/common/format'
import type { SpuVO } from '@/types'

/** 单页条数，与后端 size 上限解耦（后端自行截断） */
const PAGE_SIZE = 20

const route = useRoute()
const router = useRouter()

const products = ref<SpuVO[]>([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)

const categoryId = computed(() => String(route.params.id ?? ''))
const pageTitle = computed(() => (categoryId.value === '' ? '全部商品' : '分类商品'))

onMounted(() => {
  void load()
})

// 同页切换分类时路由参数变化但组件复用，必须重新拉取
watch(categoryId, () => {
  page.value = 1
  void load()
})

async function load(): Promise<void> {
  loading.value = true
  try {
    const result = await getSpuList({
      page: page.value,
      size: PAGE_SIZE,
      categoryId: categoryId.value === '' ? undefined : Number(categoryId.value),
    })
    products.value = result.rows
    total.value = result.total
  } catch {
    // 提示已由响应拦截器统一弹出
    products.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function onPageChange(next: number): void {
  page.value = next
  void load()
}

/** 价格区间：最低价与最高价不同时展示区间 */
function priceText(product: SpuVO): string {
  if (product.priceMin === product.priceMax) {
    return `¥${formatPrice(product.priceMin)}`
  }
  return `¥${formatPrice(product.priceMin)} - ${formatPrice(product.priceMax)}`
}

function goDetail(spuId: string): void {
  void router.push({ path: `/products/${spuId}` })
}

function goHome(): void {
  void router.push({ path: '/' })
}
</script>

<style lang="scss" scoped>
.product-list-page {
  max-width: 1080px;
  margin: 0 auto;
  padding: 16px;

  &__title {
    margin: 0 0 12px;
    font-size: 18px;
  }

  &__pager {
    display: flex;
    justify-content: center;
    padding: 16px 0;
  }
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.product-card {
  padding: 8px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  cursor: pointer;

  &:hover {
    border-color: var(--el-color-primary-light-5);
  }

  &__image {
    width: 100%;
    aspect-ratio: 1;
    object-fit: cover;
    border-radius: 4px;
  }

  &__name {
    margin: 8px 0 4px;
    font-size: 13px;
    line-height: 1.4;
    height: 36px;
    overflow: hidden;
  }

  &__price {
    margin: 0;
    font-size: 15px;
    font-weight: 600;
    color: var(--el-color-danger);
  }

  &__sales {
    margin: 4px 0 0;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }
}
</style>
