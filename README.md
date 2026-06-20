# JH-Mall — 电商平台

基于 Spring Cloud + Vue 3 的 C 端电商系统。仓库含全部前后端源码、设计文档及 MVP 部署方案。

## 项目结构

```
JH-Mall/
├── server/                    # 后端
│   ├── ruoyi/                 #   RuoYi-Cloud 基座（gateway/job/file/admin）
│   └── mall/                  #   Mall 业务模块（product/search/order/…）
├── client/
│   ├── mall-ui/               #   C 端（Vue 3 + TS + Vite）
│   └── ruoyi-ui/              #   管理端（Vue 2 + webpack）
├── deploy/
│   ├── docker/                #   本地开发编排
│   └── MVP/                   #   生产部署（2G 服务器极限配置）
├── db/                        # 数据库脚本
└── docs/                      # 系统设计文档
```

## 技术栈

| 层 | 技术 |
|----|------|
| 微服务框架 | Spring Cloud + Nacos v3.2.1 |
| 网关 | Spring Cloud Gateway |
| 鉴权 | OAuth 2.0 + JWT |
| ORM | MyBatis-Plus |
| 数据库 | MySQL 8.0 + Elasticsearch 9.2.5 |
| 缓存 / 锁 | Redis 7.4 + Redisson |
| 前端 | Vue 3 + TypeScript + Vite + Element Plus (C端)，Vue 2 + Element UI (管理端) |

## 快速开始

### 本地开发

1. 启动基础设施（docker-compose）：`cd deploy/docker && docker compose up -d`
2. 导入数据库：`db/ruoyi-sql/` 和 `db/mall-sql/`
3. 构建后端：`cd server/ruoyi && mvn clean install -DskipTests`，然后 `cd ../mall && mvn clean install -DskipTests`
4. 启动 C 端前端：`cd client/mall-ui && npm install && npm run dev`

## 在线地址

MVP 生产环境：https://mall.xiaoziai.cn
