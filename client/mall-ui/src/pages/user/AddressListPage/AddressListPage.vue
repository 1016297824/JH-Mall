<template>
  <div class="address-page">
    <div class="address-page__head">
      <h2 class="address-page__title">收货地址</h2>
      <el-button type="primary" @click="openCreate">新增地址</el-button>
    </div>

    <el-empty v-if="!loading && addresses.length === 0" :image-size="80" description="还没有收货地址" />

    <ul v-else v-loading="loading" class="address-list">
      <li v-for="addr in addresses" :key="addr.addressId" class="address-card">
        <div class="address-card__head">
          <span class="address-card__name">{{ addr.receiverName }}</span>
          <span class="address-card__phone">{{ addr.receiverPhone }}</span>
          <el-tag v-if="addr.isDefault" size="small" type="danger">默认</el-tag>
          <el-tag v-else-if="addr.label" size="small">{{ addr.label }}</el-tag>
        </div>
        <p class="address-card__detail">{{ fullAddress(addr) }}</p>
        <div class="address-card__ops">
          <el-button v-if="!addr.isDefault" link type="primary" @click="onSetDefault(addr)">
            设为默认
          </el-button>
          <el-button link type="primary" @click="openEdit(addr)">编辑</el-button>
          <el-button link type="danger" @click="onDelete(addr)">删除</el-button>
        </div>
      </li>
    </ul>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item label="收货人" prop="receiverName">
          <el-input v-model="form.receiverName" maxlength="20" placeholder="请输入收货人姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="receiverPhone">
          <el-input v-model="form.receiverPhone" maxlength="11" placeholder="请输入 11 位手机号" />
        </el-form-item>
        <el-form-item label="省市区" prop="province">
          <div class="address-page__region">
            <el-input v-model="form.province" maxlength="20" placeholder="省" />
            <el-input v-model="form.city" maxlength="20" placeholder="市" />
            <el-input v-model="form.district" maxlength="20" placeholder="区/县" />
          </div>
        </el-form-item>
        <el-form-item label="详细地址" prop="detailAddress">
          <el-input v-model="form.detailAddress" maxlength="100" placeholder="街道、门牌号等" />
        </el-form-item>
        <el-form-item label="邮编" prop="zipCode">
          <el-input v-model="form.zipCode" maxlength="6" placeholder="选填" />
        </el-form-item>
        <el-form-item label="标签" prop="label">
          <el-input v-model="form.label" maxlength="10" placeholder="选填，如「家」「公司」" />
        </el-form-item>
        <el-form-item label="默认地址">
          <el-switch v-model="form.isDefault" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  deleteAddress,
  getAddresses,
  postAddress,
  putAddress,
  putDefaultAddress,
  type UserAddress,
} from '@/api/user'
import { PHONE_PATTERN } from '@/utils/constants'

/** 列表中的地址必然已由后端落库，addressId 不为空 */
interface StoredAddress extends UserAddress {
  addressId: string
}

/** 表单初始值；addressId 为 null 表示新增 */
function emptyForm(): UserAddress {
  return {
    addressId: null,
    receiverName: '',
    receiverPhone: '',
    province: '',
    city: '',
    district: '',
    detailAddress: '',
    zipCode: null,
    isDefault: false,
    label: null,
  }
}

const addresses = ref<StoredAddress[]>([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref<string | null>(null)
const formRef = ref<FormInstance>()
const form = ref<UserAddress>(emptyForm())

/** 手机号校验与后端保持一致，规则集中在本文件以免各表单各写一套 */
const formRules: FormRules<UserAddress> = {
  receiverName: [{ required: true, message: '请输入收货人姓名', trigger: 'blur' }],
  receiverPhone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: PHONE_PATTERN, message: '手机号格式不正确', trigger: 'blur' },
  ],
  province: [{ required: true, message: '请填写省市区', trigger: 'blur' }],
  detailAddress: [{ required: true, message: '请填写详细地址', trigger: 'blur' }],
}

const dialogTitle = ref('新增地址')

onMounted(() => {
  void load()
})

async function load(): Promise<void> {
  loading.value = true
  try {
    const list = await getAddresses()
    addresses.value = list.filter((item): item is StoredAddress => item.addressId !== null)
  } catch {
    // 提示已由响应拦截器统一弹出
    addresses.value = []
  } finally {
    loading.value = false
  }
}

function fullAddress(addr: UserAddress): string {
  return `${addr.province}${addr.city}${addr.district}${addr.detailAddress}`
}

/** 空字符串在提交前转成 null，避免后端把 "" 当成有效值存库 */
function normalize(formData: UserAddress): UserAddress {
  return {
    ...formData,
    zipCode: formData.zipCode === '' ? null : formData.zipCode,
    label: formData.label === '' ? null : formData.label,
  }
}

function openCreate(): void {
  editingId.value = null
  dialogTitle.value = '新增地址'
  form.value = emptyForm()
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

function openEdit(addr: StoredAddress): void {
  editingId.value = addr.addressId
  dialogTitle.value = '编辑地址'
  form.value = { ...addr }
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

async function onSubmit(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (valid !== true) {
    return
  }
  saving.value = true
  try {
    const payload = normalize(form.value)
    if (editingId.value === null) {
      await postAddress(payload)
    } else {
      await putAddress(editingId.value, payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await load()
  } catch {
    // 提示已由响应拦截器统一弹出
  } finally {
    saving.value = false
  }
}

async function onSetDefault(addr: StoredAddress): Promise<void> {
  try {
    await putDefaultAddress(addr.addressId)
    await load()
  } catch {
    // 提示已由响应拦截器统一弹出
  }
}

async function onDelete(addr: StoredAddress): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定删除「${addr.receiverName}」的地址？`, '删除地址', {
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    await deleteAddress(addr.addressId)
    ElMessage.success('已删除')
    await load()
  } catch {
    // 提示已由响应拦截器统一弹出
  }
}
</script>

<style lang="scss" scoped>
.address-page {
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
    font-size: 16px;
    font-weight: 600;
  }

  &__region {
    display: flex;
    gap: 8px;
    width: 100%;
  }
}

.address-page__head .el-button {
  margin-left: auto;
}

.address-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.address-card {
  margin-bottom: 12px;
  padding: 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;

  &__head {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  &__name {
    font-weight: 600;
  }

  &__phone {
    color: var(--el-text-color-regular);
  }

  &__detail {
    margin: 8px 0;
    font-size: 13px;
    color: var(--el-text-color-regular);
  }

  &__ops {
    display: flex;
    justify-content: flex-end;
  }
}
</style>
