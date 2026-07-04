# 配置项参考

StarX 的配置文件位于：

- Velocity 端：`plugins/starx-velocity/config.yml`
- Paper/Folia 端：`plugins/starx-paper/config.yml`
- 数据库配置：可内嵌在 `config.yml` 或单独存放在 `database.yml`

## 顶层配置

```yaml
# 插件版本，请勿手动修改
version: "0.1.0-SNAPSHOT"

# 调试模式
debug: false

# 功能开关
features:
  auth:
    enabled: true
    allowOffline: true
    requireLimbo: false
    maxLoginAttempts: 5
    lockoutMinutes: 15
  totp:
    enabled: false
    issuer: "StarX"
  skins:
    enabled: true
    fallbackToLocal: true
    syncToBackend: true
    apiTimeoutMs: 5000
  webhook:
    enabled: false
    url: "https://example.com/webhook/starx"
    secret: ""
    retryAttempts: 3
```

## HTTP API 配置

```yaml
http:
  host: "127.0.0.1"
  port: 8788
  token: "CHANGE_ME"
  cors:
    enabled: false
    allowedOrigins: []
```

| 项 | 类型 | 默认值 | 说明 |
|----|------|--------|------|
| `http.host` | string | `127.0.0.1` | 绑定地址 |
| `http.port` | int | `8788` | 监听端口 |
| `http.token` | string | `CHANGE_ME` | Bearer Token，必须修改 |
| `http.cors.enabled` | bool | `false` | 是否启用 CORS |
| `http.cors.allowedOrigins` | list | `[]` | 允许的 Origin 列表 |

## 数据库配置

```yaml
database:
  type: H2
  url: "jdbc:h2:file:./plugins/starx-velocity/starx"
  username: "sa"
  password: ""
  pool:
    maximumPoolSize: 10
    minimumIdle: 2
    connectionTimeoutMs: 30000
    idleTimeoutMs: 600000
    maxLifetimeMs: 1800000
```

| 项 | 类型 | 默认值 | 说明 |
|----|------|--------|------|
| `database.type` | string | `H2` | 可选：`H2`、`MYSQL`、`POSTGRESQL` |
| `database.url` | string | - | JDBC URL |
| `database.username` | string | - | 数据库用户名 |
| `database.password` | string | - | 数据库密码 |
| `database.pool.maximumPoolSize` | int | 10 | 连接池最大连接数 |
| `database.pool.minimumIdle` | int | 2 | 最小空闲连接数 |

## MySQL 示例

```yaml
database:
  type: MYSQL
  url: "jdbc:mysql://localhost:3306/starx?useSSL=true&serverTimezone=Asia/Shanghai"
  username: "starx"
  password: "secure_password"
```

## PostgreSQL 示例

```yaml
database:
  type: POSTGRESQL
  url: "jdbc:postgresql://localhost:5432/starx?ssl=true"
  username: "starx"
  password: "secure_password"
```

## 认证配置

```yaml
auth:
  onlineModeRouting:
    enabled: true
  offline:
    passwordMinLength: 6
    passwordMaxLength: 128
    requireEmail: false
    allowRegistration: true
  limbo:
    enabled: false
    serverName: "limbo"
```

## 指标配置

```yaml
metrics:
  enabled: true
  exportIntervalSeconds: 60
  prometheus:
    enabled: true
    path: "/metrics"
```

## 消息配置

```yaml
messaging:
  channel: "starx:main"
  skinSyncIntervalSeconds: 30
```

## Webhook 签名

Webhook 请求体使用 HMAC-SHA256 签名，签名密钥为 `webhook.secret`。请求头：

```http
X-StarX-Signature: sha256=<hex>
X-StarX-Event-Id: <uuid>
X-StarX-Timestamp: <ISO-8601>
```

## 环境变量覆盖

以下配置项支持通过环境变量覆盖，优先级高于配置文件：

| 环境变量 | 对应配置 |
|----------|----------|
| `STARX_HTTP_TOKEN` | `http.token` |
| `STARX_DB_URL` | `database.url` |
| `STARX_DB_USERNAME` | `database.username` |
| `STARX_DB_PASSWORD` | `database.password` |
| `STARX_WEBHOOK_SECRET` | `webhook.secret` |

## 配置重载

部分配置支持热重载，可通过 HTTP API 或游戏内命令触发：

```bash
curl -X POST http://127.0.0.1:8788/api/v1/admin/reload \
  -H "Authorization: Bearer <token>"
```

注意：数据库连接池与 HTTP 绑定地址变更后需要重启服务。
