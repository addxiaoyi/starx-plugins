package io.github.addxiaoyi.starx.velocity.module.auth;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.Player;
import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.api.event.EventTypes;
import io.github.addxiaoyi.starx.common.auth.AuthCommandHandler;
import io.github.addxiaoyi.starx.common.auth.AuthResult;
import io.github.addxiaoyi.starx.common.auth.AuthService;
import io.github.addxiaoyi.starx.common.auth.AuthSession;
import io.github.addxiaoyi.starx.common.auth.PremiumResolver;
import io.github.addxiaoyi.starx.common.auth.SessionManager;
import io.github.addxiaoyi.starx.common.config.DatabaseConfig;
import io.github.addxiaoyi.starx.common.database.DatabaseManager;
import io.github.addxiaoyi.starx.common.database.JdbiUserRepository;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import io.github.addxiaoyi.starx.velocity.module.VelocityModule;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboFactory;
import net.elytrium.limboapi.api.chunk.Dimension;
import net.elytrium.limboapi.api.event.LoginLimboRegisterEvent;
import net.elytrium.limboapi.api.player.GameMode;
import net.kyori.adventure.text.Component;

/** 认证模块：在 LimboAPI 注册阶段拦截未登录玩家并送入 Limbo。 */
public final class AuthModule implements VelocityModule {

  private final StarxVelocityPlugin plugin;
  private final EventBus eventBus;

  private DatabaseManager databaseManager;
  private JdbiUserRepository userRepository;
  private SessionManager sessionManager;
  private PremiumResolver premiumResolver;
  private AuthService authService;
  private AuthCommandHandler commandHandler;
  private LimboFactory limboFactory;
  private Limbo authLimbo;

  public AuthModule(StarxVelocityPlugin plugin, EventBus eventBus) {
    this.plugin = plugin;
    this.eventBus = eventBus;
    initDatabase();
    initAuthService();
  }

  public JdbiUserRepository userRepository() {
    return userRepository;
  }

  public AuthService authService() {
    return authService;
  }

  @Override
  public String name() {
    return "auth";
  }

  @Override
  public void onEnable() {
    initLimbo();
    plugin.proxy().getEventManager().register(plugin, new LoginListener());
    new AuthCommands(plugin, authService).onEnable();
  }

  @Override
  public void onDisable() {
    if (authLimbo != null) {
      authLimbo.dispose();
    }
    if (sessionManager != null) {
      sessionManager.shutdown();
    }
    if (databaseManager != null) {
      databaseManager.close();
    }
  }

  private void initDatabase() {
    Path dbPath = plugin.dataDirectory().resolve("auth-db");
    String url = "jdbc:h2:file:" + dbPath.toAbsolutePath().toString().replace("\\", "/");
    DatabaseConfig config = new DatabaseConfig("h2", "", 0, "starx-auth", "sa", "", url, 5, 5_000L);
    this.databaseManager = new DatabaseManager(config);
    this.userRepository = new JdbiUserRepository(databaseManager.getJdbi());
  }

  private void initAuthService() {
    this.sessionManager = new SessionManager(Duration.ofMinutes(10), Instant::now);
    this.premiumResolver = new PremiumResolver();
    this.authService = new AuthService(userRepository, eventBus, sessionManager);
    this.commandHandler = new AuthCommandHandler(authService);
  }

  private void initLimbo() {
    Optional<?> optional =
        plugin
            .proxy()
            .getPluginManager()
            .getPlugin("limboapi")
            .flatMap(PluginContainer::getInstance);
    if (optional.isEmpty() || !(optional.get() instanceof LimboFactory factory)) {
      plugin.logger().log(Level.SEVERE, "LimboAPI 未找到，认证模块无法创建 Limbo");
      return;
    }
    this.limboFactory = factory;
    this.authLimbo =
        factory
            .createLimbo(factory.createVirtualWorld(Dimension.OVERWORLD, 0.5, 64, 0.5, 0.0f, 0.0f))
            .setName("starx-auth")
            .setGameMode(GameMode.ADVENTURE);
  }

  private void sendToTargetServer(Player player) {
    if (limboFactory != null) {
      limboFactory.passLoginLimbo(player);
    } else {
      player.disconnect(Component.text("服务器配置错误，无法完成登录"));
    }
  }

  private void handleAuthResult(Player player, AuthResult result) {
    if (result.success()) {
      if (result.state() == AuthSession.State.AUTHENTICATED) {
        player.sendMessage(Component.text("§a" + result.message() + " 正在进入服务器..."));
        sendToTargetServer(player);
      } else if (result.state() == AuthSession.State.AUTHENTICATING) {
        player.sendMessage(Component.text("§e" + result.message()));
        player.sendMessage(Component.text("§7（可发送 /2fa 命令管理二步验证）"));
      } else {
        player.sendMessage(Component.text("§a" + result.message()));
      }
    } else {
      player.sendMessage(Component.text("§c" + result.message()));
    }
  }

  private InetAddress playerAddress(Player player) {
    InetSocketAddress address = player.getRemoteAddress();
    return address != null ? address.getAddress() : null;
  }

  private String deviceId(Player player) {
    InetSocketAddress address = player.getRemoteAddress();
    return address != null && address.getAddress() != null
        ? address.getAddress().getHostAddress()
        : null;
  }

  private final class LoginListener {

    @Subscribe
    public void onLoginLimboRegister(LoginLimboRegisterEvent event) {
      Player player = event.getPlayer();
      UUID uuid = player.getUniqueId();
      String username = player.getUsername();

      if (premiumResolver.isPremium(uuid, player.isOnlineMode())) {
        authService.autoLogin(uuid, username, playerAddress(player));
        sendToTargetServer(player);
        return;
      }

      eventBus.publish(EventTypes.PLAYER_LOGIN_START, Map.of("uuid", uuid, "username", username));

      if (authLimbo != null) {
        authLimbo.spawnPlayer(
            player,
            new LimboSessionListener(
                player,
                authService,
                commandHandler,
                AuthModule.this::handleAuthResult,
                deviceId(player)));
      } else {
        player.disconnect(Component.text("未安装 LimboAPI，无法认证"));
      }
    }
  }
}
