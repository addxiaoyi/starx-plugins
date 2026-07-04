# StarX Plugins 文档

欢迎来到 StarX Plugins 文档中心。本文档面向开发者、运维人员与服务器管理员，覆盖架构设计、API 说明、部署配置与开发流程。

## 快速导航

| 目录 | 说明 |
|------|------|
| [architecture/overview](architecture/overview.md) | 模块架构总览与分层说明 |
| [architecture/module-loader](architecture/module-loader.md) | 模块加载器设计 |
| [architecture/auth-flow](architecture/auth-flow.md) | 认证流程：Limbo、正版/离线路由 |
| [api/rest-endpoints](api/rest-endpoints.md) | HTTP API 端点列表 |
| [api/plugin-messaging](api/plugin-messaging.md) | Plugin Messaging 通道与消息格式 |
| [api/skinsrestorer-bridge](api/skinsrestorer-bridge.md) | SkinsRestorer 集成说明 |
| [setup/ports](setup/ports.md) | 物理端口与默认监听地址 |
| [setup/installation](setup/installation.md) | 安装与启动步骤 |
| [setup/config-reference](setup/config-reference.md) | 配置项参考 |
| [development/tdd-guide](development/tdd-guide.md) | TDD 开发指南 |

## 项目简介

StarX 是一体化 Minecraft 插件套件，包含 Velocity 代理端插件与 Paper/Folia 后端配套插件。项目采用多模块 Gradle 构建，核心契约定义在 `starx-api` 中，平台实现分别位于 `starx-velocity` 与 `starx-paper`。

## 构建

```bash
./gradlew build
```

构建产物：

- `starx-velocity/build/libs/starx-velocity-*-all.jar`
- `starx-paper/build/libs/starx-paper-*-all.jar`

## 贡献

请遵循 [TDD 开发指南](development/tdd-guide.md)，在提交前运行 `./gradlew spotlessCheck` 与 `./gradlew test`。
