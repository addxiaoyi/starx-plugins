# SkinsRestorer 集成说明

StarX 通过 `starx-api` 中的 `SkinRepository` 抽象皮肤数据源，并在 `starx-velocity` 与 `starx-paper` 中分别对接 SkinsRestorer API。该设计允许在 SkinsRestorer 不可用时优雅降级到本地存储。

## 设计目标

1. **解耦**：`starx-api` 与 `starx-common` 不直接依赖 SkinsRestorer。
2. **可测试**：单元测试可使用 `InMemorySkinRepository` 替代真实 API。
3. **降级**：SkinsRestorer 未安装或不可用时，使用本地数据库缓存的皮肤数据。

## 核心抽象

```java
public interface SkinRepository {
  Optional<SkinDto> findByPlayer(UUID uuid, String name);

  void setSkinId(UUID uuid, String skinId);

  void setSkinData(UUID uuid, String value, String signature);

  void clearSkin(UUID uuid);
}
```

## 集成架构

```text
┌─────────────────┐     ┌─────────────────────────┐     ┌─────────────────┐
│   starx-api     │     │      starx-common       │     │ 外部：SkinsRestorer │
│ SkinRepository  │◄────│ DefaultSkinRepository   │────►│     API         │
│    (interface)  │     │ (本地数据库 + API 桥接)  │     │                 │
└─────────────────┘     └─────────────────────────┘     └─────────────────┘
         ▲                       ▲
         │                       │
┌─────────────────┐     ┌─────────────────────────┐
│ starx-velocity  │     │      starx-paper        │
│ VelocitySkin    │     │    PaperSkinBridge      │
│   Bridge        │     │                         │
└─────────────────┘     └─────────────────────────┘
```

## Velocity 端集成

`starx-velocity` 在构建时以 `compileOnly` 方式依赖 `skinsrestorer-api`：

```kotlin
compileOnly(libs.skinsrestorer.api)
```

运行时逻辑：

1. 玩家登录成功后，调用 `SkinRepository.findByPlayer(uuid, name)`。
2. `DefaultSkinRepository` 优先查询本地数据库。
3. 本地无数据且 SkinsRestorer 可用时，调用 SkinsRestorer API 获取皮肤。
4. 将结果写入本地缓存，并发布 `skin:applied` 事件。
5. 通过 Plugin Messaging 发送 `skin_sync` 到玩家所在 Paper 后端。

## Paper 端集成

`starx-paper` 同样以 `compileOnly` 方式依赖 `skinsrestorer-api`：

```kotlin
compileOnly(libs.skinsrestorer.api)
```

运行时逻辑：

1. 接收到 `skin_sync` 消息后，解析 `SkinDto`。
2. 调用 SkinsRestorer API 将皮肤应用到玩家实体。
3. 若 SkinsRestorer 不可用，仅更新本地缓存，等待后续重试。

## 皮肤数据对象

```java
public final class SkinDto {
  private final UUID ownerUuid;
  private final String ownerName;
  private final String skinId;
  private final String value;
  private final String signature;
  private final String textureUrl;
}
```

## 降级策略

| 场景 | 行为 |
|------|------|
| SkinsRestorer 已安装且在线 | 使用 API 实时查询并应用 |
| SkinsRestorer 未安装 | 仅使用本地数据库缓存 |
| API 调用失败 | 返回本地缓存，记录失败指标 |
| 本地无缓存 | 保持玩家默认皮肤 |

## 事件

| 事件 | 触发时机 |
|------|----------|
| `skin:refresh:request` | 玩家或管理员请求刷新皮肤 |
| `skin:applied` | 皮肤已成功应用到玩家 |
| `skin:updated` | 本地皮肤数据已更新 |

## 配置项

在 `config.yml` 中可配置：

```yaml
skins:
  enabled: true
  fallbackToLocal: true
  syncToBackend: true
  apiTimeoutMs: 5000
```

## 版本兼容

当前依赖版本：

```toml
skinsrestorer = "15.12.4"
```

升级 SkinsRestorer 前，请确认 API 包路径与接口签名是否变更，必要时调整桥接实现。
