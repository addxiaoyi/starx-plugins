package io.github.addxiaoyi.starx.velocity.module.auth;

import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import io.github.addxiaoyi.starx.velocity.module.VelocityModule;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;

/** TAB 占位符集成模块，注册自定义认证相关占位符到 TAB 插件。 */
public final class TabIntegrationModule implements VelocityModule {

  private final StarxVelocityPlugin plugin;
  private final EventBus eventBus;
  private final Config config;
  private boolean tabAvailable;

  public TabIntegrationModule(StarxVelocityPlugin plugin, EventBus eventBus, Config config) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    this.config = Objects.requireNonNull(config, "config");
    this.tabAvailable = false;
  }

  @Override
  public String name() {
    return "starx.auth.tab";
  }

  @Override
  public void onEnable() {
    checkTabAvailability();
    if (tabAvailable) {
      // TODO: 注册 TAB 占位符
      // 使用 TabAPI.getInstance().getPlaceholderManager().registerPlayerPlaceholder(...)
      plugin
          .logger()
          .log(Level.INFO, "TabIntegrationModule 已启用，注册 {0} 个占位符", config.placeholders().size());
    } else {
      plugin.logger().log(Level.WARNING, "TabIntegrationModule 已启用但未检测到 TAB 插件");
    }
  }

  @Override
  public void onDisable() {
    // TODO: 注销 TAB 占位符
  }

  private void checkTabAvailability() {
    try {
      Class.forName("me.neznamy.tab.api.TabAPI");
      this.tabAvailable = true;
    } catch (ClassNotFoundException e) {
      this.tabAvailable = false;
    }
  }

  public Map<String, String> getPlaceholders() {
    return Collections.unmodifiableMap(config.placeholders());
  }

  public boolean isTabAvailable() {
    return tabAvailable;
  }

  /**
   * 获取指定占位符的值。
   *
   * @param placeholder 占位符名称
   * @param playerName 玩家名称
   * @return 占位符值
   */
  public String resolvePlaceholder(String placeholder, String playerName) {
    // TODO: 实现占位符解析逻辑
    String key = config.placeholders().get(placeholder);
    if (key == null) {
      return "";
    }
    return switch (key) {
      case "auth_status" -> "online";
      case "2fa_status" -> "disabled";
      default -> "";
    };
  }

  /** TAB 集成模块配置。 */
  public interface Config {
    boolean enabled();

    Map<String, String> placeholders();

    static Config defaultConfig() {
      return new Config() {
        @Override
        public boolean enabled() {
          return true;
        }

        @Override
        public Map<String, String> placeholders() {
          return Map.of(
              "%starx_auth_status%", "auth_status",
              "%starx_2fa_status%", "2fa_status");
        }
      };
    }
  }
}
