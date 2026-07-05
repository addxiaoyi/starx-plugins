# StarX Plugins 功能列表

## 项目概述

StarX 是一体化 Minecraft 网络插件套件，包含 Velocity 代理端插件与 Paper/Folia 子服端插件。采用多模块 Gradle 构建，核心契约定义在 `starx-api`，平台实现分别在 `starx-velocity` 与 `starx-paper`。

---

## Velocity 代理端（28 个模块）

### 认证系统

| 模块 | 配置键 | 功能 |
|------|--------|------|
| **AuthModule** | `auth` | 核心认证引擎，管理登录会话、Limbo 停留、正版/离线验证、TOTP 两步验证、信任设备 |
| **YggdrasilModule** | `auth.yggdrasil` | 对接第三方 Yggdrasil 验证服务器，支持自定义认证 API |
| **UniAuthModule** | `auth.uniauth` | 统一认证接口，支持多后端聚合验证 |
| **FloodgateModule** | `auth.floodgate` | 基岩版玩家 Floodgate 认证，允许 BE 玩家加入 Java 版服务器 |
| **TabIntegrationModule** | `auth.tab` | TAB 插件集成，TAB 排序与认证状态联动 |
| **MigrationModule** | `auth.migration` | Mojang → Microsoft 账户迁移检测与提示 |

### 代理工具

| 模块 | 配置键 | 功能 |
|------|--------|------|
| **MaintenanceModule** | `proxytools.maintenance` | 维护模式，自定义 Kick 消息，白名单绕过 |
| **MotdModule** | `proxytools.motd` | 动态 MOTD，支持图标、在线人数、协议版本伪造 |
| **ChatModule** | `proxytools.chat` | 全局聊天同步，跨服聊天转发 |
| **RedirectModule** | `proxytools.redirect` | 玩家重定向，支持按服务器/状态智能路由 |
| **QueueModule** | `proxytools.queue` | 排队系统，满服时自动排队，支持优先级与 VIP 跳过 |
| **LimboHubModule** | `proxytools.limbo` | Limbo 虚拟大厅，登录前停留，支持虚拟世界配置 |
| **ReconnectModule** | `proxytools.reconnect` | 断线重连，Memory/Redis 两种存储后端 |
| **ProxyInfoModule** | `proxytools.info` | 代理状态查询，在线玩家数、TPS、延迟统计 |
| **ForgeCompatModule** | `proxytools.forge` | Forge 客户端兼容，FML2 握手处理 |
| **RakNetModule** | `proxytools.raknet` | 基岩版 RakNet 协议兼容，BE-JE 互通 |
| **OnlineSyncModule** | `proxytools.online` | 在线玩家同步，全局玩家列表 |
| **EnhancedProxyModule** | `proxytools.enhanced` | 增强代理命令集：`/glist`、`/find`、`/send`、`/alert`、`/ping`、`/kickall` |
| **FileCleanerModule** | `proxytools.filecleaner` | 定时清理过期日志/缓存文件，支持按天数/大小规则 |

### 安全

| 模块 | 配置键 | 功能 |
|------|--------|------|
| **BotFilterModule** | `security.bot` | 机器人攻击检测，连接速率限制、黑名单、验证码（可选） |
| **CrashFixModule** | `security.crash` | 崩溃漏洞修复，拦截异常数据包、超大书本、非法 NBT |
| **RiskModule** | `security.risk` | 风险引擎，IP/ASN 信誉检测、GeoIP 区域限制、异常行为评分 |
| **AnticheatModule** | `security.anticheat` | 代理层反作弊，移动校验、数据包速率检测 |

### 第三方集成

| 模块 | 配置键 | 功能 |
|------|--------|------|
| **SkinBridgeModule** | `skin-bridge` | 皮肤桥接，与 Paper 端 SkinsRestorer 联动 |
| **QqIntegrationModule** | `integrations.qq` | QQ 群机器人集成，支持命令执行、状态查询、事件通知 |
| **PlanIntegrationModule** | `integrations.plan` | Plan 统计插件集成，玩家数据分析 |
| **MapModIntegrationModule** | `integrations.mapmod` | 地图模组集成，网页地图上显示玩家位置 |
| **SocialIntegrationModule** | `integrations.social` | 社交功能：好友系统、公会、私聊 |

---

## Paper 子服端（10 个模块）

| 模块 | 配置键 | 功能 |
|------|--------|------|
| **ChatModule** | `chat` | 聊天格式化，与 Velocity 全局聊天同步 |
| **MaintenanceModule** | `maintenance` | 维护模式，配合 Velocity 端实现双层维护 |
| **PaperSkinModule** | `skin` | 皮肤应用，需 SkinsRestorer 依赖 |
| **AnticheatModule** | `anticheat` | 子服端反作弊，移动/战斗/交互检测，向 Velocity 上报 |
| **CrashFixModule** | `crashfix` | 崩溃漏洞修复，拦截危险命令、超大物品、非法数据包 |
| **NetworkingModule** | `networking` | 网络优化，数据包压缩、带宽控制 |
| **MapModModule** | `mapmod` | 地图模组数据提供，与 Velocity 端联动 |
| **QqModule** | `qq` | QQ 机器人指令执行，与 Velocity 端消息转发 |
| **PlanModule** | `plan` | Plan 统计插件数据提供 |
| **FileCleanerModule** | `filecleaner` | 定时清理过期文件 |

---

## 核心通信

### Plugin Messaging 通道

Velocity 与 Paper 端通过 Plugin Messaging 通道通信：

| 通道 | 方向 | 用途 |
|------|------|------|
| `starx:auth` | Velocity → Paper | 认证状态同步 |
| `starx:skin` | Velocity → Paper | 皮肤变更通知 |
| `starx:chat` | 双向 | 全局聊天消息 |
| `starx:maintenance` | Velocity → Paper | 维护模式切换 |
| `starx:anticheat` | Paper → Velocity | 反作弊检测上报 |
| `starx:sync` | 双向 | 在线玩家同步 |

### HTTP API

Velocity 端提供 REST API（默认端口 9287）：

| 端点 | 方法 | 用途 |
|------|------|------|
| `/api/v2/plugin/status` | GET | 代理状态查询 |
| `/api/v2/plugin/online` | GET | 在线玩家列表 |
| `/api/v2/plugin/kick` | POST | 踢出玩家 |
| `/api/v2/plugin/ban` | POST | 封禁玩家 |
| `/api/v2/plugin/alert` | POST | 全服公告 |
| `/api/v2/plugin/user` | GET | 用户查询 |
| `/api/v2/plugin/user/delete` | DELETE | 删除用户 |
| `/api/v2/plugin/user/password` | POST | 重置密码 |
| `/api/v2/plugin/user/email` | POST | 绑定邮箱 |
| `/api/v2/plugin/user/link` | POST | 关联外部账户 |
| `/api/v2/plugin/skin/refresh` | POST | 刷新皮肤缓存 |

---

## 技术栈

- **语言**：Java 21
- **构建**：Gradle 8.14 + Kotlin DSL
- **代理**：Velocity API
- **子服**：Paper API 1.21.1+（支持 Folia）
- **数据库**：HikariCP + JDBI + Flyway（H2/MySQL）
- **测试**：JUnit 5 + Mockito，206 个测试用例
- **代码规范**：Google Java Format + Spotless
- **CI/CD**：GitHub Actions，自动测试、构建、发布
