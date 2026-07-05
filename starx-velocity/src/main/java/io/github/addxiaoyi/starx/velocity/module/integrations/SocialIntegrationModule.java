package io.github.addxiaoyi.starx.velocity.module.integrations;

import io.github.addxiaoyi.starx.api.event.StarxEvent;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import io.github.addxiaoyi.starx.velocity.event.VelocityEventBus;
import io.github.addxiaoyi.starx.velocity.module.VelocityModule;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/** 社交平台集成模块：Discord/Telegram 机器人，玩家账号关联/解绑。 */
public final class SocialIntegrationModule implements VelocityModule {

  private final StarxVelocityPlugin plugin;
  private final VelocityEventBus eventBus;
  private final Config config;
  private final AtomicBoolean initialized = new AtomicBoolean(false);
  private final Map<String, Map<String, String>> linkedAccounts = new ConcurrentHashMap<>();

  public SocialIntegrationModule(
      StarxVelocityPlugin plugin, VelocityEventBus eventBus, Config config) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    this.config = Objects.requireNonNull(config, "config");
  }

  @Override
  public String name() {
    return "integrations.social";
  }

  @Override
  public void onEnable() {
    if (!config.enabled()) {
      return;
    }
    initialized.set(true);

    // TODO: 初始化 Discord 机器人 (JDA)
    // TODO: 初始化 Telegram 机器人 (TelegramBots)
    // TODO: 注册 /link 命令
    // TODO: 注册 /unlink 命令
    // TODO: 注册键盘布局
    // TODO: 构建 SocialManager 管理多个平台

    plugin.logger().info("SocialIntegration module initialized.");
  }

  @Override
  public void onDisable() {
    initialized.set(false);

    // TODO: 关闭 Discord 机器人
    // TODO: 关闭 Telegram 机器人

    plugin.logger().info("SocialIntegration module disabled.");
  }

  public boolean isInitialized() {
    return initialized.get();
  }

  public void linkPlayer(String playerName, String platform, String platformId) {
    linkedAccounts
        .computeIfAbsent(playerName, k -> new ConcurrentHashMap<>())
        .put(platform, platformId);

    Map<String, Object> payload = new HashMap<>();
    payload.put("player", playerName);
    payload.put("platform", platform);
    payload.put("platformId", platformId);
    eventBus.publish(new StarxEvent("social.player.linked", payload));

    plugin
        .logger()
        .info("Player " + playerName + " linked " + platform + " account: " + platformId);
  }

  public void unlinkPlayer(String playerName, String platform) {
    Map<String, String> accounts = linkedAccounts.get(playerName);
    if (accounts != null) {
      accounts.remove(platform);
    }

    Map<String, Object> payload = new HashMap<>();
    payload.put("player", playerName);
    payload.put("platform", platform);
    eventBus.publish(new StarxEvent("social.player.unlinked", payload));

    plugin.logger().info("Player " + playerName + " unlinked " + platform + " account.");
  }

  public String getLinkedDiscordId(String playerName) {
    Map<String, String> accounts = linkedAccounts.get(playerName);
    return accounts != null ? accounts.get("discord") : null;
  }

  public String getLinkedTelegramId(String playerName) {
    Map<String, String> accounts = linkedAccounts.get(playerName);
    return accounts != null ? accounts.get("telegram") : null;
  }

  public boolean isLinked(String playerName, String platform) {
    Map<String, String> accounts = linkedAccounts.get(playerName);
    return accounts != null && accounts.containsKey(platform);
  }

  public interface Config {
    boolean enabled();

    DiscordConfig discord();

    TelegramConfig telegram();

    List<List<KeyboardItem>> commands();

    Strings strings();

    static Config defaultConfig() {
      return new Config() {
        @Override
        public boolean enabled() {
          return false;
        }

        @Override
        public DiscordConfig discord() {
          return new DiscordConfig() {
            @Override
            public boolean enabled() {
              return false;
            }

            @Override
            public String token() {
              return "";
            }
          };
        }

        @Override
        public TelegramConfig telegram() {
          return new TelegramConfig() {
            @Override
            public boolean enabled() {
              return false;
            }

            @Override
            public String token() {
              return "";
            }
          };
        }

        @Override
        public List<List<KeyboardItem>> commands() {
          return List.of();
        }

        @Override
        public Strings strings() {
          return new Strings() {
            @Override
            public String linkSuccess() {
              return "Account linked!";
            }

            @Override
            public String unlinkSuccess() {
              return "Account unlinked!";
            }

            @Override
            public String notLinked() {
              return "No account linked.";
            }

            @Override
            public String alreadyLinked() {
              return "Already linked.";
            }
          };
        }
      };
    }
  }

  public interface DiscordConfig {
    boolean enabled();

    String token();
  }

  public interface TelegramConfig {
    boolean enabled();

    String token();
  }

  public interface KeyboardItem {
    String id();

    String value();
  }

  public interface Strings {
    String linkSuccess();

    String unlinkSuccess();

    String notLinked();

    String alreadyLinked();
  }
}
