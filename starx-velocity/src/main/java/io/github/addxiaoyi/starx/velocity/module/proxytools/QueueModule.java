package io.github.addxiaoyi.starx.velocity.module.proxytools;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import io.github.addxiaoyi.starx.velocity.module.VelocityModule;
import io.github.addxiaoyi.starx.velocity.module.proxytools.queue.QueueService;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/** 服务器满时按 FIFO 排队放行的模块。 */
public final class QueueModule implements VelocityModule {

  private final StarxVelocityPlugin plugin;
  private final Config config;
  private final QueueService queueService;

  public QueueModule(StarxVelocityPlugin plugin, Config config, QueueService queueService) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.config = Objects.requireNonNull(config, "config");
    this.queueService = Objects.requireNonNull(queueService, "queueService");
  }

  @Override
  public String name() {
    return "queue";
  }

  @Override
  public void onEnable() {
    ProxyServer proxy = plugin.proxy();
    proxy.getEventManager().register(plugin, new QueueListener());
    proxy
        .getScheduler()
        .buildTask(plugin, this::processQueues)
        .repeat(Duration.ofMillis(config.checkIntervalMillis()))
        .schedule();
  }

  public QueueService queueService() {
    return queueService;
  }

  void onKicked(KickedFromServerEvent event) {
    Optional<Component> reason = event.getServerKickReason();
    if (reason.isEmpty() || !isFullReason(reason.get())) {
      return;
    }
    queueService.enqueue(event.getServer(), event.getPlayer());
    event.setResult(KickedFromServerEvent.Notify.create(Component.text(config.queueMessage())));
  }

  void onServerConnected(ServerConnectedEvent event) {
    queueService.removeFromQueue(event.getServer(), event.getPlayer());
  }

  void onDisconnect(DisconnectEvent event) {
    Player player = event.getPlayer();
    player
        .getCurrentServer()
        .ifPresent(
            connection ->
                plugin
                    .proxy()
                    .getScheduler()
                    .buildTask(plugin, () -> processQueueFor(connection.getServer()))
                    .schedule());
  }

  private void processQueueFor(RegisteredServer server) {
    processQueues();
  }

  void processQueues() {
    ProxyServer proxy = plugin.proxy();
    queueService.processQueues(
        (player, serverName) -> {
          RegisteredServer server = proxy.getServer(serverName).orElse(null);
          if (server == null) {
            return false;
          }
          try {
            return player
                .createConnectionRequest(server)
                .connect()
                .thenApply(result -> result.isSuccessful())
                .exceptionally(ex -> false)
                .join();
          } catch (Exception e) {
            return false;
          }
        });
  }

  private boolean isFullReason(Component reason) {
    String text =
        PlainTextComponentSerializer.plainText().serialize(reason).toLowerCase(Locale.ROOT);
    return config.fullPatterns().stream().anyMatch(text::contains);
  }

  public interface Config {
    Set<String> fullPatterns();

    String queueMessage();

    long checkIntervalMillis();

    static Config defaultConfig() {
      return new Config() {
        @Override
        public Set<String> fullPatterns() {
          return Set.of("full", "已满", "is full");
        }

        @Override
        public String queueMessage() {
          return "Server is full, you are queued.";
        }

        @Override
        public long checkIntervalMillis() {
          return 3000L;
        }
      };
    }
  }

  private final class QueueListener {
    @Subscribe
    public void onKicked(KickedFromServerEvent event) {
      QueueModule.this.onKicked(event);
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
      QueueModule.this.onServerConnected(event);
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
      QueueModule.this.onDisconnect(event);
    }
  }
}
