# TDD 开发指南

StarX 采用测试驱动开发（TDD）作为核心开发流程。

## 测试模块

`starx-testfixtures` 提供：

- `InMemoryUserRepository`
- `InMemorySkinRepository`
- `RecordingEventBus`
- DTO 工厂类

## 示例

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

    assertThat(repository.findByUuid(user.uuid()))
        .isPresent()
        .hasValueSatisfying(u -> assertThat(u.username()).isEqualTo("Alice"));
  }
}
```

## 覆盖率

```bash
./gradlew jacocoTestReport
```

报告：`build/reports/jacoco/test/html/index.html`。

## 完整文档

详见仓库 `docs/development/tdd-guide.md`。
