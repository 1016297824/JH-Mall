<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'

import { getProfile } from '@/api/user'
import { useAuth } from '@/composables/auth'
import type { UserProfile } from '@/types'

const { logout } = useAuth()

const profile = ref<UserProfile | null>(null)
const loading = ref(false)
const loggingOut = ref(false)

/** 昵称为空时回退到手机号，避免个人信息区空缺 */
const displayName = computed(() => profile.value?.nickname || profile.value?.phone || '商城用户')

/** 拉取当前登录用户资料 */
async function loadProfile(): Promise<void> {
  loading.value = true
  try {
    profile.value = await getProfile()
  } catch {
    profile.value = null
  } finally {
    loading.value = false
  }
}

/** 登出：useAuth 负责清除本地令牌并跳转登录页 */
async function onLogout(): Promise<void> {
  loggingOut.value = true
  try {
    await logout()
    ElMessage.success('已退出登录')
  } finally {
    loggingOut.value = false
  }
}

onMounted(loadProfile)
</script>

<template>
  <div class="user-center">
    <div v-if="loading" class="user-center__hint">加载中…</div>

    <template v-else-if="profile">
      <div class="user-center__card">
        <img
          v-if="profile.avatar"
          class="user-center__avatar"
          :src="profile.avatar"
          alt="用户头像"
        />
        <div v-else class="user-center__avatar user-center__avatar--empty">暂无头像</div>

        <div class="user-center__info">
          <h2 class="user-center__name">{{ displayName }}</h2>
          <p class="user-center__level">{{ profile.membershipLevel || '普通会员' }}</p>
        </div>
      </div>

      <dl class="user-center__stats">
        <div class="user-center__stat">
          <dt class="user-center__stat-label">可用积分</dt>
          <dd class="user-center__stat-value">{{ profile.availablePoints ?? 0 }}</dd>
        </div>
        <div class="user-center__stat">
          <dt class="user-center__stat-label">当前成长值</dt>
          <dd class="user-center__stat-value">{{ profile.growth ?? 0 }}</dd>
        </div>
        <div class="user-center__stat">
          <dt class="user-center__stat-label">手机号</dt>
          <dd class="user-center__stat-value">{{ profile.phone || '—' }}</dd>
        </div>
      </dl>

      <el-button class="user-center__logout" :loading="loggingOut" @click="onLogout">
        退出登录
      </el-button>
    </template>

    <div v-else class="user-center__hint">
      <p>资料加载失败，请稍后重试</p>
      <el-button type="primary" @click="loadProfile">重新加载</el-button>
    </div>
  </div>
</template>

<style lang="scss" scoped>
@use '@/assets/styles/variables' as *;

.user-center {
  padding: $spacing-lg;

  &__hint {
    padding: $spacing-3xl 0;
    text-align: center;
    color: $color-text-secondary;
  }

  &__card {
    display: flex;
    align-items: center;
    gap: $spacing-md;
    padding: $spacing-lg;
    background: #FFF;
    border: 1px solid $color-border;
    border-radius: $radius-lg;
    box-shadow: $shadow-sm;
  }

  &__avatar {
    width: 64px;
    height: 64px;
    border-radius: $radius-full;
    object-fit: cover;

    &--empty {
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 12px;
      color: #FFF;
      background: $color-primary-light;
    }
  }

  &__name {
    font-size: 18px;
    color: $color-text-primary;
  }

  &__level {
    margin-top: $spacing-xs;
    font-size: 13px;
    color: $color-text-secondary;
  }

  &__stats {
    display: flex;
    margin-top: $spacing-md;
    background: #FFF;
    border: 1px solid $color-border;
    border-radius: $radius-lg;
  }

  &__stat {
    flex: 1;
    padding: $spacing-md 0;
    text-align: center;

    & + & {
      border-left: 1px solid $color-border;
    }
  }

  &__stat-label {
    font-size: 12px;
    color: $color-text-secondary;
  }

  &__stat-value {
    margin-top: $spacing-xs;
    font-size: 16px;
    font-weight: 600;
    color: $color-text-primary;
  }

  &__logout {
    width: 100%;
    margin-top: $spacing-xl;
  }
}
</style>
