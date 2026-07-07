package io.github.addxiaoyi.starx.common.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.addxiaoyi.starx.common.config.DatabaseConfig;
import java.sql.Connection;
import java.sql.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DatabaseManager implements AutoCloseable {

  private static final Logger LOG = LoggerFactory.getLogger(DatabaseManager.class);

  private final HikariDataSource dataSource;

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

    ensureTables();
  }

  private void ensureTables() {
    try (Connection conn = dataSource.getConnection();
        Statement stmt = conn.createStatement()) {
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
      stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_starx_users_email ON starx_users(email)");
      stmt.execute(
          "CREATE TABLE IF NOT EXISTS starx_punishments ("
              + "id VARCHAR(36) PRIMARY KEY, target_uuid VARCHAR(36) NOT NULL, "
              + "target_name VARCHAR(16) NOT NULL, type VARCHAR(16) NOT NULL, "
              + "reason VARCHAR(512), staff_uuid VARCHAR(36) NOT NULL, "
              + "staff_name VARCHAR(16) NOT NULL, created_at BIGINT NOT NULL, "
              + "expires_at BIGINT, active BOOLEAN NOT NULL DEFAULT TRUE)");
      stmt.execute(
          "CREATE TABLE IF NOT EXISTS starx_staff_notes ("
              + "id VARCHAR(36) PRIMARY KEY, target_uuid VARCHAR(36) NOT NULL, "
              + "note VARCHAR(1024) NOT NULL, severity VARCHAR(16) NOT NULL, "
              + "staff_uuid VARCHAR(36) NOT NULL, created_at BIGINT NOT NULL)");
      stmt.execute(
          "CREATE TABLE IF NOT EXISTS starx_reports ("
              + "id VARCHAR(36) PRIMARY KEY, reporter_uuid VARCHAR(36) NOT NULL, "
              + "target_uuid VARCHAR(36) NOT NULL, category VARCHAR(32) NOT NULL, "
              + "details VARCHAR(512), status VARCHAR(16) NOT NULL DEFAULT 'PENDING', "
              + "resolved_by VARCHAR(36), resolved_at BIGINT)");
      stmt.execute(
          "CREATE TABLE IF NOT EXISTS starx_announcements ("
              + "id VARCHAR(36) PRIMARY KEY, title VARCHAR(128) NOT NULL, "
              + "content VARCHAR(2048) NOT NULL, created_by VARCHAR(36) NOT NULL, "
              + "created_at BIGINT NOT NULL, expires_at BIGINT)");
      stmt.execute(
          "CREATE TABLE IF NOT EXISTS starx_announcement_reads ("
              + "announcement_id VARCHAR(36) NOT NULL, player_uuid VARCHAR(36) NOT NULL, "
              + "read_at BIGINT NOT NULL, "
              + "PRIMARY KEY (announcement_id, player_uuid))");
      stmt.execute(
          "CREATE TABLE IF NOT EXISTS starx_player_bindings ("
              + "player_uuid VARCHAR(36) PRIMARY KEY, qq_id VARCHAR(64), "
              + "discord_id VARCHAR(64), created_at BIGINT NOT NULL)");
      stmt.execute(
          "CREATE TABLE IF NOT EXISTS starx_staff_votes ("
              + "id VARCHAR(36) PRIMARY KEY, target_uuid VARCHAR(36) NOT NULL, "
              + "target_name VARCHAR(16) NOT NULL, reason VARCHAR(512), "
              + "vote_type VARCHAR(32) NOT NULL, status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE', "
              + "initiator_uuid VARCHAR(36) NOT NULL, initiator_name VARCHAR(16) NOT NULL, "
              + "yes_votes INT DEFAULT 0, no_votes INT DEFAULT 0, required_yes INT DEFAULT 3, "
              + "expires_at BIGINT NOT NULL, created_at BIGINT NOT NULL, resolved_at BIGINT)");
      stmt.execute(
          "CREATE TABLE IF NOT EXISTS starx_staff_vote_records ("
              + "vote_id VARCHAR(36) NOT NULL, voter_uuid VARCHAR(36) NOT NULL, "
              + "vote VARCHAR(4) NOT NULL, voted_at BIGINT NOT NULL, "
              + "PRIMARY KEY (vote_id, voter_uuid))");

      LOG.info("Database tables verified/created");
    } catch (Exception e) {
      LOG.error("Failed to ensure database tables", e);
      throw new RuntimeException("Failed to initialize database tables", e);
    }
  }

  private static void loadJdbcDrivers() {
    for (String driver :
        new String[] {"org.sqlite.JDBC", "com.mysql.cj.jdbc.Driver", "org.postgresql.Driver"}) {
      try {
        Class.forName(driver);
      } catch (ClassNotFoundException ignored) {
      }
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

  public javax.sql.DataSource getDataSource() {
    return dataSource;
  }

  @Override
  public void close() {
    dataSource.close();
  }
}
