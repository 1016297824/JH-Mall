import { computed, ref, type ComputedRef, type Ref } from 'vue'
import { ElMessage } from 'element-plus'

import { getCaptcha } from '@/api/auth'

/** useCaptcha 返回值 */
export interface UseCaptchaReturn {
  /** 验证码 Key，提交表单时回传后端 */
  captchaKey: Ref<string>
  /** 是否正在加载验证码图片 */
  loading: Ref<boolean>
  /** 可直接用于 img src 的图片地址 */
  imageSrc: ComputedRef<string>
  /** 是否已成功加载图片 */
  hasImage: ComputedRef<boolean>
  /** 重新获取验证码 */
  refresh: () => Promise<void>
}

/**
 * 图形验证码字段逻辑
 *
 * <p>后端验证码为一次性消费，登录/注册/重置密码失败后必须重新获取，
 * 因此把加载、刷新与图片地址兼容集中在此，避免各页面重复实现。</p>
 *
 * @returns 验证码状态与刷新方法
 */
export function useCaptcha(): UseCaptchaReturn {
  const captchaKey = ref('')
  const image = ref('')
  const loading = ref(false)

  // 后端返回的已是 data URI，此处兼容纯 base64 的情况
  const imageSrc = computed(() =>
    image.value.startsWith('data:') ? image.value : `data:image/png;base64,${image.value}`,
  )
  const hasImage = computed(() => image.value !== '')

  /** 拉取新的图形验证码 */
  async function refresh(): Promise<void> {
    loading.value = true
    try {
      const captcha = await getCaptcha()
      captchaKey.value = captcha.captchaKey
      image.value = captcha.captchaImage
    } catch {
      image.value = ''
      ElMessage.error('验证码加载失败，请点击图片重试')
    } finally {
      loading.value = false
    }
  }

  return { captchaKey, loading, imageSrc, hasImage, refresh }
}
