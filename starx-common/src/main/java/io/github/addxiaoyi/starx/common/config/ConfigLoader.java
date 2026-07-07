package io.github.addxiaoyi.starx.common.config;

import io.github.addxiaoyi.starx.common.auth.uniauth.UniAuthConfig;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

public final class ConfigLoader {

  private ConfigLoader() {}

  public static StarxConfig load(Path path) throws IOException {
    if (!Files.isRegularFile(path)) {
      return StarxConfig.defaults();
    }

    Map<String, Object> root;
    try (InputStream in = Files.newInputStream(path)) {
      root = new Yaml().load(in);
    }
    if (root == null) {
      root = Map.of();
    }

    HttpApiConfig httpApi = loadHttpApi(child(root, "http-api"));
    DatabaseConfig database = loadDatabase(child(root, "database"));
    UniAuthConfig uniauth = loadUniAuth(child(root, "uniauth"));
    Map<String, ModuleConfig> modules = loadModules(child(root, "modules"));

    return new StarxConfig(httpApi, database, uniauth, modules);
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> child(Map<String, Object> parent, String key) {
    Object value = parent.get(key);
    return value instanceof Map ? (Map<String, Object>) value : Map.of();
  }

  private static String str(Map<String, Object> map, String key, String def) {
    Object v = map.get(key);
    return v != null ? v.toString() : def;
  }

  private static int integer(Map<String, Object> map, String key, int def) {
    Object v = map.get(key);
    if (v instanceof Number n) return n.intValue();
    if (v != null) try { return Integer.parseInt(v.toString()); } catch (NumberFormatException e) { }
    return def;
  }

  private static long longVal(Map<String, Object> map, String key, long def) {
    Object v = map.get(key);
    if (v instanceof Number n) return n.longValue();
    if (v != null) try { return Long.parseLong(v.toString()); } catch (NumberFormatException e) { }
    return def;
  }

  private static boolean bool(Map<String, Object> map, String key, boolean def) {
    Object v = map.get(key);
    return v instanceof Boolean ? (Boolean) v : def;
  }

  private static UniAuthConfig loadUniAuth(Map<String, Object> node) {
    UniAuthConfig defaults = UniAuthConfig.defaults();
    return new UniAuthConfig(
        bool(node, "enabled", defaults.enabled()),
        str(node, "api-url", defaults.apiUrl()),
        str(node, "api-key", defaults.apiKey()),
        integer(node, "timeout-ms", defaults.timeoutMs()),
        bool(node, "bridge-mode", defaults.bridgeMode()));
  }

  private static HttpApiConfig loadHttpApi(Map<String, Object> node) {
    HttpApiConfig defaults = HttpApiConfig.defaults();
    return new HttpApiConfig(
        str(node, "bind", defaults.bind()),
        integer(node, "port", defaults.port()),
        str(node, "api-key", defaults.apiKey()));
  }

  private static DatabaseConfig loadDatabase(Map<String, Object> node) {
    DatabaseConfig defaults = DatabaseConfig.defaults();
    return new DatabaseConfig(
        str(node, "type", defaults.type()),
        str(node, "host", defaults.host()),
        integer(node, "port", defaults.port()),
        str(node, "database", defaults.database()),
        str(node, "username", defaults.username()),
        str(node, "password", defaults.password()),
        str(node, "url", defaults.url()),
        integer(node, "pool-max-size", defaults.poolMaxSize()),
        longVal(node, "connection-timeout-ms", defaults.connectionTimeoutMs()));
  }

  @SuppressWarnings("unchecked")
  private static Map<String, ModuleConfig> loadModules(Map<String, Object> node) {
    Map<String, ModuleConfig> modules = new HashMap<>();
    for (Map.Entry<String, Object> entry : node.entrySet()) {
      String name = entry.getKey();
      boolean enabled = false;
      if (entry.getValue() instanceof Map) {
        enabled = bool((Map<String, Object>) entry.getValue(), "enabled", false);
      }
      modules.put(name, new ModuleConfig(enabled));
    }
    return modules;
  }
}
