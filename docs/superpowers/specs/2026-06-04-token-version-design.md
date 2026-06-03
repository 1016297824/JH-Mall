# token_version 改造设计

> 2026-06-04 | 自底向上实施 | 7 步

## 目标

accessToken 有效性从"Redis session key 存在性"改为"JWT.ver == Redis token_version"

## 核心思路

- **token_version**（DB + Redis 缓存）→ 负责全端下线（改密码 / 踢人）
- **JTI 黑名单 + refresh mapping**（Redis TTL）→ 负责 refreshToken 一次性轮换
- **不再写 session key** → 删除 `mall:auth:session:{userId}:{jti}` 相关逻辑

## 实施步骤（7 步）

| 步骤 | 模块 | 变更 | TDD |
|:---:|------|------|:---:|
| 1 | mall-user | DO → Mapper → Service → Controller | ✅ |
| 2 | mall-api | RemoteUserService Feign 新增方法 | 无需 |
| 3 | mall-common | CacheConstants 新增 USER_VERSION、删除 SESSION | 无需 |
| 4 | mall-auth | MallAuthConfigProperties 新增 `tokenVersionCacheTtl` | 无需 |
| 5 | mall-auth | TokenServiceImpl（核心改造） | ✅ |
| 6 | ruoyi-gateway | MallAuthFilter 删除 session 检查，改为 version 比对 | ❌ |
| 7 | 编译验证 | `mvn clean install` | — |

## 关键设计决策

| 决策 | 结论 |
|------|------|
| Feign 返回类型 | 沿用现有风格：`void` / `Integer`，不用 `MallResult` |
| userId 类型 | Feign 边界 `String`，Service/Mapper 内部 `Long` |
| USER_VERSION TTL | Nacos `mall.auth.token-version-cache-ttl`，默认 2592000（30d） |
| 网关 cache miss | 不拦截，由下游 `verify()` 回源 DB 兜底 |
| revokeAll | 改为调 `incrementTokenVersion`，不再扫描 Redis |

## 新增 Redis Key

| Key | TTL | 用途 |
|-----|-----|------|
| `mall:auth:user_version:{userId}` | Nacos 配置（默认 30d） | 用户 token 版本号 |

## 删除 Redis Key

| Key | 说明 |
|-----|------|
| `mall:auth:session:{userId}:{jti}` | 不再写 session key，有效性改为 token_version 比对 |

## 不改的

- 前端刷新逻辑（`client.ts`）
- 登录/注册流程
- `refreshToken TTL`（7d）/ `accessToken TTL`（30min）
- JWT 签名算法（HS512）

## 参考

- 改动清单：`temp/token-version-changes.md`
- 设计文档：`docs/design/08_mall-auth详细设计.md`、`docs/design/03_07_系统详细设计-安全详细设计.md`、`docs/design/06_mall-common公共模块设计.md`
- Flyway：`db/mall-sql/V1.0.4__add_token_version.sql`（已执行）
