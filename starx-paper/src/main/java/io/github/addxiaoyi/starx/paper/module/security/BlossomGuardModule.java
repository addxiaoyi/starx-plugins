package io.github.addxiaoyi.starx.paper.module.security;

import io.github.addxiaoyi.starx.common.security.BushClient;
import io.github.addxiaoyi.starx.paper.StarxPaperPlugin;
import io.github.addxiaoyi.starx.paper.config.PaperConfigLoader;
import io.github.addxiaoyi.starx.paper.module.PaperModule;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.logging.Level;

/**
 * 在 Paper 后端通过反射调用 ChannelInitializeListenerHolder 拦截连接，阻止黑名单 IP 进入。 使用纯反射避免编译时依赖 Netty 和 Paper 内部
 * API。
 */
public final class BlossomGuardModule implements PaperModule {

  private static final String CHANNEL_INITIALIZER_LISTENER_CLASS =
      "io.papermc.paper.network.ChannelInitializeListenerHolder";
  private static final String KEY_CLASS = "net.kyori.adventure.key.Key";
  private static final String CHANNEL_INBOUND_HANDLER_ADAPTER =
      "io.netty.channel.ChannelInboundHandlerAdapter";

  private final StarxPaperPlugin plugin;
  private final PaperConfigLoader configLoader;
  private final BushClient bushClient;

  public BlossomGuardModule(StarxPaperPlugin plugin, PaperConfigLoader configLoader) {
    this(plugin, configLoader, new BushClient());
  }

  public BlossomGuardModule(
      StarxPaperPlugin plugin, PaperConfigLoader configLoader, BushClient bushClient) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.configLoader = Objects.requireNonNull(configLoader, "configLoader");
    this.bushClient = Objects.requireNonNull(bushClient, "bushClient");
  }

  @Override
  public String getName() {
    return "security.blossom";
  }

  @Override
  public boolean isEnabled() {
    return configLoader.isModuleEnabled("security.blossom");
  }

  @Override
  public void onEnable() {
    try {
      Class<?> holderClass = Class.forName(CHANNEL_INITIALIZER_LISTENER_CLASS);
      Class<?> keyClass = Class.forName(KEY_CLASS);

      Method keyMethod = keyClass.getMethod("key", String.class, String.class);
      Object key = keyMethod.invoke(null, "starx", "blossom_guard");

      Object handler = createGuardHandler(bushClient);

      Method addListenerMethod =
          holderClass.getMethod(
              "addListener", keyClass, Class.forName("java.util.function.Consumer"));
      addListenerMethod.invoke(null, key, handler);

      plugin.getLogger().info("BlossomGuard: Registered channel listener via reflection");
    } catch (Exception e) {
      plugin.getLogger().log(Level.WARNING, "BlossomGuard: Failed to register channel listener", e);
    }
  }

  @Override
  public void onDisable() {}

  private Object createGuardHandler(BushClient bushClient) throws Exception {
    Class<?> handlerAdapterClass = Class.forName(CHANNEL_INBOUND_HANDLER_ADAPTER);
    Class<?> ctxClass = Class.forName("io.netty.channel.ChannelHandlerContext");

    return Proxy.newProxyInstance(
        handlerAdapterClass.getClassLoader(),
        new Class<?>[] {Class.forName("java.util.function.Consumer")},
        (proxy, method, args) -> {
          if ("accept".equals(method.getName()) && args.length == 1) {
            Object channel = args[0];
            Method pipeline = channel.getClass().getMethod("pipeline");
            Object pipelineObj = pipeline.invoke(channel);
            Method addFirst =
                pipelineObj.getClass().getMethod("addFirst", Object.class, Object.class);

            Object guardHandler = createNettyHandler(bushClient, ctxClass);
            addFirst.invoke(pipelineObj, "starx_blossom_guard", guardHandler);
            return null;
          }
          return null;
        });
  }

  private Object createNettyHandler(BushClient bushClient, Class<?> ctxClass) throws Exception {
    Class<?> handlerAdapterClass = Class.forName(CHANNEL_INBOUND_HANDLER_ADAPTER);
    return Proxy.newProxyInstance(
        handlerAdapterClass.getClassLoader(),
        new Class<?>[] {handlerAdapterClass},
        new BlossomHandlerInvocationHandler(bushClient, ctxClass));
  }

  BushClient getBushClient() {
    return bushClient;
  }

  private static final class BlossomHandlerInvocationHandler implements InvocationHandler {

    private final BushClient bushClient;
    private final Class<?> ctxClass;
    private final Method channelActiveSuper;
    private final Method channelReadSuper;
    private boolean blacklisted;

    BlossomHandlerInvocationHandler(BushClient bushClient, Class<?> ctxClass) throws Exception {
      this.bushClient = bushClient;
      this.ctxClass = ctxClass;

      Class<?> handlerAdapterClass = Class.forName(CHANNEL_INBOUND_HANDLER_ADAPTER);
      this.channelActiveSuper = handlerAdapterClass.getDeclaredMethod("channelActive", ctxClass);
      this.channelActiveSuper.setAccessible(true);
      this.channelReadSuper =
          handlerAdapterClass.getDeclaredMethod("channelRead", ctxClass, Object.class);
      this.channelReadSuper.setAccessible(true);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      String methodName = method.getName();

      if ("channelActive".equals(methodName) && args.length == 1) {
        Object ctx = args[0];
        channelActiveSuper.invoke(this, determineTarget(ctx), ctx);

        Method channelMethod = ctxClass.getMethod("channel");
        Object channel = channelMethod.invoke(ctx);
        Method remoteAddrMethod = channel.getClass().getMethod("remoteAddress");
        Object remoteAddr = remoteAddrMethod.invoke(channel);

        if (remoteAddr instanceof InetSocketAddress addr) {
          if (bushClient.isIpBlacklisted(addr.getAddress().getHostAddress())) {
            blacklisted = true;
            Method disconnectMethod = ctxClass.getMethod("disconnect");
            disconnectMethod.invoke(ctx);
            return null;
          }
        }
        Method pipelineMethod = channel.getClass().getMethod("pipeline");
        Object pipeline = pipelineMethod.invoke(channel);
        Method removeMethod = pipeline.getClass().getMethod("remove", Object.class);
        removeMethod.invoke(pipeline, proxy);
        return null;
      }

      if ("channelRead".equals(methodName) && args.length == 2) {
        if (!blacklisted) {
          channelReadSuper.invoke(this, determineTarget(args[0]), args[0], args[1]);
        }
        return null;
      }

      return null;
    }

    /** 确定方法调用的目标实例（处理 Proxy 实例的 super 调用）。 */
    private Object determineTarget(Object ctx) {
      return this;
    }
  }
}
