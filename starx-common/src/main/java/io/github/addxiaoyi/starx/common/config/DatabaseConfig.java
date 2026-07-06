package io.github.addxiaoyi.starx.common.config;

/**
 * 数据库连接池配置。
 *
 * @param type 数据库类型，支持 h2、mysql、postgresql、sqlite
 * @param host 主机地址
 * @param port 端口
 * @param database 数据库名（SQLite时为文件路径）
 * @param username 用户名
 * @param password 密码
 * @param url 完整 JDBC URL；非空时优先使用
 * @param poolMaxSize 连接池最大连接数
 * @param connectionTimeoutMs 连接超时毫秒数
 */
public record DatabaseConfig(
    String type,
    String host,
    int port,
    String database,
    String username,
    String password,
    String url,
    int poolMaxSize,
    long connectionTimeoutMs) {

  public DatabaseConfig {
    type = type == null || type.isBlank() ? "sqlite" : type;
    host = host == null ? "" : host;
    port = port <= 0 ? 3306 : port;
    database = database == null || database.isBlank() ? "plugins/starx/starx.db" : database;
    username = username == null ? "starx" : username;
    password = password == null ? "" : password;
    url = url == null ? "" : url;
    poolMaxSize = poolMaxSize <= 0 ? 10 : poolMaxSize;
    connectionTimeoutMs = connectionTimeoutMs <= 0 ? 30_000L : connectionTimeoutMs;
  }

  public static DatabaseConfig defaults() {
    return new DatabaseConfig("sqlite", "", 3306, "plugins/starx/starx.db", "starx", "", "", 10, 30_000L);
  }

  public boolean hasUrl() {
    return !url.isBlank();
  }

  public boolean isSqlite() {
    return "sqlite".equalsIgnoreCase(type);
  }

  public String jdbcUrl() {
    if (hasUrl()) {
      return url;
    }
    return switch (type.toLowerCase()) {
      case "h2" -> "jdbc:h2:mem:" + database;
      case "mysql" ->
          "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&serverTimezone=UTC";
      case "postgresql" -> "jdbc:postgresql://" + host + ":" + port + "/" + database;
      case "sqlite" -> "jdbc:sqlite:" + database;
      default -> throw new IllegalArgumentException("Unsupported database type: " + type);
    };
  }
}
