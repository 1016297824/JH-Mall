<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

import { loginByCaptcha } from '@/api/auth'
import { useAuthStore } from '@/stores/auth.store'
import { useCaptcha } from '@/composables/auth'
import { PHONE_PATTERN } from '@/utils/constants'
import type { LoginReq } from '@/types'

/** 登录表单（captchaKey 由 useCaptcha 统一管理） */
type LoginForm = Omit<LoginReq, 'captchaKey'>

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const formRef = ref<FormInstance>()
const submitting = ref(false)

const { captchaKey, imageSrc, hasImage, loading: captchaLoading, refresh } = useCaptcha()

const form = reactive<LoginForm>({
  phone: '',
  password: '',
  captchaCode: '',
})

const rules: FormRules<LoginForm> = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: PHONE_PATTERN, message: '手机号格式不正确', trigger: 'blur' },
  ],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
}

/** 登录成功后的回跳地址，仅接受站内单层路径（排除 `//host` 这类协议相对地址） */
const redirectPath = computed(() => {
  const target = route.query.redirect
  return typeof target === 'string' && /^\/[^/\\]/.test(target) ? target : '/'
})

/** 刷新验证码并清空已输入的验证码 */
async function onRefreshCaptcha(): Promise<void> {
  form.captchaCode = ''
  await refresh()
}

/** 提交登录 */
async function onSubmit(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const token = await loginByCaptcha({ ...form, captchaKey: captchaKey.value })
    authStore.setToken(token)
    ElMessage.success('登录成功')
    await router.replace(redirectPath.value)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '登录失败，请重试')
    await onRefreshCaptcha()
  } finally {
    submitting.value = false
  }
}

onMounted(onRefreshCaptcha)
</script>

<template>
  <div class="login-page">
    <div class="login-page__panel">
      <h1 class="login-page__title">欢迎回来</h1>
      <p class="login-page__subtitle">登录后可继续购物与查看订单</p>

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
            placeholder="请输入密码"
            show-password
          />
        </el-form-item>

        <el-form-item label="验证码" prop="captchaCode">
          <div class="login-page__captcha">
            <el-input v-model="form.captchaCode" placeholder="请输入验证码" maxlength="4" />
            <button
              type="button"
              class="login-page__captcha-image"
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
          class="login-page__submit"
          :loading="submitting"
          native-type="submit"
        >
          登录
        </el-button>
      </el-form>

      <div class="login-page__links">
        <router-link to="/register">注册新账号</router-link>
        <router-link to="/forgot">忘记密码</router-link>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
@use '@/assets/styles/variables' as *;
@use '@/assets/styles/mixins' as *;

.login-page {
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
    justify-content: space-between;
    margin-top: $spacing-lg;
    font-size: 13px;
  }
}
</style>
