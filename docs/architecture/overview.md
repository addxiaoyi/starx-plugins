# 模块架构总览

StarX Plugins 采用分层多模块架构，将平台无关契约、跨平台实现与平台专用代码分离，便于单元测试、逻辑复用与后续扩展。

## 模块分层

```text
┌─────────────────────────────────────────────────────────────┐
│                     平台专用实现层                            │
│  ┌─────────────────────┐  ┌─────────────────────┐           │
│  │   starx-velocity    │  │     starx-paper     │           │
│  │  Velocity 代理端插件 │  │  Paper/Folia 后端插件 │          │
│  └──────────┬──────────┘  └──────────┬──────────┘           │
└─────────────┼────────────────────────┼───────────────────────┘
              │                        │
              ▼                        ▼
┌─────────────────────────────────────────────────────────────┐
│                     跨平台实现层                              │
│                     starx-common                            │
│  配置、数据库、密码、HTTP 签名、事件总线实现、仓库实现          │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                     公共契约层                                │
│                      starx-api                               │
│       DTO、事件、消息通道、仓库接口、平台无关工具             │
└─────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────┐
│                     测试辅助层                                │
│                  starx-testfixtures                          │
│         内存仓库、事件总线伪实现、测试工厂与断言辅助          │
└─────────────────────────────────────────────────────────────┘
```

## 模块职责

### starx-api

平台无关的公共契约，供所有模块依赖：

- `dto`：跨模块传输对象（`UserDto`、`SkinDto`、`WebhookPayload`）。
- `event`：事件总线接口 `EventBus`、事件 envelope `StarxEvent`、事件类型常量 `EventTypes`。
- `messaging`：Velocity 与 Paper 间 Plugin Messaging 的通道常量 `PluginMessageChannels`、消息对象 `PluginMessage`、处理器接口 `PluginMessageHandler`。
- `repository`：仓库接口 `UserRepository`、`SkinRepository`。

设计原则：`starx-api` 不依赖任何平台 API，仅依赖 Gson 等通用库，确保契约的稳定性与可测试性。

### starx-common

跨平台复用实现，依赖 `starx-api`：

- 配置加载（Configurate YAML）。
- 数据库连接池（HikariCP）、JDBI SQL 对象、Flyway 迁移。
- 密码哈希（BCrypt）、TOTP 工具。
- HTTP 签名与校验。
- 事件总线、仓库与 Webhook 发送的默认实现。
- Micrometer 指标核心。

运行时支持 H2、MySQL、PostgreSQL。

### starx-velocity

Velocity 代理端插件：

- 监听玩家登录/断开事件。
- 通过 LimboAPI 实现 Limbo 认证流程。
- 根据在线模式结果分发正版/离线玩家路由。
- 通过 Javalin 暴露 HTTP API，默认监听 `127.0.0.1:8788`。
- 通过 Plugin Messaging 与 Paper 后端同步玩家状态、配置与皮肤。
- 集成 SkinsRestorer API 进行皮肤查询与应用。
- 暴露 Prometheus 指标端点。

### starx-paper

Paper/Folia 后端配套插件：

- 接收 Plugin Messaging 消息并更新本地玩家状态。
- 在服务端本地应用 SkinsRestorer 皮肤。
- 监听后端事件并回传代理端。
- 支持 Folia 区域调度。

### starx-testfixtures

TDD 辅助类与内存实现：

- `InMemoryUserRepository`、`InMemorySkinRepository`。
- `RecordingEventBus`。
- JUnit 5、Mockito、AssertJ 的常用断言与工厂。

## 依赖方向

```text
starx-velocity ──► starx-common ──► starx-api
starx-paper    ──► starx-common ──► starx-api
starx-common   ──► starx-api
starx-testfixtures ──► starx-api + starx-common
```

不允许平台模块反向依赖，也不允许 `starx-api` 依赖任何平台或实现细节。

## 构建产物

- `starx-velocity/build/libs/starx-velocity-*-all.jar`：ShadowJar，包含重定位后的 Javalin、Gson、Flyway、JDBI、HikariCP、BCrypt 等。
- `starx-paper/build/libs/starx-paper-*-all.jar`：ShadowJar，包含重定位后的 Gson。
