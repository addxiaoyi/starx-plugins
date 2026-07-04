# 模块架构总览

StarX Plugins 采用分层多模块架构，将平台无关契约、跨平台实现与平台专用代码分离。

## 模块分层图

```text
平台专用实现层
├─ starx-velocity（Velocity 代理端）
└─ starx-paper（Paper/Folia 后端）

跨平台实现层
└─ starx-common（配置、数据库、密码、HTTP、事件总线）

公共契约层
└─ starx-api（DTO、事件、消息通道、仓库接口）

测试辅助层
└─ starx-testfixtures（内存实现、断言辅助）
```

## 模块职责

### starx-api

- `dto`：`UserDto`、`SkinDto`、`WebhookPayload`。
- `event`：`EventBus`、`StarxEvent`、`EventTypes`。
- `messaging`：`PluginMessageChannels`、`PluginMessage`、`PluginMessageHandler`。
- `repository`：`UserRepository`、`SkinRepository`。

### starx-common

- Configurate YAML 配置加载。
- HikariCP + JDBI + Flyway 数据库栈。
- BCrypt 密码、TOTP。
- HTTP 签名。
- Micrometer 指标。

### starx-velocity

- Velocity 事件监听与 LimboAPI 集成。
- Javalin HTTP API（默认 `127.0.0.1:8788`）。
- Plugin Messaging 与 Paper 通信。
- SkinsRestorer API 集成。
- Prometheus 指标端点。

### starx-paper

- 接收 Plugin Messaging 消息。
- 本地应用 SkinsRestorer 皮肤。
- Folia 区域调度兼容。

## 构建产物

- `starx-velocity/build/libs/starx-velocity-*-all.jar`
- `starx-paper/build/libs/starx-paper-*-all.jar`

## 完整文档

详见仓库 `docs/architecture/overview.md`。
