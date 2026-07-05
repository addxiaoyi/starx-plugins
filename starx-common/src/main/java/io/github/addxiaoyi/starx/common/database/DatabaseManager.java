package io.github.addxiaoyi.starx.common.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.addxiaoyi.starx.common.config.DatabaseConfig;
import java.sql.Connection;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

/** 管理 HikariCP 连接池、JDBI 与 Flyway 数据库迁移。 */
public final class DatabaseManager implements AutoCloseable {

  private final HikariDataSource dataSource;
  private final Jdbi jdbi;

  public DatabaseManager(DatabaseConfig config) {
    HikariConfig hikariConfig = new HikariConfig();
    hikariConfig.setJdbcUrl(config.jdbcUrl());
    hikariConfig.setUsername(config.username());
    hikariConfig.setPassword(config.password());
    hikariConfig.setConnectionTimeout(config.connectionTimeoutMs());

    boolean isH2 = config.jdbcUrl().startsWith("jdbc:h2:");
    boolean isSqlite = config.isSqlite();
    if (isH2) {
      hikariConfig.setMaximumPoolSize(Math.min(config.poolMaxSize(), 3));
      hikariConfig.setMinimumIdle(1);
    } else if (isSqlite) {
      hikariConfig.setMaximumPoolSize(Math.min(config.poolMaxSize(), 2));
      hikariConfig.setMinimumIdle(1);
    } else {
      hikariConfig.setMaximumPoolSize(config.poolMaxSize());
      hikariConfig.setMinimumIdle(2);
    }
    hikariConfig.setPoolName("starx-common-pool");

    this.dataSource = new HikariDataSource(hikariConfig);
    this.jdbi = Jdbi.create(this.dataSource).installPlugin(new SqlObjectPlugin());

    if (isSqlite) {
      configureSqlite();
    }

    migrate();
  }

  private void configureSqlite() {
    try (Connection conn = dataSource.getConnection();
        Statement stmt = conn.createStatement()) {
      stmt.execute("PRAGMA journal_mode = WAL");
      stmt.execute("PRAGMA synchronous = NORMAL");
      stmt.execute("PRAGMA foreign_keys = ON");
      stmt.execute("PRAGMA busy_timeout = 5000");
    } catch (Exception e) {
      throw new RuntimeException("Failed to configure SQLite", e);
    }
  }

  private void migrate() {
    Flyway flyway =
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load();
    flyway.migrate();
  }

  public Jdbi getJdbi() {
    return jdbi;
  }

  public HikariDataSource getDataSource() {
    return dataSource;
  }

  @Override
  public void close() {
    dataSource.close();
  }
}
