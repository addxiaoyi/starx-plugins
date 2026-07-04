# 部署与配置

本节汇总 StarX 的端口规划、安装步骤与配置项参考。

## 子主题

- [[ports|物理端口配置说明]]
- [[installation|安装步骤]]
- [[config-reference|配置项参考]]

## 环境要求

- Java 21+
- Velocity 3.5.0-SNAPSHOT
- Paper 1.21.4 / Folia
- 可选：LimboAPI 1.1.26
- 可选：SkinsRestorer 15.12.4

## 默认端口

| 服务 | 地址 |
|------|------|
| StarX HTTP API | `127.0.0.1:8788` |
| Velocity | `0.0.0.0:25577` |
| Paper/Folia | `0.0.0.0:25565` |

## 快速安装

```bash
./gradlew build
cp starx-velocity/build/libs/starx-velocity-*-all.jar <velocity>/plugins/
cp starx-paper/build/libs/starx-paper-*-all.jar <paper>/plugins/
```

然后启动服务端，编辑生成的 `config.yml` 并重启。
