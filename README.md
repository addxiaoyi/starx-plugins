# StarX Plugins

Minecraft 代理 + 子服 All-in-One 插件套件。统一的多模块 Gradle 项目，Velocity 代理端 28 个模块 + Paper 子服端 10 个模块。

## 架构

```
starx-plugins/
├── starx-api/          # API 契约层 (DTO, 事件, 消息, 仓库接口)
├── starx-common/       # 跨平台实现 (HikariCP + JDBI + Flyway, BCrypt, TOTP)
├── starx-velocity/     # Velocity 代理端 (28 个模块)
├── starx-paper/        # Paper 子服端 (10 个模块)
└── starx-testfixtures/ # 测试辅助 (内存实现, Mock)
```

## 快速开始

```bash
# 编译
./gradlew :starx-velocity:shadowJar :starx-paper:shadowJar

# 测试
./gradlew :starx-velocity:test :starx-paper:test

# 产物
# starx-velocity/build/libs/starx-velocity-0.1.0-SNAPSHOT-all.jar
# starx-paper/build/libs/starx-paper-0.1.0-SNAPSHOT-all.jar
```

## 要求

- Java 21
- Minecraft 1.21.1+
- Velocity 代理端 (LimboAPI 必需)
- Paper 子服 (SkinsRestorer 可选)

## 模块列表

### Velocity 端 (28 个)

| 类别 | 模块 | 配置键 |
|------|------|--------|
| 核心 | AuthModule | `auth` |
| 核心 | SkinBridgeModule | `skin-bridge` |
| 核心 | VelocityMessageBridge | `messaging` |
| 认证 | YggdrasilModule | `auth.yggdrasil` |
| 认证 | UniAuthModule | `auth.uniauth` |
| 认证 | FloodgateModule | `auth.floodgate` |
| 认证 | TabIntegrationModule | `auth.tab` |
| 认证 | MigrationModule | `auth.migration` |
| 代理工具 | MaintenanceModule | `proxytools.maintenance` |
| 代理工具 | MotdModule | `proxytools.motd` |
| 代理工具 | ChatModule | `proxytools.chat` |
| 代理工具 | RedirectModule | `proxytools.redirect` |
| 代理工具 | QueueModule | `proxytools.queue` |
| 代理工具 | LimboHubModule | `proxytools.limbo` |
| 代理工具 | ReconnectModule | `proxytools.reconnect` |
| 代理工具 | ProxyInfoModule | `proxytools.info` |
| 代理工具 | ForgeCompatModule | `proxytools.forge` |
| 代理工具 | RakNetModule | `proxytools.raknet` |
| 代理工具 | OnlineSyncModule | `proxytools.online` |
| 代理工具 | EnhancedProxyModule | `proxytools.enhanced` |
| 代理工具 | FileCleanerModule | `proxytools.filecleaner` |
| 安全 | BotFilterModule | `security.bot` |
| 安全 | CrashFixModule | `security.crash` |
| 安全 | RiskModule | `security.risk` |
| 安全 | AnticheatModule | `security.anticheat` |
| 集成 | QqIntegrationModule | `integrations.qq` |
| 集成 | PlanIntegrationModule | `integrations.plan` |
| 集成 | MapModIntegrationModule | `integrations.mapmod` |
| 集成 | SocialIntegrationModule | `integrations.social` |

### Paper 端 (10 个)

| 类别 | 模块 | 配置键 |
|------|------|--------|
| 核心 | ChatModule | `chat` |
| 核心 | MaintenanceModule | `maintenance` |
| 核心 | PaperSkinModule | `skin` |
| 安全 | AnticheatModule | `anticheat` |
| 安全 | CrashFixModule | `crashfix` |
| 同步 | NetworkingModule | `networking` |
| 同步 | MapModModule | `mapmod` |
| 集成 | QqModule | `qq` |
| 集成 | PlanModule | `plan` |
| 工具 | FileCleanerModule | `filecleaner` |

## 配置

首次启动后自动生成 `plugins/starx/config.yml`。模块通过 `modules` 节点控制启用/禁用：

```yaml
modules:
  auth:
    enabled: true
  security.bot:
    enabled: true
  integrations.qq:
    enabled: false
```

详细信息见 [功能列表](docs/features.md)。

## 开发

```bash
# 代码格式化 (Google Java Format)
./gradlew spotlessApply

# 单独模块测试
./gradlew :starx-velocity:test --tests "*.BotFilterModuleTest"

# 依赖更新
./gradlew :starx-velocity:dependencies
```

## 许可证

MIT