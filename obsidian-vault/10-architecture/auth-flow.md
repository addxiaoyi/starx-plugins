# 认证流程

StarX 认证流程运行在 Velocity 代理端，区分正版与离线玩家，并支持 LimboAPI 二次校验。

## 流程概览

```text
玩家连接 Velocity
   │
   ▼
LoginEvent
   │
   ▼
online-mode?
   ├─ true  ──► 正版路由 ──► Mojang 验证 ──► 进入后端
   └─ false ──► 离线路由 ──► Limbo 校验（可选）──► 进入后端
```

## 正版路由

1. Velocity 完成 Mojang 会话验证。
2. 拿到正版 UUID 与用户名。
3. 查询/创建 `UserDto`，标记 `premium=true`。
4. 发布 `player:login:success`。

## 离线路由

1. 玩家以离线 UUID 进入。
2. 根据配置决定是否进入 Limbo 大厅。
3. 完成密码/TOTP/验证码校验。
4. 发布 `player:login:success`。

## 安全策略

- 同一 IP 登录失败超限临时封禁。
- 支持 TOTP 二次验证。
- 密码使用 BCrypt，TOTP 密钥加密保存。

## 相关事件

| 事件 | 触发时机 |
|------|----------|
| `player:login:start` | 登录开始 |
| `player:login:success` | 登录成功 |
| `player:login:failed` | 登录失败 |
| `security:alert` | 安全告警 |

## 完整文档

详见仓库 `docs/architecture/auth-flow.md`。
