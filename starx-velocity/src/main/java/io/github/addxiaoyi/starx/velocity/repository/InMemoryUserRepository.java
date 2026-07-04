package io.github.addxiaoyi.starx.velocity.repository;

import io.github.addxiaoyi.starx.api.dto.UserDto;
import io.github.addxiaoyi.starx.api.repository.UserRepository;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** {@link UserRepository} 的内存占位实现。认证服务与数据库就绪前用于本地开发与测试。 */
public final class InMemoryUserRepository implements UserRepository {

  private final Map<UUID, UserDto> usersByUuid = new ConcurrentHashMap<>();
  private final Map<String, UUID> uuidByUsername = new ConcurrentHashMap<>();
  private final Map<String, UUID> uuidByEmail = new ConcurrentHashMap<>();

  @Override
  public Optional<UserDto> findByUuid(UUID uuid) {
    return Optional.ofNullable(usersByUuid.get(uuid));
  }

  @Override
  public Optional<UserDto> findByUsername(String username) {
    return Optional.ofNullable(uuidByUsername.get(username)).map(usersByUuid::get);
  }

  @Override
  public Optional<UserDto> findByEmail(String email) {
    return Optional.ofNullable(uuidByEmail.get(email)).map(usersByUuid::get);
  }

  @Override
  public boolean existsByUsername(String username) {
    return uuidByUsername.containsKey(username);
  }

  @Override
  public void save(UserDto user) {
    usersByUuid.put(user.uuid(), user);
    uuidByUsername.put(user.username(), user.uuid());
    if (user.email() != null && !user.email().isBlank()) {
      uuidByEmail.put(user.email(), user.uuid());
    }
  }

  @Override
  public void delete(UUID uuid) {
    UserDto removed = usersByUuid.remove(uuid);
    if (removed != null) {
      uuidByUsername.remove(removed.username());
      if (removed.email() != null) {
        uuidByEmail.remove(removed.email());
      }
    }
  }
}
