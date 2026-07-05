package io.github.addxiaoyi.starx.velocity.module.security;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import io.github.addxiaoyi.starx.common.security.BushClient;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import io.github.addxiaoyi.starx.velocity.module.VelocityModule;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.logging.Level;

/** 在 Velocity 代理层通过反射替换 ChannelInitializer，在连接建立前拦截黑名单 IP。 使用纯反射避免编译时依赖 Netty。 */
public final class BlossomGuardModule implements VelocityModule {

  private static final String CHANNEL_INITIALIZER_CLASS = "io.netty.channel.ChannelInitializer";
  private static final String CHANNEL_CLASS = "io.netty.channel.Channel";
  private static final String INET_SOCKET_ADDRESS_CLASS = "java.net.InetSocketAddress";

  private final StarxVelocityPlugin plugin;
  private final BushClient bushClient;
  private Object originalInitializer;

  public BlossomGuardModule(StarxVelocityPlugin plugin) {
    this(plugin, new BushClient());
  }

  public BlossomGuardModule(StarxVelocityPlugin plugin, BushClient bushClient) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.bushClient = Objects.requireNonNull(bushClient, "bushClient");
  }

  @Override
  public String name() {
    return "security.blossom";
  }

  @Override
  public void onEnable() {
    plugin.proxy().getEventManager().register(plugin, new InitListener());
  }

  @Override
  public void onDisable() {
    restoreOriginalInitializer();
  }

  void replaceChannelInitializer() {
    try {
      Object proxyServer = plugin.proxy();
      Field cmField = proxyServer.getClass().getDeclaredField("cm");
      cmField.setAccessible(true);
      Object connectionManager = cmField.get(proxyServer);

      Method getInitializerMethod =
          connectionManager.getClass().getDeclaredMethod("getServerChannelInitializer");
      getInitializerMethod.setAccessible(true);
      Object initializerHolder = getInitializerMethod.invoke(connectionManager);

      Method getMethod = initializerHolder.getClass().getDeclaredMethod("get");
      getMethod.setAccessible(true);
      originalInitializer = getMethod.invoke(initializerHolder);

      Object newInitializer = createBlossomInitializer(bushClient, originalInitializer);

      Class<?> channelInitializerClass = Class.forName(CHANNEL_INITIALIZER_CLASS);
      Method setMethod = initializerHolder.getClass().getMethod("set", channelInitializerClass);
      setMethod.invoke(initializerHolder, newInitializer);

      plugin.logger().info("BlossomGuard: ChannelInitializer replaced successfully");
    } catch (Exception e) {
      plugin.logger().log(Level.WARNING, "BlossomGuard: Failed to replace ChannelInitializer", e);
    }
  }

  void restoreOriginalInitializer() {
    if (originalInitializer == null) {
      return;
    }
    try {
      Object proxyServer = plugin.proxy();
      Field cmField = proxyServer.getClass().getDeclaredField("cm");
      cmField.setAccessible(true);
      Object connectionManager = cmField.get(proxyServer);

      Method getInitializerMethod =
          connectionManager.getClass().getDeclaredMethod("getServerChannelInitializer");
      getInitializerMethod.setAccessible(true);
      Object initializerHolder = getInitializerMethod.invoke(connectionManager);

      Class<?> channelInitializerClass = Class.forName(CHANNEL_INITIALIZER_CLASS);
      Method setMethod = initializerHolder.getClass().getMethod("set", channelInitializerClass);
      setMethod.invoke(initializerHolder, originalInitializer);

      plugin.logger().info("BlossomGuard: ChannelInitializer restored");
    } catch (Exception e) {
      plugin.logger().log(Level.WARNING, "BlossomGuard: Failed to restore ChannelInitializer", e);
    }
  }

  /** 通过反射创建 BlossomInitializer 子类实例。 */
  private static Object createBlossomInitializer(BushClient bushClient, Object original)
      throws Exception {
    Class<?> channelInitializerClass = Class.forName(CHANNEL_INITIALIZER_CLASS);
    Class<?> channelClass = Class.forName(CHANNEL_CLASS);
    Method initChannelMethod =
        channelInitializerClass.getDeclaredMethod("initChannel", channelClass);
    initChannelMethod.setAccessible(true);

    return java.lang.reflect.Proxy.newProxyInstance(
        channelInitializerClass.getClassLoader(),
        new Class<?>[] {channelInitializerClass},
        (proxy, method, args) -> {
          if ("initChannel".equals(method.getName()) && args.length == 1) {
            Object ch = args[0];
            Method getRemoteAddr = ch.getClass().getMethod("remoteAddress");
            Object remoteAddr = getRemoteAddr.invoke(ch);
            if (remoteAddr instanceof InetSocketAddress addr) {
              if (bushClient.isIpBlacklisted(addr.getAddress().getHostAddress())) {
                Method disconnect = ch.getClass().getMethod("disconnect");
                disconnect.invoke(ch);
                return null;
              }
            }
            initChannelMethod.invoke(original, ch);
            return null;
          }
          return method.invoke(original, args);
        });
  }

  BushClient getBushClient() {
    return bushClient;
  }

  private final class InitListener {
    @Subscribe
    public void onProxyInit(ProxyInitializeEvent event) {
      replaceChannelInitializer();
    }
  }
}
