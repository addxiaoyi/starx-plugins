# 架构设计

## 分层架构

```text
平台专用实现层：starx-velocity / starx-paper
跨平台实现层：starx-common
公共契约层：starx-api
测试辅助层：starx-testfixtures
```

依赖方向严格向下：`starx-velocity` / `starx-paper` → `starx-common` → `starx-api`。

## 子主题

- [[overview|模块架构总览]]
- [[module-loader|模块加载器设计]]
- [[auth-flow|认证流程：Limbo、正版/离线路由]]

## 关键接口

- `EventBus`：平台无关事件总线。
- `StarxEvent`：事件 envelope，含 type、eventId、timestamp、payload。
- `PluginMessage`：Velocity 与 Paper 间消息 envelope。
- `UserRepository`、`SkinRepository`：仓库接口，解耦实现。

## 事件类型速查

| 事件 | 说明 |
|------|------|
| `player:login:start` | 登录开始 |
| `player:login:success` | 登录成功 |
| `player:login:failed` | 登录失败 |
| `player:logout` | 玩家登出 |
| `security:alert` | 安全告警 |
| `skin:applied` | 皮肤应用 |
