package io.github.addxiaoyi.starx.velocity.module.welcome;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import io.github.addxiaoyi.starx.common.database.JdbiUserRepository;
import io.github.addxiaoyi.starx.common.model.StarxUser;
import io.github.addxiaoyi.starx.common.security.HttpClient;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import io.github.addxiaoyi.starx.velocity.module.VelocityModule;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class WelcomeModule implements VelocityModule {
  private static final Logger LOGGER = Logger.getLogger(WelcomeModule.class.getName());

  private static final TextColor PRIMARY = TextColor.color(99, 102, 241);
  private static final TextColor SECONDARY = TextColor.color(139, 92, 246);
  private static final TextColor ACCENT = TextColor.color(236, 72, 153);
  private static final TextColor SUCCESS = TextColor.color(34, 197, 94);
  private static final TextColor WARNING = TextColor.color(251, 191, 36);
  private static final TextColor INFO = TextColor.color(59, 130, 246);
  private static final TextColor TEXT = TextColor.color(226, 232, 240);
  private static final TextColor SUBTEXT = TextColor.color(148, 163, 184);
  private static final TextColor BORDER = TextColor.color(51, 65, 85);

  private final StarxVelocityPlugin plugin;
  private final JdbiUserRepository userRepository;
  private final Map<UUID, Instant> loginTimestamps = new HashMap<>();
  private final Config config;

  public WelcomeModule(StarxVelocityPlugin plugin, JdbiUserRepository userRepository) {
    this.plugin = plugin;
    this.userRepository = userRepository;
    this.config = Config.defaultConfig();
  }

  public WelcomeModule(
      StarxVelocityPlugin plugin, JdbiUserRepository userRepository, Config config) {
    this.plugin = plugin;
    this.userRepository = userRepository;
    this.config = config;
  }

  @Override
  public String name() {
    return "welcome";
  }

  @Override
  public void onEnable() {
    plugin.proxy().getEventManager().register(plugin, new WelcomeListener());
    LOGGER.info("Welcome module enabled");
  }

  @Override
  public void onDisable() {
    loginTimestamps.clear();
  }

  private void onPlayerJoin(Player player) {
    UUID uuid = player.getUniqueId();
    String username = player.getUsername();

    loginTimestamps.put(uuid, Instant.now());

    String ip = getPlayerIp(player);
    fetchAndSaveIpInfo(uuid, ip);

    Optional<StarxUser> userOpt = userRepository.findFullByUuid(uuid);
    if (userOpt.isPresent()) {
      StarxUser user = userOpt.get();
      sendWelcomeMessage(player, user);
    } else {
      sendFirstTimeWelcome(player, username);
    }
  }

  private void onPlayerQuit(Player player) {
    UUID uuid = player.getUniqueId();
    Instant loginTime = loginTimestamps.remove(uuid);
    if (loginTime != null) {
      long duration = Duration.between(loginTime, Instant.now()).getSeconds();
      if (duration > 0) {
        userRepository.updateTotalPlaytime(uuid, duration);
        userRepository.updateLastLogout(uuid, Instant.now());
      }
    }
  }

  private String getPlayerIp(Player player) {
    InetSocketAddress address = player.getRemoteAddress();
    if (address != null && address.getAddress() != null) {
      return address.getAddress().getHostAddress();
    }
    return "unknown";
  }

  private void fetchAndSaveIpInfo(UUID uuid, String ip) {
    plugin
        .proxy()
        .getScheduler()
        .buildTask(
            plugin,
            () -> {
              try {
                IpApiResponse response =
                    HttpClient.get("http://ip-api.com/json/" + ip).sendJson(IpApiResponse.class);
                if (response != null && "success".equals(response.status)) {
                  String isp = response.isp;
                  String location = formatLocation(response);
                  userRepository.updateLoginInfo(uuid, ip, isp, location);
                }
              } catch (Exception e) {
                LOGGER.log(Level.FINE, "Failed to fetch IP info for " + uuid, e);
              }
            })
        .delay(1, TimeUnit.SECONDS)
        .schedule();
  }

  private String formatLocation(IpApiResponse response) {
    StringBuilder sb = new StringBuilder();
    if (response.city != null) sb.append(response.city);
    if (response.regionName != null) {
      if (sb.length() > 0) sb.append(", ");
      sb.append(response.regionName);
    }
    if (response.country != null) {
      if (sb.length() > 0) sb.append(", ");
      sb.append(response.country);
    }
    return sb.length() > 0 ? sb.toString() : "Unknown";
  }

  private void sendWelcomeMessage(Player player, StarxUser user) {
    String username = user.username();
    String ip = user.lastLoginIp();
    String isp = user.lastLoginIsp();
    String location = user.lastLoginLocation();
    Long totalPlaytime = user.totalPlaytime();
    Instant lastLogout = user.lastLogoutAt();
    String email = user.email();
    String totpSecret = user.totpSecret();
    Boolean welcomeShown = user.welcomeMessageShown();

    TextComponent.Builder welcome = Component.text();

    welcome.append(Component.newline());
    welcome.append(createTopBorder());
    welcome.append(Component.newline());

    welcome.append(createHeaderRow(username, welcomeShown != null && welcomeShown));
    welcome.append(Component.newline());

    welcome.append(createDivider());
    welcome.append(Component.newline());

    if (welcomeShown != null && welcomeShown) {
      welcome.append(createInfoRow("👋", "欢迎回来！", "很高兴再次见到你", SUCCESS));
    } else {
      welcome.append(createInfoRow("🎉", "首次登录！", "欢迎加入我们！", SECONDARY));
      userRepository.markWelcomeMessageShown(user.uuid());
    }
    welcome.append(Component.newline());

    if (ip != null || isp != null || location != null) {
      welcome.append(createDivider());
      welcome.append(Component.newline());

      if (location != null) {
        welcome.append(createInfoRow("📍", "位置", location, TEXT));
        welcome.append(Component.newline());
      }
      if (isp != null) {
        welcome.append(createInfoRow("🌐", "网络", isp, TEXT));
        welcome.append(Component.newline());
      }
    }

    if (totalPlaytime != null && totalPlaytime > 0) {
      String playtimeStr = formatPlaytime(totalPlaytime);
      welcome.append(createDivider());
      welcome.append(Component.newline());
      welcome.append(createInfoRow("⏱️", "总游玩时长", playtimeStr, TEXT));
      welcome.append(Component.newline());
    }

    if (lastLogout != null) {
      String offlineDuration = formatDuration(Duration.between(lastLogout, Instant.now()));
      welcome.append(createInfoRow("🕒", "上次离线", offlineDuration + " 前", TEXT));
      welcome.append(Component.newline());
    }

    boolean needsEmail = email == null || email.isBlank();
    boolean needs2FA = totpSecret == null || totpSecret.isBlank();

    if (needsEmail || needs2FA) {
      welcome.append(createDivider());
      welcome.append(Component.newline());
      welcome.append(createInfoRow("🔒", "安全提醒", "建议您完善账号安全设置", WARNING, TextDecoration.BOLD));
      welcome.append(Component.newline());

      if (needsEmail) {
        welcome.append(createActionButton("📧", "绑定邮箱", "/auth email", "点击绑定邮箱", INFO));
        welcome.append(Component.newline());
      }

      if (needs2FA) {
        welcome.append(createActionButton("🔐", "开启2FA", "/auth 2fa", "点击开启二步验证", INFO));
        welcome.append(Component.newline());
      }
    }

    welcome.append(createBottomBorder());
    welcome.append(Component.newline());

    player.sendMessage(welcome.build());
  }

  private void sendFirstTimeWelcome(Player player, String username) {
    TextComponent.Builder welcome = Component.text();

    welcome.append(Component.newline());
    welcome.append(createTopBorder());
    welcome.append(Component.newline());

    welcome.append(createHeaderRow(username, false));
    welcome.append(Component.newline());

    welcome.append(createDivider());
    welcome.append(Component.newline());

    welcome.append(createInfoRow("🎊", "欢迎加入！", "很高兴见到你！", SUCCESS, TextDecoration.BOLD));
    welcome.append(Component.newline());

    welcome.append(createDivider());
    welcome.append(Component.newline());

    welcome.append(createInfoRow("💡", "快速开始", "请先完成以下安全设置", SUBTEXT));
    welcome.append(Component.newline());

    welcome.append(createActionButton("📧", "1. 绑定邮箱", "/auth email", "点击绑定邮箱", PRIMARY));
    welcome.append(Component.newline());

    welcome.append(createActionButton("🔐", "2. 开启2FA", "/auth 2fa", "点击开启二步验证", SECONDARY));
    welcome.append(Component.newline());

    welcome.append(createBottomBorder());
    welcome.append(Component.newline());

    player.sendMessage(welcome.build());
  }

  private Component createTopBorder() {
    return Component.text()
        .append(Component.text("╭─────────────────────────────────────╮", BORDER))
        .build();
  }

  private Component createBottomBorder() {
    return Component.text()
        .append(Component.text("╰─────────────────────────────────────╯", BORDER))
        .build();
  }

  private Component createDivider() {
    return Component.text()
        .append(Component.text("├─────────────────────────────────────┤", BORDER))
        .build();
  }

  private Component createHeaderRow(String username, boolean isReturning) {
    TextComponent.Builder builder = Component.text();
    builder.append(Component.text("│ ", BORDER));

    if (isReturning) {
      builder.append(Component.text("✨ ", PRIMARY));
    } else {
      builder.append(Component.text("🌟 ", SECONDARY));
    }

    builder.append(
        Component.text(username, TEXT, TextDecoration.BOLD)
            .hoverEvent(HoverEvent.showText(Component.text("玩家: " + username, SUBTEXT))));

    int padding = 28 - username.length();
    if (padding > 0) {
      builder.append(Component.text(" ".repeat(Math.max(0, padding))));
    }

    builder.append(Component.text(" │", BORDER));
    return builder.build();
  }

  private Component createInfoRow(String icon, String label, String value, TextColor valueColor) {
    return createInfoRow(icon, label, value, valueColor, null);
  }

  private Component createInfoRow(
      String icon, String label, String value, TextColor valueColor, TextDecoration decoration) {
    TextComponent.Builder builder = Component.text();
    builder.append(Component.text("│ ", BORDER));
    builder.append(Component.text(icon + " ", TEXT));
    builder.append(Component.text(label + ": ", SUBTEXT));

    TextComponent valueComponent = Component.text(value, valueColor);
    if (decoration != null) {
      valueComponent = valueComponent.decorate(decoration);
    }
    builder.append(valueComponent);

    int totalLength = 3 + icon.length() + label.length() + 2 + value.length();
    int padding = 35 - totalLength;
    if (padding > 0) {
      builder.append(Component.text(" ".repeat(Math.max(0, padding))));
    }

    builder.append(Component.text(" │", BORDER));
    return builder.build();
  }

  private Component createActionButton(
      String icon, String text, String command, String hoverText, TextColor color) {
    TextComponent.Builder builder = Component.text();
    builder.append(Component.text("│ ", BORDER));
    builder.append(Component.text("  ", TEXT));

    builder.append(
        Component.text()
            .append(Component.text("[", BORDER))
            .append(Component.text(icon + " " + text, color, TextDecoration.BOLD))
            .append(Component.text("]", BORDER))
            .clickEvent(ClickEvent.suggestCommand(command))
            .hoverEvent(
                HoverEvent.showText(
                    Component.text()
                        .append(Component.text(hoverText, INFO))
                        .append(Component.newline())
                        .append(Component.text("点击执行: " + command, SUBTEXT))
                        .build())));

    int totalLength = 4 + 1 + icon.length() + 1 + text.length() + 1;
    int padding = 35 - totalLength;
    if (padding > 0) {
      builder.append(Component.text(" ".repeat(Math.max(0, padding))));
    }

    builder.append(Component.text(" │", BORDER));
    return builder.build();
  }

  private String formatPlaytime(long seconds) {
    long hours = seconds / 3600;
    long minutes = (seconds % 3600) / 60;
    if (hours > 0) {
      return hours + " 小时 " + minutes + " 分钟";
    }
    return minutes + " 分钟";
  }

  private String formatDuration(Duration duration) {
    long days = duration.toDays();
    long hours = duration.toHours() % 24;
    long minutes = duration.toMinutes() % 60;

    if (days > 0) {
      return days + " 天";
    } else if (hours > 0) {
      return hours + " 小时";
    } else if (minutes > 0) {
      return minutes + " 分钟";
    }
    return "刚刚";
  }

  private class WelcomeListener {
    @Subscribe
    public void onServerConnect(ServerPostConnectEvent event) {
      if (event.getPreviousServer() == null) {
        onPlayerJoin(event.getPlayer());
      }
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
      onPlayerQuit(event.getPlayer());
    }
  }

  public interface Config {
    boolean enabled();

    static Config defaultConfig() {
      return () -> true;
    }
  }
}
