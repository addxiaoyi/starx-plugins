# 模块加载器设计

StarX 在 Velocity 与 Paper 两端均以单插件形式部署，但内部按模块组织生命周期。模块加载器负责按依赖顺序初始化各组件，并在关闭时反向释放资源。

## 设计目标

1. **显式依赖**：每个模块声明自己依赖的其他模块，避免隐式初始化顺序。
2. **优雅降级**：可选模块（如 SkinsRestorer、LimboAPI）不存在时自动跳过，不影响核心流程。
3. **统一生命周期**：所有模块遵循 `init -> start -> stop -> close` 四个阶段。
4. **可测试性**：模块加载器可在单元测试中替换为手动组装的模块集合。

## 模块定义

```java
public interface StarxModule {

  String name();

  Set<Class<? extends StarxModule>> dependencies();

  default void init(StarxContext context) {}

  default void start() {}

  default void stop() {}

  default void close() {}
}
```

## 加载顺序

```text
1. 扫描并注册所有 StarxModule 实现
2. 根据 dependencies() 构建有向无环图（DAG）
3. 拓扑排序，检测循环依赖并报错
4. 按顺序执行 init -> start
5. 服务器关闭时按逆序执行 stop -> close
```

## 核心模块清单

| 模块 | 依赖 | 说明 |
|------|------|------|
| ConfigModule | - | 加载 `config.yml` 与 `database.yml` |
| DatabaseModule | ConfigModule | 初始化 HikariCP、JDBI、Flyway |
| EventBusModule | - | 注册平台事件总线实现 |
| RepositoryModule | DatabaseModule, EventBusModule | 实例化 `UserRepository`、`SkinRepository` |
| WebhookModule | ConfigModule | 配置出站 Webhook 签名与重试 |
| HttpApiModule | ConfigModule, RepositoryModule | 启动 Javalin HTTP 服务 |
| MetricsModule | ConfigModule | 注册 Micrometer Prometheus 注册表 |
| VelocityAuthModule | RepositoryModule, LimboModule（可选） | 处理登录事件与认证路由 |
| LimboModule | ConfigModule（可选） | 集成 LimboAPI 的验证大厅 |
| SkinBridgeModule | RepositoryModule, SkinsRestorer 软依赖 | 皮肤同步与桥接 |
| PaperMessagingModule | RepositoryModule, SkinBridgeModule | 接收 Plugin Messaging 消息 |

## 可选模块降级

- **LimboAPI**：未安装时，VelocityAuthModule 直接通过 Velocity 原生事件完成快速认证，不进入 Limbo 大厅。
- **SkinsRestorer**：未安装时，SkinBridgeModule 仅使用本地仓库数据；若本地也无数据，则保持玩家默认皮肤。

## 错误处理

- 循环依赖：在启动阶段抛出 `IllegalStateException`，打印涉及的模块名。
- 必要模块缺失：直接阻止插件启用，并输出诊断日志。
- 可选模块初始化失败：记录警告，继续启动其余模块。
