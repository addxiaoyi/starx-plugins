package io.github.addxiaoyi.starx.velocity.config;

import io.github.addxiaoyi.starx.common.auth.uniauth.UniAuthConfig;
import io.github.addxiaoyi.starx.common.config.DatabaseConfig;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

public final class ConfigLoader {

  private static final String DEFAULT_CONFIG =
      """
      # StarX Velocity 配置文件
      api-key: ""

      http:
        bind: "127.0.0.1"
        port: 8788

      webhook:
        url: ""
        secret: ""

      database:
        type: "sqlite"
        host: ""
        port: 3306
        database: "plugins/starx/data.db"
        username: "starx"
        password: ""
        url: ""
        pool-max-size: 2
        connection-timeout-ms: 30000

      uniauth:
        enabled: false
        api-url: "https://api.example.com/uniauth/"
        api-key: ""
        timeout-ms: 5000
        bridge-mode: false

      modules:
        starx.auth:
          enabled: true
        starx.auth.yggdrasil:
          enabled: true
        starx.auth.uniauth:
          enabled: false
        starx.auth.floodgate:
          enabled: true
        starx.auth.tab:
          enabled: true
        starx.auth.migration:
          enabled: false
        starx.skin-bridge:
          enabled: true
        starx.chat:
          enabled: true
        starx.maintenance:
          enabled: true
        starx.motd:
          enabled: true
        starx.redirect:
          enabled: true
        starx.queue:
          enabled: true
        starx.limbo:
          enabled: true
        starx.reconnect:
          enabled: true
        starx.info:
          enabled: true
        starx.forge:
          enabled: false
        starx.proxytools.raknet:
          enabled: false
        starx.online:
          enabled: true
        starx.enhanced:
          enabled: true
        starx.proxytools.filecleaner:
          enabled: false
        starx.security.bot:
          enabled: true
        starx.security.crash:
          enabled: true
        starx.security.risk:
          enabled: true
        starx.security.anticheat:
          enabled: true
        starx.integrations.qq:
          enabled: false
        starx.integrations.plan:
          enabled: false
        starx.integrations.mapmod:
          enabled: false
        starx.integrations.social:
          enabled: false
        starx.integrations.napcat:
          enabled: false
        starx.vote:
          enabled: true

      napcat:
        enabled: false
        ws-url: "ws://127.0.0.1:6700"
        http-url: "http://127.0.0.1:3000"
        qq-group-id: 0
        forward-format: "[MC] {player}: {message}"
      """;

  private ConfigLoader() {}

  @SuppressWarnings("unchecked")
  public static StarxConfig load(Path path) throws IOException {
    if (!Files.exists(path)) {
      Files.createDirectories(path.getParent());
      Files.writeString(path, DEFAULT_CONFIG, StandardCharsets.UTF_8);
    }

    Map<String, Object> root;
    try (InputStream in = Files.newInputStream(path)) {
      root = new Yaml().load(in);
    }
    if (root == null) root = Map.of();

    String apiKey = str(root, "api-key", "");

    Map<String, Object> httpNode = child(root, "http");
    StarxConfig.HttpConfig http =
        new StarxConfig.HttpConfig(
            str(httpNode, "bind", "127.0.0.1"), integer(httpNode, "port", 8788));

    Map<String, Object> webhookNode = child(root, "webhook");
    StarxConfig.WebhookConfig webhook =
        new StarxConfig.WebhookConfig(str(webhookNode, "url", ""), str(webhookNode, "secret", ""));

    Map<String, StarxConfig.ModuleConfig> modules = new HashMap<>();
    Map<String, Object> modulesNode = child(root, "modules");
    for (Map.Entry<String, Object> entry : modulesNode.entrySet()) {
      String name = entry.getKey();
      boolean enabled = false;
      if (entry.getValue() instanceof Map) {
        enabled = bool((Map<String, Object>) entry.getValue(), "enabled", false);
      }
      modules.put(name, new StarxConfig.ModuleConfig(enabled));
    }

    DatabaseConfig database = parseDatabaseConfig(child(root, "database"));
    UniAuthConfig uniauth = parseUniAuthConfig(child(root, "uniauth"));
    StarxConfig.NapcatConfig napcat = parseNapcatConfig(child(root, "napcat"));

    return new StarxConfig(apiKey, http, webhook, database, uniauth, napcat, modules);
  }

  private static Map<String, Object> child(Map<String, Object> parent, String key) {
    Object v = parent.get(key);
    return v instanceof Map ? (Map<String, Object>) v : Map.of();
  }

  private static String str(Map<String, Object> map, String key, String def) {
    Object v = map.get(key);
    return v != null ? v.toString() : def;
  }

  private static int integer(Map<String, Object> map, String key, int def) {
    Object v = map.get(key);
    if (v instanceof Number n) return n.intValue();
    if (v != null)
      try {
        return Integer.parseInt(v.toString());
      } catch (NumberFormatException e) {
      }
    return def;
  }

  private static long longVal(Map<String, Object> map, String key, long def) {
    Object v = map.get(key);
    if (v instanceof Number n) return n.longValue();
    if (v != null)
      try {
        return Long.parseLong(v.toString());
      } catch (NumberFormatException e) {
      }
    return def;
  }

  private static boolean bool(Map<String, Object> map, String key, boolean def) {
    Object v = map.get(key);
    return v instanceof Boolean ? (Boolean) v : def;
  }

  private static UniAuthConfig parseUniAuthConfig(Map<String, Object> node) {
    return new UniAuthConfig(
        bool(node, "enabled", false),
        str(node, "api-url", "https://api.example.com/uniauth/"),
        str(node, "api-key", ""),
        integer(node, "timeout-ms", 5000),
        bool(node, "bridge-mode", false));
  }

  private static StarxConfig.NapcatConfig parseNapcatConfig(Map<String, Object> node) {
    return new StarxConfig.NapcatConfig(
        bool(node, "enabled", false),
        str(node, "ws-url", "ws://127.0.0.1:6700"),
        str(node, "http-url", "http://127.0.0.1:3000"),
        longVal(node, "qq-group-id", 0),
        str(node, "forward-format", "[MC] {player}: {message}"));
  }

  private static DatabaseConfig parseDatabaseConfig(Map<String, Object> node) {
    return new DatabaseConfig(
        str(node, "type", "h2"),
        str(node, "host", ""),
        integer(node, "port", 3306),
        str(node, "database", "starx"),
        str(node, "username", "starx"),
        str(node, "password", ""),
        str(node, "url", ""),
        integer(node, "pool-max-size", 10),
        longVal(node, "connection-timeout-ms", 30_000L));
  }
}
