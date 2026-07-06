# StarX 使用指南

StarX 是一体化 Minecraft 网络管理插件，在 Velocity 代理端和 Paper 子服端分别安装，即可获得完整的服务器管理功能。

---

## 安装

### 环境要求

- Java 21 或更高
- Velocity 代理端
- Paper 1.21.1+ 子服

### 步骤

1. 从 [Releases](https://github.com/addxiaoyi/starx-plugins/releases) 下载 `starx-velocity-*.jar` 和 `starx-paper-*.jar`
2. 将 `starx-velocity-*.jar` 放入 Velocity 的 `plugins/` 目录
3. 将 `starx-paper-*.jar` 放入每个子服的 `plugins/` 目录
4. 重启服务，StarX 会自动生成默认配置
5. 编辑 `plugins/starx/config.yml` 按需调整设置

---

## 命令列表

### 玩家命令

| 命令 | 说明 | 使用场景 |
|------|------|----------|
| `/hub` / `/lobby` | 返回大厅 | 从子服回到大厅服务器 |
| `/list` | 查看在线玩家 | 查看所有子服的在线玩家 |
| `/ping [玩家]` | 查看延迟 | 查看自己或别人的网络延迟 |
| `/2fa status` | 查看二步验证状态 | 检查是否开启 2FA |
| `/2fa enable <密码>` | 开启二步验证 | 增强账户安全 |
| `/2fa disable <密码>` | 关闭二步验证 | 停用 2FA |

> 登录/注册无需命令，进入服务器后直接在聊天框输入密码即可（首次输入自动注册，之后输入自动登录）。

### 管理员命令

| 命令 | 说明 | 权限 |
|------|------|------|
| `/glist` | 全局服务器列表，显示各子服及玩家 | `starx.commands.glist` |
| `/find <玩家>` | 查找玩家所在服务器 | `starx.commands.find` |
| `/send <玩家> <服务器>` | 将玩家传送到指定服务器 | `starx.commands.send` |
| `/alert <消息>` | 全服公告 | `starx.commands.alert` |
| `/kickall <服务器> [原因]` | 踢出指定服务器的所有玩家 | `starx.commands.kickall` |
| `/maintenance <on\|off>` | 开关维护模式 | `starx.maintenance.bypass` |
| `/starx info` | 查看代理状态（玩家数、内存、运行时间） | 无 |
| `/starx servers` | 查看所有子服状态 | 无 |
| `/starx uptime` | 查看运行时间 | 无 |
| `/starx anticheat` | 查看反作弊统计 | 无 |
| `/starx anticheat player <玩家名>` | 查看玩家违规详情 | 无 |
| `/starx anticheat clear [玩家名]` | 清除反作弊数据 | 无 |
| `/skin` | 刷新皮肤 | 无 |

### 权限列表

| 权限节点 | 说明 |
|----------|------|
| `starx.commands.glist` | 使用 `/glist` |
| `starx.commands.find` | 使用 `/find` |
| `starx.commands.send` | 使用 `/send` |
| `starx.commands.alert` | 使用 `/alert` |
| `starx.commands.ping` | 使用 `/ping` |
| `starx.commands.kickall` | 使用 `/kickall` |
| `starx.kickall.bypass` | 被 `/kickall` 时不被踢出 |
| `starx.maintenance.bypass` | 维护模式时仍可登录 |
| `starx.anticheat.bypass` | 绕过反作弊检测 |
| `starx.crashfix.bypass` | 绕过崩溃拦截限制 |

---

## 功能说明

### 账号系统

StarX 提供完整的账号注册、登录、二步验证功能。

**配置示例**（`plugins/starx/config.yml`）：

```yaml
features:
  auth:
    enabled: true
    allowOffline: true       # 允许离线模式玩家注册
    maxLoginAttempts: 5      # 最多尝试次数
    lockoutMinutes: 15       # 锁定时间
  totp:
    enabled: false           # 是否开启二步验证
    issuer: "StarX"
```

**工作流程**：玩家首次进入时直接在聊天框输入密码即可自动注册，之后输入密码即可自动登录。二步验证（2FA）为可选功能，玩家可自行通过命令开启或关闭：

- **开启**：`/2fa enable <密码>` — 获取 TOTP 密钥，用 Google Authenticator 等 App 扫码
- **关闭**：`/2fa disable <密码>` — 移除二步验证
- **查看**：`/2fa status` — 查看当前 2FA 状态

开启 2FA 后，每次登录输入密码后需额外输入验证码。

### 维护模式

维护期间只允许拥有 `starx.maintenance.bypass` 权限的玩家进入，其他玩家会看到维护提示。

```
/maintenance on    # 开启维护
/maintenance off   # 关闭维护
```

### 动态 MOTD

服务器列表显示的 MOTD 会根据在线人数、服务器状态动态变化，支持图标和自定义文本。

### 全局聊天

开启后，不同子服的玩家可以互相聊天，消息会跨服同步显示。

### 排队系统

满服时自动排队，支持 VIP 优先级跳过。排队期间玩家停留在 Limbo 虚拟大厅。

### 皮肤系统

需要子服安装 SkinsRestorer 插件。玩家更换皮肤后自动同步到所有子服。

### 断线重连

玩家意外断线后，在配置时间内重连会自动回到原来的服务器，不会丢失背包和位置。

### 反作弊

自动检测移动异常、战斗加速、数据包速率异常等行为。`/starx anticheat player <玩家名>` 可查看具体违规记录。

### 机器人防护

检测并拦截可疑的机器人连接，支持连接速率限制、IP 黑名单。

### 崩溃修复

自动拦截可能导致服务器崩溃的危险操作，包括超大书本、异常 NBT 数据、危险命令等。

### 基岩版兼容

允许基岩版（BE）玩家通过 Floodgate 加入 Java 版服务器，支持 RakNet 协议。

### Forge 兼容

支持 Forge 客户端通过代理连接，处理 FML2 握手。

### QQ 机器人

对接 QQ 群机器人，可在群内执行服务器命令、查看状态、接收事件通知。

### 文件清理

自动清理过期日志和缓存文件，可按天数和文件大小设置规则。

### 社交功能

好友系统、公会、跨服私聊（需 Velocity 端和 Paper 端同时启用）。

---

## HTTP API

StarX 提供 REST API 用于外部管理，默认端口 9287，使用 Bearer Token 鉴权。

| 端点 | 用途 |
|------|------|
| `GET /api/v2/plugin/status` | 查看代理状态 |
| `GET /api/v2/plugin/online` | 在线玩家列表 |
| `POST /api/v2/plugin/kick` | 踢出玩家 |
| `POST /api/v2/plugin/ban` | 封禁玩家 |
| `POST /api/v2/plugin/alert` | 全服公告 |
| `GET /api/v2/plugin/user` | 查询用户信息 |
| `DELETE /api/v2/plugin/user/delete` | 删除用户 |
| `POST /api/v2/plugin/user/password` | 重置用户密码 |
| `POST /api/v2/plugin/user/email` | 绑定邮箱 |
| `POST /api/v2/plugin/user/link` | 关联外部账户 |
| `POST /api/v2/plugin/skin/refresh` | 刷新皮肤缓存 |

---

## 配置重载

部分配置修改后无需重启，通过 API 即可重新加载：

```bash
curl -X POST http://127.0.0.1:9287/api/v1/admin/reload \
  -H "Authorization: Bearer <你的token>"
```

---

## 升级

1. 备份 `plugins/starx/config.yml` 和数据库
2. 停止服务
3. 替换 jar 文件
4. 启动服务，数据库迁移会自动执行
