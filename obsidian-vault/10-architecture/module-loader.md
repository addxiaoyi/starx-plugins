# 模块加载器设计

StarX 在 Velocity 与 Paper 两端以单插件形式部署，内部按模块组织生命周期。

## 生命周期

所有模块遵循四个阶段：

1. `init`：读取配置、建立连接。
2. `start`：注册监听器、启动服务。
3. `stop`：停止接收新请求。
4. `close`：释放资源。

## 核心模块

| 模块 | 依赖 | 说明 |
|------|------|------|
| ConfigModule | - | 加载配置 |
| DatabaseModule | ConfigModule | 数据库连接池与迁移 |
| EventBusModule | - | 事件总线实现 |
| RepositoryModule | DatabaseModule, EventBusModule | 仓库实现 |
| HttpApiModule | ConfigModule, RepositoryModule | Javalin HTTP 服务 |
| MetricsModule | ConfigModule | Prometheus 指标 |
| VelocityAuthModule | RepositoryModule, LimboModule（可选） | 认证路由 |
| SkinBridgeModule | RepositoryModule, SkinsRestorer 软依赖 | 皮肤桥接 |
| PaperMessagingModule | RepositoryModule, SkinBridgeModule | 后端消息处理 |

## 降级策略

- **LimboAPI 缺失**：直接通过 Velocity 原生事件认证。
- **SkinsRestorer 缺失**：使用本地数据库缓存。

## 完整文档

详见仓库 `docs/architecture/module-loader.md`。
