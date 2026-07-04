# 认证流程

StarX 的认证流程运行在 Velocity 代理端，负责区分正版玩家与离线玩家，并在必要时通过 LimboAPI 将玩家送入验证大厅进行二次校验。

## 总体流程

```text
玩家连接 Velocity
        │
        ▼
PreLoginEvent / LoginEvent
        │
        ▼
判断服务器 online-mode 配置
        │
        ├── online-mode=true ──► 正版路由 ──► Mojang 会话验证
        │                                          │
        │                                          ▼
        │                              验证成功：写入 UserDto.premium=true
        │                                          │
        │                                          ▼
        │                              进入后端服务器
        │
        └── online-mode=false ──► 离线路由 ──► 可选 Limbo 校验
                                                   │
                                                   ▼
                                      LimboAPI 将玩家送入验证大厅
                                                   │
                                                   ▼
                                      完成密码/TOTP/验证码校验
                                                   │
                                                   ▼
                                      放行到后端服务器
```

## 正版路由

当 Velocity `config.yml` 中 `online-mode=true` 时：

1. Velocity 自动完成 Mojang 会话验证。
2. `VelocityAuthModule` 在 `GameProfileRequestEvent` 阶段拿到正版 UUID 与用户名。
3. 查询 `UserRepository`：
   - 玩家已存在：更新 `lastLoginAt`。
   - 玩家不存在：创建新记录，`premium=true`。
4. 发布 `player:login:success` 事件。
5. 玩家被路由至默认后端或上一次所在后端。

## 离线路由

当 Velocity `config.yml` 中 `online-mode=false` 时：

1. 玩家以离线 UUID 进入代理端。
2. `VelocityAuthModule` 拦截登录事件，根据配置决定是否进入 Limbo 大厅。
3. 若启用 LimboAPI：
   - 玩家被传送至 Limbo 大厅。
   - 在大厅中通过 StarX 命令或消息完成密码/TOTP/验证码校验。
   - 校验成功后，LimboAPI 将玩家释放到目标后端服务器。
4. 若未启用 LimboAPI：
   - 直接查询本地数据库进行用户名/密码校验。
   - 校验成功后放行。
5. 发布 `player:login:success` 事件。

## Limbo 大厅

Limbo 大厅用于离线玩家的二次校验，避免未认证玩家直接进入真实后端。

```text
进入 Limbo
   │
   ▼
显示验证码 / 密码输入界面（可通过命令或聊天交互）
   │
   ▼
StarX 校验凭据
   │
   ├── 失败：提示并重试，超过阈值触发 security:alert
   └── 成功：记录登录状态并放行
```

## 账号绑定

离线玩家可通过网站后端绑定外部账号（如论坛账号、Discord）。绑定成功后，网站后端发送 webhook 或在游戏内通过命令触发 `LINK_EXTERNAL_USER` 事件，将 `externalUserId` 写入 `UserDto`。

## 事件列表

| 事件 | 触发时机 |
|------|----------|
| `player:login:start` | 玩家开始登录，尚未完成认证 |
| `player:login:success` | 认证成功，即将进入后端 |
| `player:login:failed` | 认证失败，含失败原因 |
| `player:logout` | 玩家断开连接 |
| `player:register` | 新离线玩家注册完成 |
| `security:alert` | 登录异常（暴力破解、IP 异常等） |
| `security:bot:failed` | 机器人检测失败 |

## 安全策略

- 同一 IP 短时间内登录失败次数超过阈值，临时封禁该 IP。
- 支持 TOTP 二次验证，管理员可在配置中强制开启。
- 密码使用 BCrypt 存储，TOTP 密钥加密保存。
