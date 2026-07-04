package io.github.addxiaoyi.starx.velocity.database;

import io.github.addxiaoyi.starx.common.config.DatabaseConfig;
import io.github.addxiaoyi.starx.common.database.JdbiUserRepository;
import java.util.Objects;

/** 数据库连接管理器，包装 {@link io.github.addxiaoyi.starx.common.database.DatabaseManager}。 */
public final class DatabaseManager {

  private final io.github.addxiaoyi.starx.common.database.DatabaseManager commonManager;
  private final JdbiUserRepository userRepository;
  private boolean closed;

  public DatabaseManager(DatabaseConfig config) {
    Objects.requireNonNull(config, "databaseConfig");
    this.commonManager = new io.github.addxiaoyi.starx.common.database.DatabaseManager(config);
    this.userRepository = new JdbiUserRepository(commonManager.getJdbi());
    this.closed = false;
  }

  public JdbiUserRepository getUserRepository() {
    return userRepository;
  }

  public boolean isOpen() {
    return !closed;
  }

  public void close() {
    if (!closed) {
      closed = true;
      commonManager.close();
    }
  }
}
