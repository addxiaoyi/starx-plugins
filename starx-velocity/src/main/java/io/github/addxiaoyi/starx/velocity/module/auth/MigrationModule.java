package io.github.addxiaoyi.starx.velocity.module.auth;

import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.common.auth.uniauth.UniAuthClient;
import io.github.addxiaoyi.starx.common.database.JdbcUserRepository;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import io.github.addxiaoyi.starx.velocity.module.VelocityModule;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/** 数据迁移模块，支持从 MultiLogin / AuthMe / StarVC 等插件迁移用户数据到 StarX。 */
public final class MigrationModule implements VelocityModule {

  private static final AtomicBoolean RUNNING = new AtomicBoolean(false);

  private final StarxVelocityPlugin plugin;
  private final EventBus eventBus;
  private final Config config;
  private final JdbcUserRepository userRepository;
  private final UniAuthClient uniAuthClient;

  public MigrationModule(
      StarxVelocityPlugin plugin,
      EventBus eventBus,
      Config config,
      JdbcUserRepository userRepository,
      UniAuthClient uniAuthClient) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    this.config = Objects.requireNonNull(config, "config");
    this.userRepository = userRepository;
    this.uniAuthClient = uniAuthClient;
  }

  public MigrationModule(StarxVelocityPlugin plugin, EventBus eventBus, Config config) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    this.config = Objects.requireNonNull(config, "config");
    this.userRepository = null;
    this.uniAuthClient = null;
  }

  @Override
  public String name() {
    return "starx.auth.migration";
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
          // TODO: 实现 AuthMe 数据迁移
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

  /**
   * 从 StarVC 导入用户元数据（不包含密码，通过 UniAuth 桥接在首次登录时完成迁移）。
   *
   * @param dryRun 是否为试运行模式
   * @return 迁移结果
   */
  public MigrationResult importStarVCMeta(boolean dryRun) {
    if (!RUNNING.compareAndSet(false, true)) {
      throw new IllegalStateException("Migration is already running.");
    }

    if (userRepository == null) {
      RUNNING.set(false);
      throw new IllegalStateException("userRepository is not available");
    }

    long start = System.currentTimeMillis();
    int total = 0;
    int imported = 0;
    int skippedExisting = 0;
    int skippedInvalid = 0;
    int errors = 0;

    try (Connection sourceConn = getSourceConnection()) {
      String schemaMode = config.schemaMode();
      String tablePrefix = config.tablePrefix();

      plugin
          .logger()
          .log(
              Level.INFO,
              "开始从 StarVC 导入用户元数据 (schema={0}, prefix={1}, dryRun={2})",
              new Object[] {schemaMode, tablePrefix, dryRun});

      // 根据 schema 模式选择查询语句
      String query = buildStarVCQuery(schemaMode, tablePrefix);
      plugin.logger().log(Level.FINE, "执行查询: {0}", query);

      try (PreparedStatement st = sourceConn.prepareStatement(query);
          ResultSet rs = st.executeQuery()) {

        // 先检测表是否存在
        if (!rs.isBeforeFirst()) {
          plugin.logger().log(Level.WARNING, "未找到任何用户数据，请检查表名和连接配置是否正确");
        }

        while (rs.next()) {
          total++;
          try {
            StarVCUserEntry entry = parseStarVCUserEntry(rs, schemaMode);

            if (entry.username() == null || entry.username().isBlank()) {
              skippedInvalid++;
              plugin.logger().log(Level.FINE, "跳过无效用户: 用户名缺失");
              continue;
            }

            UUID uuid;
            try {
              uuid = entry.uuid();
            } catch (Exception e) {
              skippedInvalid++;
              plugin.logger().log(Level.FINE, "跳过无效用户: UUID 解析失败 - {0}", entry.uuidStr());
              continue;
            }

            if (userRepository.existsByUuid(uuid)
                || userRepository.existsByUsername(entry.username())) {
              skippedExisting++;
              plugin
                  .logger()
                  .log(Level.FINE, "跳过已存在用户: {0} ({1})", new Object[] {entry.username(), uuid});
              continue;
            }

            if (!dryRun) {
              // 导入用户元数据，标记为来自 StarVC，状态为 pending（等待首次登录完成密码迁移）
              var user =
                  new io.github.addxiaoyi.starx.common.model.StarxUser(
                      uuid,
                      entry.username(),
                      entry.email(),
                      null, // 密码哈希暂时为空
                      null,
                      entry.premium(),
                      java.time.Instant.now(),
                      null,
                      null,
                      java.util.List.of(),
                      null,
                      "starvc",
                      "pending",
                      null,
                      null,
                      null,
                      null,
                      0L,
                      null,
                      false);
              userRepository.create(user);
              plugin
                  .logger()
                  .log(Level.FINE, "成功导入用户: {0} ({1})", new Object[] {entry.username(), uuid});
            } else {
              plugin
                  .logger()
                  .log(
                      Level.FINE,
                      "[dry-run] 预导入用户: {0} ({1})",
                      new Object[] {entry.username(), uuid});
            }
            imported++;
          } catch (Exception e) {
            errors++;
            plugin
                .logger()
                .log(Level.WARNING, "导入用户 #{0} 失败: {1}", new Object[] {total, e.getMessage()});
            plugin.logger().log(Level.FINE, "详细错误", e);
          }
        }
      }

      plugin
          .logger()
          .log(
              Level.INFO,
              "StarVC 导入完成: 总计={0}, 导入={1}, 跳过已存在={2}, 跳过无效={3}, 错误={4}",
              new Object[] {total, imported, skippedExisting, skippedInvalid, errors});
    } catch (Exception e) {
      plugin.logger().log(Level.SEVERE, "从 StarVC 导入失败: " + e.getMessage(), e);
      errors++;
    } finally {
      RUNNING.set(false);
    }

    long duration = System.currentTimeMillis() - start;
    return new MigrationResult(
        total, imported, skippedExisting, skippedInvalid, errors, duration, dryRun);
  }

  /** 从 StarVC 解析的用户条目 */
  private record StarVCUserEntry(
      String uuidStr, UUID uuid, String username, String email, boolean premium) {}

  /** 根据 schema 模式构建查询语句 */
  private String buildStarVCQuery(String schemaMode, String tablePrefix) {
    String tableName =
        tablePrefix
            + switch (schemaMode.toLowerCase()) {
              case "authme" -> "authme";
              case "authlib" -> "users";
              case "luckperms" -> "luckperms_players";
              default -> "starvc_users";
            };

    return switch (schemaMode.toLowerCase()) {
      case "authme" ->
          String.format(
              "SELECT realname AS username, uuid, email, is_premium AS premium FROM %s", tableName);
      case "authlib" -> String.format("SELECT username, uuid, email, premium FROM %s", tableName);
      case "luckperms" -> String.format("SELECT username, uuid FROM %s", tableName);
      default -> String.format("SELECT uuid, username, email, premium FROM %s", tableName);
    };
  }

  /** 从 ResultSet 解析 StarVC 用户条目 */
  private StarVCUserEntry parseStarVCUserEntry(ResultSet rs, String schemaMode)
      throws SQLException {
    String uuidStr;
    String username;
    String email = null;
    boolean premium = false;

    switch (schemaMode.toLowerCase()) {
      case "authme" -> {
        username = rs.getString("username");
        uuidStr = rs.getString("uuid");
        email = rs.getString("email");
        premium = rs.getBoolean("premium");
      }
      case "authlib" -> {
        username = rs.getString("username");
        uuidStr = rs.getString("uuid");
        email = rs.getString("email");
        premium = rs.getBoolean("premium");
      }
      case "luckperms" -> {
        username = rs.getString("username");
        uuidStr = rs.getString("uuid");
      }
      default -> {
        uuidStr = rs.getString("uuid");
        username = rs.getString("username");
        email = rs.getString("email");
        try {
          premium = rs.getBoolean("premium");
        } catch (SQLException e) {
          // premium 字段可能不存在，忽略
        }
      }
    }

    // 尝试解析 UUID，支持多种格式
    UUID uuid;
    try {
      uuid = UUID.fromString(uuidStr);
    } catch (IllegalArgumentException e) {
      // 尝试处理不带连字符的 UUID
      if (uuidStr != null && uuidStr.length() == 32) {
        uuid =
            UUID.fromString(
                uuidStr.substring(0, 8)
                    + "-"
                    + uuidStr.substring(8, 12)
                    + "-"
                    + uuidStr.substring(12, 16)
                    + "-"
                    + uuidStr.substring(16, 20)
                    + "-"
                    + uuidStr.substring(20));
      } else {
        throw e;
      }
    }

    return new StarVCUserEntry(uuidStr, uuid, username, email, premium);
  }

  private MigrationResult migrateFromMultiLogin(boolean dryRun) throws Exception {
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

    /** StarVC 特定配置：表名前缀 */
    default String tablePrefix() {
      return "";
    }

    /** StarVC 特定配置：表结构模式 (starvc, authme, authlib) */
    default String schemaMode() {
      return "starx.starvc";
    }

    static Config defaultConfig() {
      return new Config() {
        @Override
        public boolean enabled() {
          return false;
        }

        @Override
        public String source() {
          return "starx.starvc";
        }

        @Override
        public String backend() {
          return "starx.sqlite";
        }

        @Override
        public Map<String, Object> connection() {
          return Map.of("path", "plugins/StarVC/starvc.db");
        }

        @Override
        public String tablePrefix() {
          return "";
        }

        @Override
        public String schemaMode() {
          return "starx.starvc";
        }
      };
    }
  }
}
