# StarX Plugins 知识库

本知识库是 StarX Plugins 项目的 Obsidian 版本入口，按主题分为五个区域。完整详细内容请参见仓库 `docs/` 目录。

## 导航

- [[10-architecture/index|架构设计]]
- [[20-api/index|API 与协议]]
- [[30-setup/index|部署与配置]]
- [[40-development/index|开发实践]]
- [[50-ops/index|运维与 CI/CD]]

## 快速链接

- 项目仓库：`starx-plugins`
- 完整文档：`docs/`
- CI 配置：`.github/workflows/ci.yml`
- 构建命令：`./gradlew build`

## 模块速览

```text
starx-api          公共契约（DTO、事件、消息、仓库接口）
starx-common       跨平台实现（配置、数据库、密码、HTTP、指标）
starx-velocity     Velocity 代理端插件
starx-paper        Paper/Folia 后端插件
starx-testfixtures 测试辅助与内存实现
```

## 关键默认地址

| 服务 | 地址 |
|------|------|
| StarX HTTP API | `127.0.0.1:8788` |
| Velocity 代理 | `0.0.0.0:25577` |
| Paper/Folia 后端 | `0.0.0.0:25565` |
