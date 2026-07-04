package io.github.addxiaoyi.starx.velocity.database;

import io.github.addxiaoyi.starx.velocity.config.StarxConfig;
import java.util.Objects;

/** 数据库连接管理器（占位实现）。 */
public final class DatabaseManager {

  private final StarxConfig config;

  public DatabaseManager(StarxConfig config) {
    this.config = Objects.requireNonNull(config, "config");
  }

  public void initialize() {
    // TODO: 初始化连接池与迁移
  }

  public void close() {
    // TODO: 关闭连接池
  }
}
