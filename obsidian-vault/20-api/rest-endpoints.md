# HTTP API 端点

StarX Velocity 通过 Javalin 暴露内部管理 API，默认监听 `127.0.0.1:8788`。

## 鉴权

请求头：`Authorization: Bearer <token>`，token 在 `config.yml` 中配置。

## 核心端点

| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| GET | `/health` | 否 | 健康检查 |
| GET | `/api/v1/players/{uuid}` | Bearer | 按 UUID 查玩家 |
| GET | `/api/v1/players/by-username/{username}` | Bearer | 按用户名查玩家 |
| GET | `/api/v1/players/by-email/{email}` | Bearer | 按邮箱查玩家 |
| PATCH | `/api/v1/players/{uuid}` | Bearer | 更新玩家信息 |
| DELETE | `/api/v1/players/{uuid}` | Bearer | 删除玩家 |
| GET | `/api/v1/players/{uuid}/skin` | Bearer | 查询皮肤 |
| POST | `/api/v1/players/{uuid}/skin` | Bearer | 设置皮肤 |
| DELETE | `/api/v1/players/{uuid}/skin` | Bearer | 清除皮肤 |
| POST | `/api/v1/admin/kick` | Bearer | 踢出玩家 |
| POST | `/api/v1/admin/ban` | Bearer | 封禁玩家 |
| POST | `/api/v1/admin/reset-password` | Bearer | 重置密码 |
| POST | `/api/v1/admin/webhook/test` | Bearer | 测试 Webhook |
| GET | `/metrics` | 否 | Prometheus 指标 |

## 完整文档

详见仓库 `docs/api/rest-endpoints.md`。
