# HTTP API 端点

StarX Velocity 通过 Javalin 暴露内部管理 API，默认监听 `127.0.0.1:8788`。该 API 仅供本地服务（如网站后端、面板、监控脚本）调用，不建议直接暴露到公网。

## 基础信息

- **监听地址**：`127.0.0.1:8788`
- **协议**：HTTP/1.1（可通过反向代理启用 HTTPS）
- **序列化**：JSON
- **鉴权**：请求头 `Authorization: Bearer <token>`，token 在 `config.yml` 中配置
- **错误格式**：统一返回 JSON `{ "error": "<message>", "code": "<code>" }`

## 通用响应格式

成功：

```json
{
  "success": true,
  "data": { }
}
```

失败：

```json
{
  "success": false,
  "error": "Invalid token",
  "code": "UNAUTHORIZED"
}
```

## 端点列表

### 健康检查

| 属性 | 值 |
|------|-----|
| 方法 | `GET` |
| 路径 | `/health` |
| 鉴权 | 否 |
| 说明 | 返回服务运行状态与数据库连接状态 |

响应示例：

```json
{
  "status": "UP",
  "database": "UP",
  "version": "0.1.0-SNAPSHOT"
}
```

### 查询玩家

| 属性 | 值 |
|------|-----|
| 方法 | `GET` |
| 路径 | `/api/v1/players/{uuid}` |
| 鉴权 | Bearer Token |
| 说明 | 根据 UUID 查询玩家账户信息 |

路径参数：

| 参数 | 类型 | 说明 |
|------|------|------|
| `uuid` | string | 玩家 UUID，带或不带 `-` |

响应示例：

```json
{
  "success": true,
  "data": {
    "uuid": "550e8400-e29b-41d4-a716-446655440000",
    "username": "ExamplePlayer",
    "email": "player@example.com",
    "premium": true,
    "createdAt": "2024-01-01T00:00:00Z",
    "lastLoginAt": "2024-06-01T12:00:00Z",
    "externalUserId": "ext_12345"
  }
}
```

### 按用户名查询玩家

| 属性 | 值 |
|------|-----|
| 方法 | `GET` |
| 路径 | `/api/v1/players/by-username/{username}` |
| 鉴权 | Bearer Token |
| 说明 | 根据用户名查询玩家账户信息 |

### 按邮箱查询玩家

| 属性 | 值 |
|------|-----|
| 方法 | `GET` |
| 路径 | `/api/v1/players/by-email/{email}` |
| 鉴权 | Bearer Token |
| 说明 | 根据邮箱查询玩家账户信息 |

### 更新玩家信息

| 属性 | 值 |
|------|-----|
| 方法 | `PATCH` |
| 路径 | `/api/v1/players/{uuid}` |
| 鉴权 | Bearer Token |
| 说明 | 更新玩家邮箱、外部用户 ID 等信息 |

请求体：

```json
{
  "email": "new@example.com",
  "externalUserId": "ext_67890"
}
```

### 删除玩家

| 属性 | 值 |
|------|-----|
| 方法 | `DELETE` |
| 路径 | `/api/v1/players/{uuid}` |
| 鉴权 | Bearer Token |
| 说明 | 删除玩家账户与关联数据 |

### 查询玩家皮肤

| 属性 | 值 |
|------|-----|
| 方法 | `GET` |
| 路径 | `/api/v1/players/{uuid}/skin` |
| 鉴权 | Bearer Token |
| 说明 | 查询玩家当前皮肤数据 |

响应示例：

```json
{
  "success": true,
  "data": {
    "ownerUuid": "550e8400-e29b-41d4-a716-446655440000",
    "ownerName": "ExamplePlayer",
    "skinId": "steve",
    "value": "...",
    "signature": "...",
    "textureUrl": "https://textures.minecraft.net/texture/..."
  }
}
```

### 设置玩家皮肤

| 属性 | 值 |
|------|-----|
| 方法 | `POST` |
| 路径 | `/api/v1/players/{uuid}/skin` |
| 鉴权 | Bearer Token |
| 说明 | 按皮肤 ID 设置玩家皮肤 |

请求体：

```json
{
  "skinId": "alex"
}
```

### 清除玩家皮肤

| 属性 | 值 |
|------|-----|
| 方法 | `DELETE` |
| 路径 | `/api/v1/players/{uuid}/skin` |
| 鉴权 | Bearer Token |
| 说明 | 清除玩家自定义皮肤 |

### 踢出玩家

| 属性 | 值 |
|------|-----|
| 方法 | `POST` |
| 路径 | `/api/v1/admin/kick` |
| 鉴权 | Bearer Token |
| 说明 | 从代理端踢出指定玩家 |

请求体：

```json
{
  "uuid": "550e8400-e29b-41d4-a716-446655440000",
  "reason": "违反服务器规则"
}
```

### 封禁玩家

| 属性 | 值 |
|------|-----|
| 方法 | `POST` |
| 路径 | `/api/v1/admin/ban` |
| 鉴权 | Bearer Token |
| 说明 | 封禁指定玩家或 IP |

请求体：

```json
{
  "target": "550e8400-e29b-41d4-a716-446655440000",
  "type": "uuid",
  "durationMinutes": 60,
  "reason": "恶意攻击"
}
```

### 重置密码

| 属性 | 值 |
|------|-----|
| 方法 | `POST` |
| 路径 | `/api/v1/admin/reset-password` |
| 鉴权 | Bearer Token |
| 说明 | 为离线玩家生成临时密码 |

请求体：

```json
{
  "uuid": "550e8400-e29b-41d4-a716-446655440000"
}
```

### 发送 Webhook 测试

| 属性 | 值 |
|------|-----|
| 方法 | `POST` |
| 路径 | `/api/v1/admin/webhook/test` |
| 鉴权 | Bearer Token |
| 说明 | 触发一条测试 webhook |

### 指标端点

| 属性 | 值 |
|------|-----|
| 方法 | `GET` |
| 路径 | `/metrics` |
| 鉴权 | 否（建议通过反向代理限制访问） |
| 说明 | Prometheus 指标，由 Micrometer 输出 |

## 状态码

| HTTP 状态码 | 含义 |
|-------------|------|
| 200 | 请求成功 |
| 400 | 请求参数错误 |
| 401 | 未授权或 Token 无效 |
| 403 | 权限不足 |
| 404 | 玩家或资源不存在 |
| 409 | 资源冲突（如用户名已存在） |
| 500 | 服务器内部错误 |

## 安全建议

- 始终将 Javalin 绑定到 `127.0.0.1`，通过 Nginx/Traefik 反向代理提供 HTTPS。
- 使用强随机 Token，定期轮换。
- 对 `/metrics` 和 `/health` 按需配置访问控制。
