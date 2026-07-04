package io.github.addxiaoyi.starx.velocity.module.auth;

import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import io.github.addxiaoyi.starx.velocity.module.VelocityModule;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/** 数据迁移模块，支持从 MultiLogin / AuthMe 等插件迁移用户数据到 StarX。 */
public final class MigrationModule implements VelocityModule {

  private static final AtomicBoolean RUNNING = new AtomicBoolean(false);

  private final StarxVelocityPlugin plugin;
  private final EventBus eventBus;
  private final Config config;

  public MigrationModule(StarxVelocityPlugin plugin, EventBus eventBus, Config config) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    this.config = Objects.requireNonNull(config, "config");
  }

  @Override
  public String name() {
    return "auth.migration";
  }

  @Override
  public void onEnable() {
    plugin.logger().log(Level.INFO, "MigrationModule 已启用，数据源: {0}", config.source());
  }

  @Override
  public void onDisable() {
    // 如果迁移正在运行中，等待完成
    RUNNING.set(false);
  }

  public static boolean isRunning() {
    return RUNNING.get();
  }

  /**
   * 执行数据迁移。
   *
   * @param dryRun 是否为试运行模式（不实际写入数据）
   * @return 迁移结果
   */
  public MigrationResult migrate(boolean dryRun) {
    if (!RUNNING.compareAndSet(false, true)) {
      throw new IllegalStateException("Migration is already running.");
    }
    long start = System.currentTimeMillis();
    int total = 0;
    int imported = 0;
    int skippedExisting = 0;
    int skippedInvalid = 0;
    int errors = 0;

    try {
      switch (config.source().toLowerCase()) {
        case "multilogin":
          {
            var result = migrateFromMultiLogin(dryRun);
            total = result.total;
            imported = result.imported;
            skippedExisting = result.skippedExisting;
            skippedInvalid = result.skippedInvalid;
            errors = result.errors;
            break;
          }
        case "authme":
          // TODO: 实现 AuthMe 数据迁移（参考 AuthMe 数据库结构）
          plugin.logger().log(Level.WARNING, "AuthMe 迁移尚未实现");
          break;
        default:
          plugin.logger().log(Level.SEVERE, "不支持的数据源: {0}", config.source());
          break;
      }
    } catch (Exception e) {
      plugin.logger().log(Level.SEVERE, "迁移失败: " + e.getMessage(), e);
      errors++;
    } finally {
      RUNNING.set(false);
    }

    long duration = System.currentTimeMillis() - start;
    return new MigrationResult(
        total, imported, skippedExisting, skippedInvalid, errors, duration, dryRun);
  }

  private MigrationResult migrateFromMultiLogin(boolean dryRun) throws Exception {
    // 参考 StarVC MultiLoginUuidMigrator 逻辑
    int total = 0;
    int imported = 0;
    int skippedExisting = 0;
    int skippedInvalid = 0;
    int errors = 0;

    try (Connection sourceConn = getSourceConnection()) {
      String table = "multilogin_in_game_profile_v3";
      String query = "SELECT in_game_uuid, current_username_original FROM " + table;

      try (PreparedStatement st = sourceConn.prepareStatement(query);
          ResultSet rs = st.executeQuery()) {
        while (rs.next()) {
          total++;
          String username = rs.getString("current_username_original");
          UUID uuid = bytesToUuid(rs.getBytes("in_game_uuid"));

          if (username == null || username.isBlank() || uuid == null) {
            skippedInvalid++;
            continue;
          }

          try {
            // TODO: 检查是否已存在用户数据
            // if (userRepository.hasUUIDData(username)) { skippedExisting++; continue; }
            if (!dryRun) {
              // TODO: 写入目标数据库
              // userRepository.setUUID(username, uuid);
            }
            imported++;
          } catch (Exception e) {
            errors++;
            plugin.logger().log(Level.WARNING, "迁移用户 " + username + " 失败: " + e.getMessage());
          }
        }
      }
    }

    return new MigrationResult(total, imported, skippedExisting, skippedInvalid, errors, 0, dryRun);
  }

  private Connection getSourceConnection() throws Exception {
    Map<String, Object> conn = config.connection();
    String backend = config.backend().toLowerCase();
    return switch (backend) {
      case "mysql" -> {
        String host = (String) conn.getOrDefault("host", "localhost");
        int port = (int) conn.getOrDefault("port", 3306);
        String database = (String) conn.getOrDefault("database", "multilogin");
        String username = (String) conn.getOrDefault("username", "root");
        String password = (String) conn.getOrDefault("password", "");
        String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database;
        yield DriverManager.getConnection(jdbcUrl, username, password);
      }
      case "sqlite" -> {
        String path = (String) conn.getOrDefault("path", "");
        String jdbcUrl = "jdbc:sqlite:" + path;
        yield DriverManager.getConnection(jdbcUrl);
      }
      case "h2" -> {
        String path = (String) conn.getOrDefault("path", "");
        String username = (String) conn.getOrDefault("username", "sa");
        String password = (String) conn.getOrDefault("password", "");
        String jdbcUrl = "jdbc:h2:" + path;
        yield DriverManager.getConnection(jdbcUrl, username, password);
      }
      default -> throw new IllegalArgumentException("不支持的后端类型: " + backend);
    };
  }

  private static UUID bytesToUuid(byte[] bytes) {
    if (bytes == null || bytes.length != 16) {
      return null;
    }
    ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
    return new UUID(bb.getLong(), bb.getLong());
  }

  /** 迁移结果记录。 */
  public record MigrationResult(
      int total,
      int imported,
      int skippedExisting,
      int skippedInvalid,
      int errors,
      long durationMs,
      boolean dryRun) {}

  /** 迁移模块配置。 */
  public interface Config {
    boolean enabled();

    String source();

    String backend();

    Map<String, Object> connection();

    static Config defaultConfig() {
      return new Config() {
        @Override
        public boolean enabled() {
          return false;
        }

        @Override
        public String source() {
          return "multilogin";
        }

        @Override
        public String backend() {
          return "mysql";
        }

        @Override
        public Map<String, Object> connection() {
          return Map.of(
              "host", "localhost",
              "port", 3306,
              "database", "multilogin",
              "username", "root",
              "password", "");
        }
      };
    }
  }
}
