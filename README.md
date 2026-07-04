# StarX Plugins

StarX 是一体化 Minecraft 插件套件，包含 Velocity 代理端插件和 Paper/Folia 后端配套插件。

## 模块

- `starx-api`：平台无关的公共契约（事件、DTO、仓库接口、消息通道）。
- `starx-common`：跨平台复用实现（配置、数据库、密码、HTTP 签名、事件总线）。
- `starx-testfixtures`：TDD 测试辅助类与内存实现。
- `starx-velocity`：Velocity 代理端插件，集成 37 个参考插件功能。
- `starx-paper`：Paper/Folia 后端配套插件。

## 构建

```bash
./gradlew build
```

构建产物：
- `starx-velocity/build/libs/starx-velocity-*-all.jar`
- `starx-paper/build/libs/starx-paper-*-all.jar`

## 文档

- 仓库文档：`docs/`
- 知识库：`obsidian-vault/`
