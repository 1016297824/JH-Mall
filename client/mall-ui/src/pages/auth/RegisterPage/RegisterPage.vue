<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormItemRule, FormRules } from 'element-plus'

import { registerByCaptcha } from '@/api/auth'
import { useAuthStore } from '@/stores/auth.store'
import { useCaptcha } from '@/composables/auth'
import { PASSWORD_PATTERN, PHONE_PATTERN } from '@/utils/constants'
import type { RegisterReq } from '@/types'

/** 注册表单（captchaKey 由 useCaptcha 管理，confirmPassword 仅前端校验） */
interface RegisterForm extends Omit<RegisterReq, 'captchaKey'> {
  /** 确认密码 */
  confirmPassword: string
}

const router = useRouter()
const authStore = useAuthStore()

const formRef = ref<FormInstance>()
const submitting = ref(false)

const { captchaKey, imageSrc, hasImage, loading: captchaLoading, refresh } = useCaptcha()

const form = reactive<RegisterForm>({
  phone: '',
  password: '',
  confirmPassword: '',
  captchaCode: '',
  isPrivacyAgreed: false,
})

/** 校验两次输入的密码一致 */
const validateConfirm: FormItemRule['validator'] = (_rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
    return
  }
  callback()
}

/** 校验隐私协议已勾选 */
const validatePrivacy: FormItemRule['validator'] = (_rule, value, callback) => {
  if (value !== true) {
    callback(new Error('请先阅读并同意隐私协议'))
    return
  }
  callback()
}

const rules: FormRules<RegisterForm> = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: PHONE_PATTERN, message: '手机号格式不正确', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 32, message: '密码长度需在 8-32 个字符之间', trigger: 'blur' },
    { pattern: PASSWORD_PATTERN, message: '密码必须同时包含字母和数字', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' },
  ],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
  isPrivacyAgreed: [{ validator: validatePrivacy, trigger: 'change' }],
}

/** 刷新验证码并清空已输入的验证码 */
async function onRefreshCaptcha(): Promise<void> {
  form.captchaCode = ''
  await refresh()
}

/** 提交注册，成功后后端直接下发令牌 */
async function onSubmit(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const token = await registerByCaptcha({
      phone: form.phone,
      password: form.password,
      captchaKey: captchaKey.value,
      captchaCode: form.captchaCode,
      isPrivacyAgreed: form.isPrivacyAgreed,
    })
    authStore.setToken(token)
    ElMessage.success('注册成功，已自动登录')
    await router.replace('/')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '注册失败，请重试')
    await onRefreshCaptcha()
  } finally {
    submitting.value = false
  }
}

onMounted(onRefreshCaptcha)
</script>

<template>
  <div class="register-page">
    <div class="register-page__panel">
      <h1 class="register-page__title">创建账号</h1>
      <p class="register-page__subtitle">注册即刻开始购物</p>

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

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="8-32 位，须含字母和数字"
            show-password
          />
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            show-password
          />
        </el-form-item>

        <el-form-item label="验证码" prop="captchaCode">
          <div class="register-page__captcha">
            <el-input v-model="form.captchaCode" placeholder="请输入验证码" maxlength="4" />
            <button
              type="button"
              class="register-page__captcha-image"
              :disabled="captchaLoading"
              @click="onRefreshCaptcha"
            >
              <img v-if="hasImage" :src="imageSrc" alt="图形验证码" />
              <span v-else>加载中</span>
            </button>
          </div>
        </el-form-item>

        <el-form-item prop="isPrivacyAgreed">
          <el-checkbox v-model="form.isPrivacyAgreed">我已阅读并同意隐私协议</el-checkbox>
        </el-form-item>

        <el-button
          type="primary"
          class="register-page__submit"
          :loading="submitting"
          native-type="submit"
        >
          注册
        </el-button>
      </el-form>

      <div class="register-page__links">
        <span>已有账号？</span>
        <router-link to="/login">去登录</router-link>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
@use '@/assets/styles/variables' as *;
@use '@/assets/styles/mixins' as *;

.register-page {
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
