# 配置项参考

## 顶层配置

```yaml
version: "0.1.0-SNAPSHOT"
debug: false
```

## HTTP API

```yaml
http:
  host: "127.0.0.1"
  port: 8788
  token: "CHANGE_ME"
```

## 数据库

```yaml
database:
  type: H2
  url: "jdbc:h2:file:./plugins/starx-velocity/starx"
  username: "sa"
  password: ""
```

类型可选：`H2`、`MYSQL`、`POSTGRESQL`。

## 功能开关

```yaml
features:
  auth:
    enabled: true
    allowOffline: true
    requireLimbo: false
    maxLoginAttempts: 5
  totp:
    enabled: false
  skins:
    enabled: true
    fallbackToLocal: true
    syncToBackend: true
```

## 环境变量

| 环境变量 | 对应配置 |
|----------|----------|
| `STARX_HTTP_TOKEN` | `http.token` |
| `STARX_DB_URL` | `database.url` |
| `STARX_DB_USERNAME` | `database.username` |
| `STARX_DB_PASSWORD` | `database.password` |

## 完整文档

详见仓库 `docs/setup/config-reference.md`。
