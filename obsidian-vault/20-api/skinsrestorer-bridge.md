# SkinsRestorer 集成说明

StarX 通过 `SkinRepository` 抽象皮肤数据源，在 Velocity 与 Paper 两端分别桥接 SkinsRestorer API。

## 核心抽象

```java
public interface SkinRepository {
  Optional<SkinDto> findByPlayer(UUID uuid, String name);
  void setSkinId(UUID uuid, String skinId);
  void setSkinData(UUID uuid, String value, String signature);
  void clearSkin(UUID uuid);
}
```

## 依赖

```kotlin
compileOnly(libs.skinsrestorer.api) // 15.12.4
```

## 降级策略

| 场景 | 行为 |
|------|------|
| SkinsRestorer 在线 | 使用 API 实时查询 |
| SkinsRestorer 未安装 | 使用本地缓存 |
| API 失败 | 返回本地缓存 |
| 本地无缓存 | 保持默认皮肤 |

## 完整文档

详见仓库 `docs/api/skinsrestorer-bridge.md`。
