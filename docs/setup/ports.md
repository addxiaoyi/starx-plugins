# 物理端口配置说明

StarX 与 Minecraft 服务端、代理端以及周边服务会占用若干网络端口。部署前请确认这些端口未被占用，并根据实际需求调整防火墙与反向代理规则。

## 默认端口一览

| 服务 | 默认地址 | 默认端口 | 协议 | 说明 |
|------|----------|----------|------|------|
| StarX HTTP API | `127.0.0.1:8788` | 8788 | HTTP | Javalin 管理 API |
| StarX 指标端点 | `127.0.0.1:8788` | 8788 | HTTP | `/metrics` 路径 |
| Velocity 代理 | `0.0.0.0:25577` | 25577 | Minecraft | 玩家入口端口 |
| Velocity 查询 | `0.0.0.0:25577` | 25577 | UDP | 服务端列表查询（与游戏端口共用） |
| Paper/Folia 后端 | `0.0.0.0:25565` | 25565 | Minecraft | 后端游戏端口 |
| 数据库（H2） | 本地文件 | - | - | 嵌入式，无网络端口 |
| 数据库（MySQL） | 自定义 | 3306 | TCP | 可在 `database.yml` 中配置 |
| 数据库（PostgreSQL） | 自定义 | 5432 | TCP | 可在 `database.yml` 中配置 |

## StarX HTTP API

默认监听 `127.0.0.1:8788`，仅允许本机访问。如需通过公网或内网其他机器访问，请使用反向代理：

```nginx
server {
  listen 443 ssl;
  server_name starx.example.com;

  location / {
    proxy_pass http://127.0.0.1:8788;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
  }
}
```

不建议直接修改绑定地址为 `0.0.0.0` 并暴露到公网，除非额外配置 IP 白名单或 mTLS。

## Velocity 代理端口

在 `velocity.toml` 中配置：

```toml
bind = "0.0.0.0:25577"
show-max-players = 100
online-mode = true
```

- 如果 `online-mode=true`，玩家必须拥有正版账号。
- 如果 `online-mode=false`，StarX 将启用离线认证流程。

## Paper/Folia 后端端口

在 `server.properties` 中配置：

```properties
server-port=25565
online-mode=false
velocity.enabled=true
velocity.secret=<与 velocity.toml 一致>
```

注意：后端必须关闭 `online-mode`，由 Velocity 统一处理认证；否则会出现 UUID 不一致问题。

## 端口冲突排查

如果服务无法启动，请检查：

1. 目标端口是否被其他进程占用：`netstat -ano | findstr :8788`。
2. 防火墙是否放行了 Minecraft 与数据库端口。
3. 容器环境中是否正确映射了端口。

## 修改默认端口

StarX HTTP API 端口在 `plugins/starx-velocity/config.yml` 中修改：

```yaml
http:
  host: "127.0.0.1"
  port: 8788
```

Velocity 与 Paper 端口分别在其各自的配置文件中修改。
