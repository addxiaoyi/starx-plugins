# 物理端口配置说明

## 默认端口一览

| 服务 | 默认地址 | 端口 | 协议 |
|------|----------|------|------|
| StarX HTTP API | `127.0.0.1` | 8788 | HTTP |
| Velocity 代理 | `0.0.0.0` | 25577 | Minecraft |
| Velocity 查询 | `0.0.0.0` | 25577 | UDP |
| Paper/Folia 后端 | `0.0.0.0` | 25565 | Minecraft |
| MySQL | 自定义 | 3306 | TCP |
| PostgreSQL | 自定义 | 5432 | TCP |

## 安全建议

- HTTP API 默认绑定 `127.0.0.1`，通过 Nginx/Traefik 反向代理提供 HTTPS。
- 不要将 `0.0.0.0:8788` 直接暴露到公网。

## 修改 StarX 端口

```yaml
http:
  host: "127.0.0.1"
  port: 8788
```

## 完整文档

详见仓库 `docs/setup/ports.md`。
