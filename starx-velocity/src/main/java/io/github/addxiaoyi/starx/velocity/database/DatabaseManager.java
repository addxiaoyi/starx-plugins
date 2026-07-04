package io.github.addxiaoyi.starx.velocity.database;

import io.github.addxiaoyi.starx.api.dto.UserDto;
import io.github.addxiaoyi.starx.api.repository.UserRepository;
import io.github.addxiaoyi.starx.velocity.config.StarxConfig;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 数据库连接管理器（占位实现）。 */
public final class DatabaseManager {

  private final StarxConfig config;
  private final UserRepository userRepository;

  public DatabaseManager(StarxConfig config) {
    this.config = config;
    this.userRepository = new InMemoryUserRepository();
  }

  public void initialize() {
    // TODO: 初始化连接池与迁移
  }

  public void close() {
    // TODO: 关闭连接池
  }

  public UserRepository getUserRepository() {
    return userRepository;
  }

  private static final class InMemoryUserRepository implements UserRepository {
    private final ConcurrentHashMap<UUID, UserDto> users = new ConcurrentHashMap<>();

    @Override
    public Optional<UserDto> findByUuid(UUID uuid) {
      return Optional.ofNullable(users.get(uuid));
    }

    @Override
    public Optional<UserDto> findByUsername(String username) {
      return users.values().stream().filter(u -> u.username().equals(username)).findFirst();
    }

    @Override
    public Optional<UserDto> findByEmail(String email) {
      return users.values().stream()
          .filter(u -> u.email() != null && u.email().equals(email))
          .findFirst();
    }

    @Override
    public boolean existsByUsername(String username) {
      return users.values().stream().anyMatch(u -> u.username().equals(username));
    }

    @Override
    public void save(UserDto user) {
      users.put(user.uuid(), user);
    }

    @Override
    public void delete(UUID uuid) {
      users.remove(uuid);
    }
  }
}
