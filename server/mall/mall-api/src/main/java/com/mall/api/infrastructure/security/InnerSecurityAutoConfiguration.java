package com.mall.api.infrastructure.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 内部接口签名自动装配
 *
 * <p>{@link InnerSignatureFilter} 与 {@link InnerFeignRequestInterceptor} 虽带
 * {@code @Component}，但位于 {@code com.mall.api} 包下，而各业务模块的
 * {@code scanBasePackages} 只含自身包与 {@code com.mall.common}，且 mall-api
 * 原先没有 {@code src/main/resources} —— 这两个组件<b>从未被装配</b>：
 * 跨服务 {@code /inner/**} 既不加签也不验签，安全性实际只靠网关拦截与内网隔离。</p>
 *
 * <p>本类经 {@code AutoConfiguration.imports} 被所有依赖 mall-api 的模块引入，
 * 显式扫描该包下的两个组件。</p>
 *
 * <p><b>⚠️ 两侧必须同时装配</b>：只装配过滤器会让所有 {@code /inner/**} 调用因缺少
 * 签名头而失败（500/401）——加签与验签是同一条链路的两个半环。</p>
 *
 * <p><b>⚠️ 装配由配置驱动</b>：仅在 {@code mall.security.internal-secret} 存在时生效。
 * 生产环境该配置在 Nacos 的 {@code application-dev.yml} 中（见 AGENTS.md 配置清单）；
 * 未配置时不装配，避免"没有密钥却强验签"导致的全部内网调用失败。反过来说，
 * <b>生产若漏配该密钥，签名将静默不生效</b>——部署时须核对 Nacos。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty("mall.security.internal-secret")
@ComponentScan(basePackageClasses = InnerSecurityAutoConfiguration.class)
public class InnerSecurityAutoConfiguration {
}
