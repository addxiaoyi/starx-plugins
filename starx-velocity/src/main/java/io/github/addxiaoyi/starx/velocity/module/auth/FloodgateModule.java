package io.github.addxiaoyi.starx.velocity.module.auth;

import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import io.github.addxiaoyi.starx.velocity.module.VelocityModule;
import java.util.Objects;
import java.util.logging.Level;

/** Floodgate 基岩版玩家支持模块，检测 Floodgate 玩家并自动跳过正版认证。 */
public final class FloodgateModule implements VelocityModule {

  private final StarxVelocityPlugin plugin;
  private final EventBus eventBus;
  private final Config config;
  private boolean floodgateAvailable;

  public FloodgateModule(StarxVelocityPlugin plugin, EventBus eventBus, Config config) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    this.config = Objects.requireNonNull(config, "config");
    this.floodgateAvailable = false;
  }

  @Override
  public String name() {
    return "auth.floodgate";
  }

  @Override
  public void onEnable() {
    // TODO: 检测 Floodgate API 是否可用（参考 StarVC FloodgateHandler，使用 FloodgateApi.getInstance()）
    checkFloodgateAvailability();
    if (floodgateAvailable) {
      plugin.logger().log(Level.INFO, "FloodgateModule 已启用，自动登录: {0}", config.autoLogin());
    } else {
      plugin.logger().log(Level.WARNING, "FloodgateModule 已启用但未检测到 Floodgate 插件");
    }
  }

  @Override
  public void onDisable() {
    // 无资源需要释放
  }

  private void checkFloodgateAvailability() {
    try {
      Class.forName("org.geysermc.floodgate.api.FloodgateApi");
      this.floodgateAvailable = true;
    } catch (ClassNotFoundException e) {
      this.floodgateAvailable = false;
    }
  }

  public String getPrefix() {
    return config.prefix();
  }

  public boolean isFloodgateAvailable() {
    return floodgateAvailable;
  }

  /**
   * 判断玩家是否为 Floodgate（基岩版）玩家。
   *
   * @param username 玩家用户名
   * @return 是否为 Floodgate 玩家
   */
  public boolean isFloodgatePlayer(String username) {
    if (!floodgateAvailable) {
      return false;
    }
    // TODO: 实现完整的 Floodgate 检测逻辑（参考 StarVC FloodgateHandler.isFloodgatePlayer）
    // 通过前缀判断：Geyser 玩家默认带有配置的前缀
    return username.startsWith(config.prefix());
  }

  /**
   * 判断是否应对该玩家自动登录。
   *
   * @param username 玩家用户名
   * @return 是否应自动登录
   */
  public boolean shouldAutoLogin(String username) {
    return config.autoLogin() && isFloodgatePlayer(username);
  }

  /** Floodgate 模块配置。 */
  public interface Config {
    boolean enabled();

    boolean autoLogin();

    String prefix();

    static Config defaultConfig() {
      return new Config() {
        @Override
        public boolean enabled() {
          return true;
        }

        @Override
        public boolean autoLogin() {
          return true;
        }

        @Override
        public String prefix() {
          return ".";
        }
      };
    }
  }
}
