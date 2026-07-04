# 运维与 CI/CD

本节汇总 StarX 的持续集成工作流、代码质量检查与构建运维命令。

## CI/CD 工作流

仓库使用 GitHub Actions，配置文件位于 `.github/workflows/ci.yml`。

### 触发条件

- `push` 到 `main` 或 `master` 分支
- `pull_request`

### 工作流步骤

1. Checkout 代码
2. 设置 JDK 21
3. 使用 Gradle Wrapper 构建项目
4. 运行测试
5. 执行 Spotless 代码格式检查
6. 生成 Jacoco 覆盖率报告
7. 上传 ShadowJar 构建产物

## 常用命令

```bash
# 完整构建
./gradlew build

# 代码格式检查
./gradlew spotlessCheck

# 自动修复格式
./gradlew spotlessApply

# 测试
./gradlew test

# 覆盖率报告
./gradlew jacocoTestReport
```

## 构建产物

| 产物 | 路径 |
|------|------|
| Velocity ShadowJar | `starx-velocity/build/libs/starx-velocity-*-all.jar` |
| Paper ShadowJar | `starx-paper/build/libs/starx-paper-*-all.jar` |

## 代码质量

- Java：Google Java Format 1.25.2
- Kotlin/Gradle 脚本：ktlint 1.5.0
- Markdown：去 trailing whitespace、补换行

## 覆盖率聚合

根项目配置 `jacoco-report-aggregation` 插件，聚合所有子模块覆盖率。聚合报告位于 `build/reports/jacoco/testCodeCoverageReport/html/index.html`。
