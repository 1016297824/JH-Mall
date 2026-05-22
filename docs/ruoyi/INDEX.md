# 若依（RuoYi-Cloud）功能文档索引

## 文档列表

| # | 文档 | 说明 |
|---|------|------|
| 00 | [总览与架构](00-总览与架构.md) | 系统架构、微服务列表、外部依赖、数据流 |
| 01 | [系统管理](01-系统管理.md) | 用户/角色/菜单/部门/岗位管理 |
| 02 | [系统监控](02-系统监控.md) | 在线用户/操作日志/登录日志/定时任务 |
| 03 | [系统工具](03-系统工具.md) | 代码生成/表单构建/系统接口 |
| 04 | [核心框架](04-核心框架.md) | 网关/认证/公共模块/Feign 接口 |
| 05 | [认证与授权](05-认证与授权.md) | 登录流程/Token/权限校验/数据权限 |
| 06 | [字典与参数配置](06-字典与参数配置.md) | 字典管理/参数配置/缓存机制 |
| 07 | [通知公告](07-通知公告.md) | 公告发布/阅读跟踪 |
| 08 | [文件服务](08-文件服务.md) | 文件上传/本地存储/MinIO |
| 09 | [个人中心](09-个人中心.md) | 个人信息/密码修改/头像 |
| 10 | [前端架构](10-前端架构.md) | Vue 目录/路由/Vuex/API 封装 |
| 11 | [复杂问题排查与解决](11-复杂问题排查与解决.md) | SPA 404 排查：Vue Router 4 路由名冲突 |

## 后端代码结构
```
server/manager/
├── ruoyi-api/              # Feign 远程调用接口
├── ruoyi-auth/             # 认证授权中心
├── ruoyi-common/           # 公共模块（9 个子模块）
├── ruoyi-gateway/          # API 网关
├── ruoyi-modules/          # 业务模块
│   ├── ruoyi-system/       # 系统管理
│   ├── ruoyi-gen/          # 代码生成
│   ├── ruoyi-job/          # 定时任务
│   └── ruoyi-file/         # 文件服务
└── ruoyi-visual/           # 图形化管理
    └── ruoyi-monitor/      # 监控中心
```

## 前端代码结构
```
client/ruoyi-ui/
├── src/
│   ├── api/                # API 接口
│   ├── router/             # 路由配置
│   ├── store/              # Vuex 状态
│   └── views/              # 页面视图
│       ├── system/         # 系统管理
│       ├── monitor/        # 系统监控
│       └── tool/           # 系统工具
└── src/views/
    ├── system/user/        # 用户管理
    ├── system/role/        # 角色管理
    ├── system/menu/        # 菜单管理
    ├── system/dept/        # 部门管理
    ├── system/post/        # 岗位管理
    ├── system/dict/        # 字典管理
    ├── system/config/      # 参数配置
    ├── system/notice/      # 通知公告
    ├── system/logininfor/  # 登录日志
    ├── system/operlog/     # 操作日志
    ├── monitor/online/     # 在线用户
    ├── monitor/job/        # 定时任务
    ├── tool/gen/           # 代码生成
    └── tool/build/         # 表单构建
```
