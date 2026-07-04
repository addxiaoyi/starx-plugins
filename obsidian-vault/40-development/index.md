# 开发实践

## TDD 核心流程

1. 红：编写失败的测试。
2. 绿：编写最小实现。
3. 重构：优化结构。
4. 重复。

## 测试层级

| 层级 | 工具 |
|------|------|
| 单元测试 | JUnit 5 + Mockito + AssertJ |
| 集成测试 | Testcontainers + H2 |
| 组件测试 | `starx-testfixtures` |

## 常用命令

```bash
./gradlew test
./gradlew spotlessCheck
./gradlew spotlessApply
./gradlew jacocoTestReport
```

## 子主题

- [[tdd-guide|TDD 开发指南]]

## 完整文档

详见仓库 `docs/development/tdd-guide.md`。
