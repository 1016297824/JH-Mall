<template>
  <div class="after-sale-page">
    <div class="after-sale-page__head">
      <h2 class="after-sale-page__title">售后</h2>
      <el-button type="primary" @click="openApply">申请售后</el-button>
    </div>

    <el-empty v-if="!loading && afterSales.length === 0" description="暂无售后记录" />

    <ul v-else v-loading="loading" class="sale-list">
      <li v-for="sale in afterSales" :key="sale.id" class="sale-card">
        <div class="sale-card__head">
          <span class="sale-card__no">售后单号：{{ sale.afterSaleNo }}</span>
          <span class="sale-card__order">订单号：{{ sale.orderNo }}</span>
          <el-tag class="sale-card__status" size="small">{{ sale.afterSaleStatusDesc }}</el-tag>
        </div>
        <div class="sale-card__body">
          <p class="sale-card__line">类型：{{ sale.afterSaleTypeDesc }}</p>
          <p class="sale-card__line">退款金额：{{ formatPrice(sale.amount) }} 元</p>
          <p class="sale-card__line">申请时间：{{ formatDateTime(sale.applyTime) }}</p>
          <p class="sale-card__line">退款原因：{{ sale.reason }}</p>
          <p v-if="sale.approveRemark" class="sale-card__line">
            审核意见：{{ sale.approveRemark }}
          </p>
          <p v-if="sale.returnExpressNo" class="sale-card__line">
            退货物流：{{ sale.returnExpressCompany }} {{ sale.returnExpressNo }}
          </p>
        </div>
      </li>
    </ul>

    <el-dialog v-model="applyVisible" title="申请售后" width="480px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="订单号" prop="orderNo">
          <el-input v-model="form.orderNo" placeholder="请输入需要退款的订单号" />
        </el-form-item>
        <el-form-item label="售后类型" prop="afterSaleType">
          <el-radio-group v-model="form.afterSaleType">
            <el-radio :value="AFTER_SALE_TYPE_REFUND_ONLY">仅退款</el-radio>
            <el-radio :value="AFTER_SALE_TYPE_RETURN">退货退款</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="退款原因" prop="reason">
          <el-input
            v-model="form.reason"
            type="textarea"
            :rows="2"
            maxlength="200"
            show-word-limit
            placeholder="请说明退款原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { getAfterSales, postAfterSale, type AfterSale, type SubmitAfterSaleParams } from '@/api/order'
import { formatDateTime, formatPrice } from '@/utils/common/format'

/** 售后类型码，与后端 AfterSaleTypeEnum 一致 */
const AFTER_SALE_TYPE_REFUND_ONLY = 1
const AFTER_SALE_TYPE_RETURN = 2

const route = useRoute()
const afterSales = ref<AfterSale[]>([])
const loading = ref(false)
const submitting = ref(false)
const applyVisible = ref(false)
const formRef = ref<FormInstance>()
const form = ref<SubmitAfterSaleParams>({
  orderNo: '',
  afterSaleType: AFTER_SALE_TYPE_REFUND_ONLY,
  reason: '',
})

const formRules: FormRules<SubmitAfterSaleParams> = {
  orderNo: [{ required: true, message: '请输入订单号', trigger: 'blur' }],
  afterSaleType: [{ required: true, message: '请选择售后类型', trigger: 'change' }],
  reason: [{ required: true, message: '请填写退款原因', trigger: 'blur' }],
}

onMounted(() => {
  // 从订单详情跳过来时带上订单号，直接定位到该单
  const orderNo = route.query.orderNo
  if (typeof orderNo === 'string' && orderNo !== '') {
    form.value.orderNo = orderNo
    applyVisible.value = true
  }
  void load()
})

async function load(): Promise<void> {
  loading.value = true
  try {
    const orderNo = typeof route.query.orderNo === 'string' ? route.query.orderNo : undefined
    afterSales.value = await getAfterSales(orderNo)
  } catch {
    // 提示已由响应拦截器统一弹出
    afterSales.value = []
  } finally {
    loading.value = false
  }
}

function openApply(): void {
  applyVisible.value = true
}

async function onSubmit(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (valid !== true) {
    return
  }
  submitting.value = true
  try {
    const afterSaleNo = await postAfterSale({ ...form.value })
    ElMessage.success(`售后申请已提交，单号 ${afterSaleNo}`)
    applyVisible.value = false
    form.value.reason = ''
    await load()
  } catch {
    // 提示已由响应拦截器统一弹出
  } finally {
    submitting.value = false
  }
}
</script>

<style lang="scss" scoped>
.after-sale-page {
  max-width: 880px;
  margin: 0 auto;
  padding: 16px;

  &__head {
    display: flex;
    align-items: center;
    margin-bottom: 12px;
  }

  &__title {
    margin: 0;
    font-size: 18px;
  }
}

.after-sale-page__head .el-button {
  margin-left: auto;
}

.sale-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.sale-card {
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

  &__order {
    color: var(--el-text-color-secondary);
  }

  &__status {
    margin-left: auto;
  }

  &__body {
    padding: 10px 12px;
  }

  &__line {
    margin: 4px 0;
    font-size: 13px;
    color: var(--el-text-color-regular);
  }
}
</style>
