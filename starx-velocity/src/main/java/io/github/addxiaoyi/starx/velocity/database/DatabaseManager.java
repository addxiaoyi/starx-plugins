package io.github.addxiaoyi.starx.velocity.database;

import io.github.addxiaoyi.starx.common.config.DatabaseConfig;
import io.github.addxiaoyi.starx.common.database.JdbcUserRepository;
import java.util.Objects;

public final class DatabaseManager {

  private final io.github.addxiaoyi.starx.common.database.DatabaseManager commonManager;
  private final JdbcUserRepository userRepository;
  private boolean closed;

  public DatabaseManager(DatabaseConfig config) {
    Objects.requireNonNull(config, "databaseConfig");
    this.commonManager = new io.github.addxiaoyi.starx.common.database.DatabaseManager(config);
    this.userRepository = new JdbcUserRepository(commonManager.getDataSource());
    this.closed = false;
  }

  public io.github.addxiaoyi.starx.common.database.DatabaseManager commonManager() {
    return commonManager;
  }

  public JdbcUserRepository getUserRepository() {
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
