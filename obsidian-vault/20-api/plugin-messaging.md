# Plugin Messaging 通道与消息格式

所有 StarX 内部消息通过通道 `starx:main` 传输，消息体为 JSON。

## 消息结构

```json
{
  "command": "<子命令>",
  "payload": { }
}
```

## 子命令

| 子命令 | 方向 | 说明 |
|--------|------|------|
| `skin_sync` | Velocity → Paper | 同步皮肤数据 |
| `player_state` | Velocity → Paper | 同步登录/登出/切服状态 |
| `config_sync` | Velocity → Paper | 配置热同步 |
| `security_alert` | Velocity → Paper | 安全告警广播 |

## 处理接口

```java
public interface PluginMessageHandler {
  void handle(PluginMessage message);
}
```

## 完整文档

详见仓库 `docs/api/plugin-messaging.md`。
