# Plugin Messaging 通道与消息格式

StarX 使用 Minecraft Plugin Messaging 机制在 Velocity 代理端与 Paper 后端之间传输控制命令与状态同步数据。所有消息均通过单一主通道 `starx:main` 发送，消息体内部通过 `command` 字段区分子类型。

## 主通道

| 属性 | 值 |
|------|-----|
| 通道 ID | `starx:main` |
| 方向 | Velocity ⇄ Paper |
| 编码 | UTF-8 字符串 JSON |

## 消息结构

```json
{
  "command": "<子命令>",
  "payload": {
    ...
  }
}
```

Java 对应类：`io.github.addxiaoyi.starx.api.messaging.PluginMessage`

## 子命令列表

### `skin_sync`

同步皮肤数据。通常由 Velocity 在皮肤变更后推送给所有在线 Paper 后端。

Payload：

```json
{
  "ownerUuid": "550e8400-e29b-41d4-a716-446655440000",
  "ownerName": "ExamplePlayer",
  "skinId": "alex",
  "value": "...",
  "signature": "...",
  "textureUrl": "https://textures.minecraft.net/texture/..."
}
```

### `player_state`

同步玩家登录/登出/切换状态，便于后端更新缓存或触发事件。

Payload：

```json
{
  "uuid": "550e8400-e29b-41d4-a716-446655440000",
  "username": "ExamplePlayer",
  "state": "LOGIN",
  "serverId": "lobby-01",
  "timestamp": "2024-06-01T12:00:00Z"
}
```

`state` 可选值：`LOGIN`、`LOGOUT`、`SERVER_SWITCH`。

### `config_sync`

当 Velocity 配置热重载时，将相关配置片段广播给后端。

Payload：

```json
{
  "section": "features",
  "values": {
    "skinSyncEnabled": true,
    "lobbyKickEnabled": false
  }
}
```

### `security_alert`

安全告警广播，例如检测到暴力破解或异常登录。

Payload：

```json
{
  "alertType": "BRUTE_FORCE",
  "target": "ExamplePlayer",
  "sourceIp": "192.0.2.1",
  "details": "5 分钟内失败 10 次",
  "timestamp": "2024-06-01T12:00:00Z"
}
```

## 消息处理

Velocity 与 Paper 分别实现 `PluginMessageHandler`：

```java
public interface PluginMessageHandler {
  void handle(PluginMessage message);
}
```

处理流程：

1. 接收字节数组，使用 UTF-8 解码为 JSON 字符串。
2. 反序列化为 `PluginMessage` 对象。
3. 根据 `command` 字段路由到对应处理器。
4. 处理完成后可选择回发一条响应消息。

## 常量定义

```java
public final class PluginMessageChannels {
  public static final String MAIN = "starx:main";
  public static final String CMD_SKIN_SYNC = "skin_sync";
  public static final String CMD_PLAYER_STATE = "player_state";
  public static final String CMD_CONFIG_SYNC = "config_sync";
  public static final String CMD_SECURITY_ALERT = "security_alert";
}
```

## 注意事项

- Plugin Messaging 有长度限制，皮肤 value/signature 较大时应避免频繁全量同步，可改用皮肤 ID 让后端自行查询。
- 跨后端通信必须经过 Velocity 转发，Paper 之间不能直接通信。
- 所有消息应做好异常捕获，避免单个坏消息导致通道关闭。
