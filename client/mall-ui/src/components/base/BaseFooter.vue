<script setup lang="ts">
const currentYear = new Date().getFullYear()
</script>

<template>
  <footer class="site-footer">
    <div class="footer-inner">
      <div class="footer-links">
        <a href="#">关于我们</a>
        <span class="divider">|</span>
        <a href="#">帮助中心</a>
        <span class="divider">|</span>
        <a href="#">隐私政策</a>
        <span class="divider">|</span>
        <a href="#">用户协议</a>
      </div>
      <p class="footer-copyright">&copy; {{ currentYear }} JH-Mall. All Rights Reserved.</p>
      <a
        class="footer-icp"
        href="https://beian.miit.gov.cn/"
        target="_blank"
        rel="noopener noreferrer"
      >京ICP备XXXXXXXX号</a>
    </div>
  </footer>
</template>

<style lang="scss" scoped>
@use '@/assets/styles/variables' as *;

.site-footer {
  background: #F8F9FA;
  border-top: 1px solid $color-border;
  padding: 32px $spacing-lg 24px;
}

.footer-inner {
  max-width: 1280px;
  margin: 0 auto;
  text-align: center;
}

.footer-links {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 0;
  margin-bottom: $spacing-md;

  a {
    font-size: 13px;
    color: var(--el-text-color-regular);
    text-decoration: none;
    padding: 0 12px;

    &:hover {
      color: $color-primary;
    }
  }
}

.divider {
  font-size: 12px;
  color: $color-border;
}

.footer-copyright {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: $spacing-xs;
}

.footer-icp {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  text-decoration: none;

  &:hover {
    color: $color-primary;
  }
}
</style>
