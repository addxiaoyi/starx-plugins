# TDD 开发指南

StarX 采用测试驱动开发（TDD）作为核心开发流程。本指南说明如何在多模块 Gradle 项目中编写、运行与维护测试。

## 测试策略

| 层级 | 范围 | 工具 | 速度 |
|------|------|------|------|
| 单元测试 | 单个类或方法 | JUnit 5 + Mockito + AssertJ | 快 |
| 集成测试 | 仓库 + 数据库 | Testcontainers + H2 | 中 |
| 组件测试 | 模块加载器组合 | `starx-testfixtures` | 中 |
| 端到端测试 | 完整插件 | 手动/未来扩展 | 慢 |

## 测试模块

### starx-testfixtures

提供可在多个模块复用的测试辅助类：

- `InMemoryUserRepository`：内存实现 `UserRepository`，支持断言查询次数。
- `InMemorySkinRepository`：内存实现 `SkinRepository`。
- `RecordingEventBus`：记录所有发布的事件，便于断言。
- `UserDtoFactory`、`SkinDtoFactory`：快速构造测试数据。

依赖方式：

```kotlin
testImplementation(project(":starx-testfixtures"))
```

## TDD 循环

1. **红**：编写一个失败的测试，明确期望行为。
2. **绿**：编写最小实现让测试通过。
3. **重构**：在不改变行为的前提下优化代码结构。
4. **重复**：继续下一个需求。

## 编写单元测试示例

```java
class DefaultUserRepositoryTest {

  private InMemoryUserRepository repository;

  @BeforeEach
  void setUp() {
    repository = new InMemoryUserRepository();
  }

  @Test
  void shouldFindSavedUserByUuid() {
    UserDto user = UserDto.builder()
        .uuid(UUID.randomUUID())
        .username("Alice")
        .premium(false)
        .build();

    repository.save(user);

    Optional<UserDto> found = repository.findByUuid(user.uuid());

    assertThat(found).isPresent();
    assertThat(found.get().username()).isEqualTo("Alice");
  }
}
```

## 事件测试

使用 `RecordingEventBus` 断言事件发布：

```java
@Test
void shouldPublishLoginSuccessEvent() {
  RecordingEventBus eventBus = new RecordingEventBus();
  AuthService authService = new AuthService(eventBus, repository);

  authService.authenticate(player);

  assertThat(eventBus.findEvents(EventTypes.PLAYER_LOGIN_SUCCESS))
      .hasSize(1);
}
```

## 数据库集成测试

```java
@Testcontainers
class UserRepositoryIntegrationTest {

  @Container
  static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
      .withDatabaseName("starx");

  @Test
  void shouldPersistUserInMySQL() {
    // 初始化仓库并执行测试
  }
}
```

## 运行测试

```bash
# 运行所有测试
./gradlew test

# 运行指定模块测试
./gradlew :starx-common:test

# 持续测试
./gradlew test --continuous
```

## 代码覆盖率

项目已配置 Jacoco，运行：

```bash
./gradlew jacocoTestReport
```

报告位于各模块 `build/reports/jacoco/test/html/index.html`。根项目通过 `jacoco-report-aggregation` 聚合所有模块覆盖率。

## 代码格式化

提交前必须运行：

```bash
./gradlew spotlessCheck
```

若检查失败，可自动修复：

```bash
./gradlew spotlessApply
```

Spotless 配置覆盖：

- Java：Google Java Format 1.25.2
- Kotlin DSL/Gradle 脚本：ktlint 1.5.0
- Markdown：去 trailing whitespace、补换行

## CI 检查

CI 会执行以下命令：

```bash
./gradlew build
./gradlew spotlessCheck
./gradlew jacocoTestReport
```

任何一步失败都会阻止合并。

## 测试命名规范

- 测试类：`<被测类>Test` 或 `<被测类>IntegrationTest`。
- 测试方法：`should<期望行为>When<条件>`，例如 `shouldReturnEmptyWhenUserNotFound`。

## 注意事项

- 不要修改 Java 源代码来实现测试需求；测试应驱动实现，而不是绕过实现。
- 单元测试避免访问真实网络或数据库。
- 需要平台 API 的代码应在接口层测试，平台实现可通过 Mockito 模拟。
