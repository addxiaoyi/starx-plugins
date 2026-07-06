package io.github.addxiaoyi.starx.common.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.addxiaoyi.starx.common.config.DatabaseConfig;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 管理 HikariCP 连接池、JDBI 与 Flyway 数据库迁移。 */
public final class DatabaseManager implements AutoCloseable {

  private static final Logger LOG = LoggerFactory.getLogger(DatabaseManager.class);

  private final HikariDataSource dataSource;
  private final Jdbi jdbi;

  public DatabaseManager(DatabaseConfig config) {
    loadJdbcDrivers();

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

    if (isSqlite) {
      configureSqlite();
    }

    migrate();
    verifyTables();

    this.jdbi = Jdbi.create(this.dataSource).installPlugin(new SqlObjectPlugin());
  }

  private void verifyTables() {
    try (Connection conn = dataSource.getConnection();
        ResultSet rs = conn.getMetaData().getTables(null, null, "starx_users", null)) {
      if (!rs.next()) {
        LOG.warn("starx_users table not found after Flyway migration, creating directly");
        try (Statement stmt = conn.createStatement()) {
          stmt.execute(
              "CREATE TABLE IF NOT EXISTS starx_users ("
                  + "uuid VARCHAR(36) PRIMARY KEY, username VARCHAR(255) NOT NULL, "
                  + "email VARCHAR(255), password_hash VARCHAR(255), totp_secret VARCHAR(255), "
                  + "premium BOOLEAN NOT NULL DEFAULT FALSE, created_at TIMESTAMP NOT NULL, "
                  + "last_login_at TIMESTAMP, external_user_id VARCHAR(255), "
                  + "trusted_devices TEXT, recovery_codes VARCHAR(512) DEFAULT NULL, "
                  + "source_system VARCHAR(50), migration_state VARCHAR(20), "
                  + "password_migrated_at TIMESTAMP, last_login_ip VARCHAR(255), "
                  + "last_login_isp VARCHAR(255), last_login_location VARCHAR(255), "
                  + "total_playtime BIGINT DEFAULT 0, last_logout_at TIMESTAMP, "
                  + "welcome_message_shown BOOLEAN DEFAULT FALSE)");
          stmt.execute(
              "CREATE UNIQUE INDEX IF NOT EXISTS idx_starx_users_username ON starx_users(username)");
          stmt.execute(
              "CREATE UNIQUE INDEX IF NOT EXISTS idx_starx_users_email ON starx_users(email)");
        }
        LOG.info("starx_users table created via direct fallback");
      } else {
        LOG.info("starx_users table exists after Flyway migration");
      }
    } catch (Exception e) {
      LOG.error("Failed to verify database tables", e);
    }
  }

  /** 手动加载可能被 relocate 的 JDBC 驱动类。 */
  private static void loadJdbcDrivers() {
    try {
      Class.forName("org.sqlite.JDBC");
    } catch (ClassNotFoundException ignored) {
    }
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
    try {
      Flyway flyway =
          Flyway.configure()
              .dataSource(dataSource)
              .locations("classpath:db/migration")
              .load();
      var result = flyway.migrate();
      LOG.info("Flyway migration completed: {} migrations applied", result.migrations.size());
    } catch (Exception e) {
      LOG.error("Flyway migration failed", e);
      throw e;
    }
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
