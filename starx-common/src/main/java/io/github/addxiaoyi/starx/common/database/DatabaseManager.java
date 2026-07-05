package io.github.addxiaoyi.starx.common.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.addxiaoyi.starx.common.config.DatabaseConfig;
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
    if (isH2) {
      hikariConfig.setMaximumPoolSize(Math.min(config.poolMaxSize(), 3));
      hikariConfig.setMinimumIdle(1);
    } else {
      hikariConfig.setMaximumPoolSize(config.poolMaxSize());
      hikariConfig.setMinimumIdle(2);
    }
    hikariConfig.setPoolName("starx-common-pool");

    this.dataSource = new HikariDataSource(hikariConfig);
    this.jdbi = Jdbi.create(this.dataSource).installPlugin(new SqlObjectPlugin());

    migrate();
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
