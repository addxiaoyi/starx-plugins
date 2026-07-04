# API 与协议

StarX 通过三种方式与外部系统交互：HTTP API、Plugin Messaging、SkinsRestorer API。

## 子主题

- [[rest-endpoints|HTTP API 端点]]
- [[plugin-messaging|Plugin Messaging 通道与消息格式]]
- [[skinsrestorer-bridge|SkinsRestorer 集成说明]]

## HTTP API 基础

- 默认地址：`127.0.0.1:8788`
- 鉴权：`Authorization: Bearer <token>`
- 序列化：JSON

## Plugin Messaging 主通道

- 通道 ID：`starx:main`
- 编码：UTF-8 JSON
- 子命令：`skin_sync`、`player_state`、`config_sync`、`security_alert`

## SkinsRestorer 桥接

- 通过 `SkinRepository` 抽象。
- Velocity 与 Paper 均以 `compileOnly` 依赖 `skinsrestorer-api:15.12.4`。
- 支持 SkinsRestorer 缺失时降级到本地缓存。
