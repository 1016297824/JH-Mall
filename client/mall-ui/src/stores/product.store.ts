import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getCategoryTree } from '@/api/product'
import type { CategoryVO } from '@/types'

export const useProductStore = defineStore('product', () => {
  const categories = ref<CategoryVO[]>([])
  const loading = ref(false)

  async function fetchCategories() {
    if (categories.value.length > 0) return
    loading.value = true
    try {
      categories.value = await getCategoryTree()
    } finally {
      loading.value = false
    }
  }

  return { categories, loading, fetchCategories }
})
