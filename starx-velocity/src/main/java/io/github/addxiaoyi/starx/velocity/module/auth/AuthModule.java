package io.github.addxiaoyi.starx.velocity.module.auth;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.api.event.EventTypes;
import io.github.addxiaoyi.starx.common.auth.AuthCommandHandler;
import io.github.addxiaoyi.starx.common.auth.AuthResult;
import io.github.addxiaoyi.starx.common.auth.AuthService;
import io.github.addxiaoyi.starx.common.auth.AuthSession;
import io.github.addxiaoyi.starx.common.auth.PremiumResolver;
import io.github.addxiaoyi.starx.common.auth.SessionManager;
import io.github.addxiaoyi.starx.common.auth.uniauth.UniAuthBridge;
import io.github.addxiaoyi.starx.common.auth.uniauth.UniAuthClient;
import io.github.addxiaoyi.starx.common.auth.uniauth.UniAuthConfig;
import io.github.addxiaoyi.starx.common.config.DatabaseConfig;
import io.github.addxiaoyi.starx.common.database.DatabaseManager;
import io.github.addxiaoyi.starx.common.database.JdbcUserRepository;
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

/** 认证模块：在 LimboAPI 注册阶段拦截未登录玩家并送入 Limbo；无 LimboAPI 时自动降级到直接登录。 */
public final class AuthModule implements VelocityModule {

  private final StarxVelocityPlugin plugin;
  private final EventBus eventBus;
  private final UniAuthConfig uniauthConfig;

  private DatabaseManager databaseManager;
  private JdbcUserRepository userRepository;
  private SessionManager sessionManager;
  private PremiumResolver premiumResolver;
  private AuthService authService;
  private AuthCommandHandler commandHandler;
  private LimboFactory limboFactory;
  private Limbo authLimbo;
  private UniAuthClient uniauthClient;
  private UniAuthBridge uniauthBridge;

  public AuthModule(StarxVelocityPlugin plugin, EventBus eventBus) {
    this(plugin, eventBus, UniAuthConfig.defaults());
  }

  public AuthModule(StarxVelocityPlugin plugin, EventBus eventBus, UniAuthConfig uniauthConfig) {
    this.plugin = plugin;
    this.eventBus = eventBus;
    this.uniauthConfig = uniauthConfig;
    initDatabase();
    initUniAuth();
    initAuthService();
  }

  public JdbcUserRepository userRepository() {
    return userRepository;
  }

  public AuthService authService() {
    return authService;
  }

  @Override
  public String name() {
    return "starx.auth";
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
    Path dbPath = plugin.dataDirectory().resolve("data.db");
    DatabaseConfig config =
        new DatabaseConfig(
            "sqlite",
            "",
            0,
            dbPath.toAbsolutePath().toString().replace("\\", "/"),
            "starx",
            "",
            "",
            2,
            5_000L);
    this.databaseManager = new DatabaseManager(config);
    this.userRepository = new JdbcUserRepository(databaseManager.getDataSource());
  }

  private void initUniAuth() {
    if (uniauthConfig.enabled()) {
      this.uniauthClient = new UniAuthClient(uniauthConfig);
      this.uniauthBridge = new UniAuthBridge(uniauthConfig, uniauthClient, userRepository);
      plugin.logger().log(Level.INFO, "UniAuth 桥接已启用: {0}", uniauthConfig.apiUrl());
    }
  }

  private void initAuthService() {
    this.sessionManager = new SessionManager(Duration.ofMinutes(10), Instant::now);
    this.premiumResolver = new PremiumResolver();
    this.authService =
        new AuthService(userRepository, eventBus, sessionManager, uniauthConfig, uniauthBridge);
    this.commandHandler = new AuthCommandHandler(authService);
  }

  private void initLimbo() {
    Optional<PluginContainer> container = plugin.proxy().getPluginManager().getPlugin("limboapi");
    if (container.isEmpty()) {
      plugin.logger().log(Level.WARNING, "LimboAPI 未安装，将使用无 Limbo 模式");
      return;
    }
    Optional<?> instance = container.get().getInstance();
    if (instance.isEmpty()) {
      plugin.logger().log(Level.SEVERE, "LimboAPI 已安装但加载失败（请检查控制台错误日志），将使用无 Limbo 模式");
      return;
    }
    if (!(instance.get() instanceof LimboFactory factory)) {
      plugin.logger().log(Level.SEVERE, "LimboAPI 实例类型异常，将使用无 Limbo 模式");
      return;
    }
    this.limboFactory = factory;
    this.authLimbo =
        factory
            .createLimbo(factory.createVirtualWorld(Dimension.OVERWORLD, 0.5, 64, 0.5, 0.0f, 0.0f))
            .setName("starx-auth")
            .setGameMode(GameMode.ADVENTURE);
    plugin.logger().log(Level.INFO, "LimboAPI 已加载");
  }

  private static final Component NOT_FOUND = Component.text("未找到 lobby 服务器");

  private void sendToTargetServer(Player player) {
    String targetServer = "lobby";
    plugin
        .logger()
        .log(
            Level.INFO, "玩家 {0} 认证完成，准备发送至 {1}", new Object[] {player.getUsername(), targetServer});

    var optServer = plugin.proxy().getServer(targetServer);
    if (optServer.isEmpty()) {
      plugin
          .logger()
          .log(
              Level.SEVERE,
              "目标服务器 {0} 不存在，断开玩家 {1}",
              new Object[] {targetServer, player.getUsername()});
      player.disconnect(NOT_FOUND);
      return;
    }

    if (limboFactory != null) {
      try {
        limboFactory.passLoginLimbo(player);
        plugin
            .logger()
            .log(
                Level.INFO,
                "玩家 {0} 已通过 passLoginLimbo 切入 {1}",
                new Object[] {player.getUsername(), targetServer});
      } catch (Exception e) {
        plugin.logger().log(Level.WARNING, "passLoginLimbo 失败，回退到直接连接: {0}", e.getMessage());
        fallbackConnect(player, optServer.get(), targetServer);
      }
    } else {
      fallbackConnect(player, optServer.get(), targetServer);
    }
  }

  private void fallbackConnect(Player player, RegisteredServer server, String serverName) {
    player.createConnectionRequest(server).fireAndForget();
    plugin
        .logger()
        .log(Level.INFO, "玩家 {0} 已发送连接请求到 {1}", new Object[] {player.getUsername(), serverName});
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

  private void handleLogin(Player player) {
    UUID uuid = player.getUniqueId();
    String username = player.getUsername();
    String addr = deviceId(player);

    plugin.logger().log(Level.INFO, "玩家尝试登录: {0} ({1}) @ {2}", new Object[] {username, uuid, addr});

    if (premiumResolver.isPremium(uuid, player.isOnlineMode())) {
      plugin.logger().log(Level.INFO, "正版玩家自动认证通过: {0}", username);
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
      plugin.logger().log(Level.INFO, "离线玩家 {0} 已切入 Limbo 等待认证", username);
    } else {
      plugin.logger().log(Level.WARNING, "LimboAPI 未加载；离线玩家 {0} 被拒绝连接", username);
      if (authService.isUserRegistered(uuid)) {
        player.disconnect(Component.text("LimboAPI 未正确加载，无法完成认证。请联系管理员。"));
      } else {
        player.disconnect(Component.text("请先在网站注册账号，或安装 LimboAPI 以支持游戏内注册"));
      }
    }
  }

  private final class LoginListener {

    @Subscribe
    public void onLoginLimboRegister(LoginLimboRegisterEvent event) {
      handleLogin(event.getPlayer());
    }

    @Subscribe
    public void onLogin(LoginEvent event) {
      if (authLimbo == null) {
        handleLogin(event.getPlayer());
      }
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
      Player player = event.getPlayer();
      plugin
          .logger()
          .log(
              Level.INFO,
              "玩家断开连接: {0} ({1})",
              new Object[] {player.getUsername(), player.getUniqueId()});
    }

    @Subscribe
    public void onKicked(KickedFromServerEvent event) {
      Player player = event.getPlayer();
      String reason = event.getServerKickReason().map(Object::toString).orElse("无");
      String originalResult = event.getResult().toString();
      plugin
          .logger()
          .log(
              Level.WARNING,
              "玩家 {0} 从服务器被踢 (原因: {1}) → {2}",
              new Object[] {player.getUsername(), reason, originalResult});
    }
  }
}
