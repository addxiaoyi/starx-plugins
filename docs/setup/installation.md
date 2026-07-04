# 安装步骤

本文档说明如何在 Velocity 代理端与 Paper/Folia 后端上安装 StarX Plugins。

## 环境要求

- Java 21 或更高版本
- Velocity 3.5.0-SNAPSHOT 或兼容版本
- Paper 1.21.4 或 Folia 兼容版本（后端）
- 可选：LimboAPI 1.1.26
- 可选：SkinsRestorer 15.12.4

## 构建插件

从源码构建：

```bash
./gradlew build
```

构建成功后，产物位于：

- `starx-velocity/build/libs/starx-velocity-*-all.jar`
- `starx-paper/build/libs/starx-paper-*-all.jar`

## 安装 Velocity 端

1. 将 `starx-velocity-*-all.jar` 复制到 Velocity 的 `plugins/` 目录。
2. 确保已安装 LimboAPI（如需离线 Limbo 验证）。
3. 启动 Velocity，StarX 会自动生成默认配置。
4. 关闭 Velocity，编辑 `plugins/starx-velocity/config.yml`：

   ```yaml
   http:
     host: "127.0.0.1"
     port: 8788
     token: "请替换为强随机 Token"

   database:
     type: H2
     url: "jdbc:h2:file:./plugins/starx-velocity/starx"
     username: "sa"
     password: ""
   ```

5. 再次启动 Velocity。

## 安装 Paper/Folia 端

1. 将 `starx-paper-*-all.jar` 复制到 Paper/Folia 服务端的 `plugins/` 目录。
2. 确保 `server.properties` 中 `online-mode=false` 且启用了 Velocity 转发：

   ```properties
   online-mode=false
   velocity.enabled=true
   velocity.secret=<与 velocity.toml 中的 secret 一致>
   ```

3. 启动 Paper/Folia，StarX 会自动生成默认配置。
4. 关闭服务端，按需编辑 `plugins/starx-paper/config.yml`。
5. 再次启动服务端。

## 配置 Velocity 转发

在 `velocity.toml` 中确保正确配置后端：

```toml
[servers]
lobby = "127.0.0.1:25565"

[forced-hosts]
# 按需配置

[player-info-forwarding]
mode = "modern"
secret = "your-secret"
```

## 数据库迁移

首次启动时，StarX 会自动运行 Flyway 迁移，创建所需表结构。请确保数据库用户具备 DDL 权限；生产环境稳定后，可收回 DDL 权限。

## 验证安装

1. 启动 Velocity 与 Paper/Folia。
2. 在 Velocity 服务端控制台查看 StarX 启动日志，确认模块加载成功。
3. 访问 `http://127.0.0.1:8788/health` 检查 HTTP API 是否正常。
4. 使用 Minecraft 客户端连接 Velocity，确认能正常登录。

## 升级

1. 备份配置文件与数据库。
2. 停止 Velocity 与 Paper/Folia。
3. 替换旧版 jar 文件。
4. 启动服务，StarX 会自动执行新的 Flyway 迁移。

## 故障排查

| 现象 | 可能原因 | 解决方案 |
|------|----------|----------|
| HTTP API 无法访问 | 端口被占用或未绑定到正确地址 | 检查 `config.yml` 与端口占用 |
| 玩家无法登录 | Velocity 与后端 secret 不一致 | 核对 `velocity.toml` 与 `server.properties` |
| 皮肤未同步 | SkinsRestorer 未安装或版本不兼容 | 确认 SkinsRestorer 版本为 15.12.4 |
| 数据库连接失败 | URL 或凭据错误 | 检查 `database.yml` 与网络连通性 |
