package io.github.addxiaoyi.starx.velocity.module.proxytools;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import io.github.addxiaoyi.starx.velocity.module.VelocityModule;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.Collection;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * 代理信息命令模块：提供管理命令查看代理运行状态。
 *
 * <p>参考 VelocityTools、VelocityUptime、VServerInfo 插件逻辑。提供 /starx info、/starx uptime、 /starx servers
 * 命令，显示代理运行时间、内存使用、服务器列表、玩家统计。
 */
public final class ProxyInfoModule implements VelocityModule {

  private static final long STARTUP_TIME = System.currentTimeMillis();

  private final StarxVelocityPlugin plugin;
  private final Config config;

  public ProxyInfoModule(StarxVelocityPlugin plugin, Config config) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.config = Objects.requireNonNull(config, "config");
  }

  @Override
  public String name() {
    return "info";
  }

  @Override
  public void onEnable() {
    if (!config.enabled()) {
      return;
    }
    plugin.proxy().getCommandManager().register("starx", new StarxInfoCommand());
  }

  @Override
  public void onDisable() {}

  private String formatUptime() {
    long uptime = System.currentTimeMillis() - STARTUP_TIME;
    long seconds = uptime / 1000;
    long minutes = seconds / 60;
    long hours = minutes / 60;
    long days = hours / 24;
    return String.format("%dd %dh %dm %ds", days, hours % 24, minutes % 60, seconds % 60);
  }

  private String formatMemory() {
    MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    long used = memoryBean.getHeapMemoryUsage().getUsed() / (1024 * 1024);
    long max = memoryBean.getHeapMemoryUsage().getMax() / (1024 * 1024);
    return String.format("%dMB / %dMB", used, max);
  }

  private final class StarxInfoCommand implements SimpleCommand {
    @Override
    public void execute(Invocation invocation) {
      String[] args = invocation.arguments();
      if (args.length == 0) {
        sendHelp(invocation);
        return;
      }
      switch (args[0].toLowerCase()) {
        case "info" -> sendInfo(invocation);
        case "uptime" -> sendUptime(invocation);
        case "servers" -> sendServers(invocation);
        default -> sendHelp(invocation);
      }
    }

    private void sendHelp(Invocation invocation) {
      invocation
          .source()
          .sendMessage(
              Component.text("StarX Proxy Commands:", NamedTextColor.GOLD)
                  .decoration(TextDecoration.BOLD, true));
      invocation
          .source()
          .sendMessage(
              Component.text("  /starx info    - Proxy status info", NamedTextColor.YELLOW));
      invocation
          .source()
          .sendMessage(Component.text("  /starx uptime  - Proxy uptime", NamedTextColor.YELLOW));
      invocation
          .source()
          .sendMessage(Component.text("  /starx servers - Server list", NamedTextColor.YELLOW));
    }

    private void sendInfo(Invocation invocation) {
      ProxyServer proxy = plugin.proxy();
      invocation
          .source()
          .sendMessage(
              Component.text("==== StarX Proxy Info ====", NamedTextColor.GOLD)
                  .decoration(TextDecoration.BOLD, true));
      invocation
          .source()
          .sendMessage(
              Component.text("Players: ", NamedTextColor.WHITE)
                  .append(Component.text(proxy.getPlayerCount(), NamedTextColor.GREEN)));
      invocation
          .source()
          .sendMessage(
              Component.text("Servers: ", NamedTextColor.WHITE)
                  .append(Component.text(proxy.getAllServers().size(), NamedTextColor.GREEN)));
      invocation
          .source()
          .sendMessage(
              Component.text("Uptime: ", NamedTextColor.WHITE)
                  .append(Component.text(formatUptime(), NamedTextColor.GREEN)));
      invocation
          .source()
          .sendMessage(
              Component.text("Memory: ", NamedTextColor.WHITE)
                  .append(Component.text(formatMemory(), NamedTextColor.GREEN)));
    }

    private void sendUptime(Invocation invocation) {
      invocation
          .source()
          .sendMessage(
              Component.text("Proxy uptime: ", NamedTextColor.GOLD)
                  .append(Component.text(formatUptime(), NamedTextColor.GREEN)));
    }

    private void sendServers(Invocation invocation) {
      ProxyServer proxy = plugin.proxy();
      Collection<RegisteredServer> servers = proxy.getAllServers();
      invocation
          .source()
          .sendMessage(
              Component.text("==== Servers (" + servers.size() + ") ====", NamedTextColor.GOLD)
                  .decoration(TextDecoration.BOLD, true));
      for (RegisteredServer server : servers) {
        int playerCount = server.getPlayersConnected().size();
        String status = server.ping() != null ? "Online" : "Offline";
        NamedTextColor color = "Online".equals(status) ? NamedTextColor.GREEN : NamedTextColor.RED;
        invocation
            .source()
            .sendMessage(
                Component.text("  " + server.getServerInfo().getName() + ": ", NamedTextColor.WHITE)
                    .append(Component.text(status + " (" + playerCount + " players)", color)));
      }
    }
  }

  /** 模块配置。 */
  public interface Config {
    boolean enabled();

    static Config defaultConfig() {
      return () -> true;
    }

    static Config disabled() {
      return () -> false;
    }
  }
}
