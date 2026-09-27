<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormItemRule, FormRules } from 'element-plus'

import { resetPasswordByCaptcha } from '@/api/auth'
import { useAuthStore } from '@/stores/auth.store'
import { useCaptcha } from '@/composables/auth'
import { LOGIN_PATH, PASSWORD_PATTERN, PHONE_PATTERN } from '@/utils/constants'
import type { ResetPasswordReq } from '@/types'

/** 重置密码表单（captchaKey 由 useCaptcha 管理，confirmPassword 仅前端校验） */
interface ResetForm extends Omit<ResetPasswordReq, 'captchaKey'> {
  /** 确认新密码 */
  confirmPassword: string
}

const router = useRouter()
const authStore = useAuthStore()

const formRef = ref<FormInstance>()
const submitting = ref(false)

const { captchaKey, imageSrc, hasImage, loading: captchaLoading, refresh } = useCaptcha()

const form = reactive<ResetForm>({
  phone: '',
  newPassword: '',
  confirmPassword: '',
  captchaCode: '',
})

/** 校验两次输入的密码一致 */
const validateConfirm: FormItemRule['validator'] = (_rule, value, callback) => {
  if (value !== form.newPassword) {
    callback(new Error('两次输入的密码不一致'))
    return
  }
  callback()
}

const rules: FormRules<ResetForm> = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: PHONE_PATTERN, message: '手机号格式不正确', trigger: 'blur' },
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, max: 32, message: '密码长度需在 8-32 个字符之间', trigger: 'blur' },
    { pattern: PASSWORD_PATTERN, message: '密码必须同时包含字母和数字', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' },
  ],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
}

/** 刷新验证码并清空已输入的验证码 */
async function onRefreshCaptcha(): Promise<void> {
  form.captchaCode = ''
  await refresh()
}

/** 提交重置密码；后端会作废该用户全部令牌，需重新登录 */
async function onSubmit(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await resetPasswordByCaptcha({
      phone: form.phone,
      newPassword: form.newPassword,
      captchaKey: captchaKey.value,
      captchaCode: form.captchaCode,
    })
    // 后端已作废该用户全部令牌，本地一并清理，避免残留失效登录态
    authStore.clearAuth()
    ElMessage.success('密码已重置，请使用新密码登录')
    await router.replace(LOGIN_PATH)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '重置失败，请重试')
    await onRefreshCaptcha()
  } finally {
    submitting.value = false
  }
}

onMounted(onRefreshCaptcha)
</script>

<template>
  <div class="forgot-page">
    <div class="forgot-page__panel">
      <h1 class="forgot-page__title">重置密码</h1>
      <p class="forgot-page__subtitle">验证手机号后设置新密码</p>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="onSubmit"
      >
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="11" clearable />
        </el-form-item>

        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="form.newPassword"
            type="password"
            placeholder="8-32 位，须含字母和数字"
            show-password
          />
        </el-form-item>

        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            show-password
          />
        </el-form-item>

        <el-form-item label="验证码" prop="captchaCode">
          <div class="forgot-page__captcha">
            <el-input v-model="form.captchaCode" placeholder="请输入验证码" maxlength="4" />
            <button
              type="button"
              class="forgot-page__captcha-image"
              :disabled="captchaLoading"
              @click="onRefreshCaptcha"
            >
              <img v-if="hasImage" :src="imageSrc" alt="图形验证码" />
              <span v-else>加载中</span>
            </button>
          </div>
        </el-form-item>

        <el-button
          type="primary"
          class="forgot-page__submit"
          :loading="submitting"
          native-type="submit"
        >
          重置密码
        </el-button>
      </el-form>

      <div class="forgot-page__links">
        <span>想起密码了？</span>
        <router-link to="/login">去登录</router-link>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
@use '@/assets/styles/variables' as *;
@use '@/assets/styles/mixins' as *;

.forgot-page {
  @include flex-center;
  min-height: 100vh;
  padding: $spacing-lg;
  background: $color-bg;

  &__panel {
    width: 100%;
    max-width: 400px;
    padding: $spacing-xl;
    background: #FFF;
    border: 1px solid $color-border;
    border-radius: $radius-lg;
    box-shadow: $shadow-md;
  }

  &__title {
    font-size: 24px;
    color: $color-text-primary;
  }

  &__subtitle {
    margin: $spacing-xs 0 $spacing-lg;
    font-size: 13px;
    color: $color-text-secondary;
  }

  &__captcha {
    display: flex;
    gap: $spacing-sm;
    width: 100%;
  }

  &__captcha-image {
    flex: 0 0 110px;
    height: 32px;
    padding: 0;
    overflow: hidden;
    background: #FFF;
    border: 1px solid $color-border;
    border-radius: $radius-sm;
    cursor: pointer;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }

  &__submit {
    width: 100%;
    margin-top: $spacing-sm;
  }

  &__links {
    display: flex;
    gap: $spacing-xs;
    justify-content: flex-end;
    margin-top: $spacing-lg;
    font-size: 13px;
  }
}
</style>
