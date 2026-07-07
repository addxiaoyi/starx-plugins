package io.github.addxiaoyi.starx.velocity.context;

import com.velocitypowered.api.proxy.Player;
import io.github.addxiaoyi.starx.common.database.JdbcBindingRepository;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * LuckPerms 绑定状态 Context Calculator。
 * 使用反射避免编译期依赖；若 LuckPerms 未加载则静默跳过。
 */
public final class BindingContextCalculator {

  private static final String LUCKPERMS_CLASS = "net.luckperms.api.LuckPermsProvider";
  private static final String CONTEXT_MANAGER_CLASS = "net.luckperms.api.context.ContextManager";

  private final JdbcBindingRepository bindingRepo;

  public BindingContextCalculator(JdbcBindingRepository bindingRepo) {
    this.bindingRepo = Objects.requireNonNull(bindingRepo, "bindingRepo");
  }

  public void register() {
    try {
      Class.forName(LUCKPERMS_CLASS);
      registerWithReflection();
    } catch (ClassNotFoundException e) {
      // LuckPerms not loaded, skip
    } catch (Exception e) {
      // Log but don't crash
    }
  }

  private void registerWithReflection() throws Exception {
    Method getMethod = Class.forName(LUCKPERMS_CLASS).getMethod("get");
    Object luckPerms = getMethod.invoke(null);

    Method getContextManager = luckPerms.getClass().getMethod("getContextManager");
    Object contextManager = getContextManager.invoke(luckPerms);

    Object calculator = createCalculatorProxy();
    Method registerMethod = Class.forName(CONTEXT_MANAGER_CLASS)
        .getMethod("registerCalculator", Class.forName("net.luckperms.api.context.ContextCalculator"));
    registerMethod.invoke(contextManager, calculator);
  }

  @SuppressWarnings("unchecked")
  private Object createCalculatorProxy() {
    Class<?> contextCalculatorClass;
    try {
      contextCalculatorClass = Class.forName("net.luckperms.api.context.ContextCalculator");
    } catch (ClassNotFoundException e) {
      return null;
    }

    return Proxy.newProxyInstance(
        contextCalculatorClass.getClassLoader(),
        new Class<?>[]{contextCalculatorClass},
        (proxy, method, args) -> {
          if ("calculate".equals(method.getName()) && args.length == 2) {
            Player target = (Player) args[0];
            Object consumer = args[1];
            if (target != null && consumer != null) {
              var binding = bindingRepo.findByPlayer(target.getUniqueId());
              boolean qqBound = binding.isPresent() && binding.get().qqId() != null;
              boolean discordBound = binding.isPresent() && binding.get().discordId() != null;

              Method acceptMethod = consumer.getClass().getMethod("accept", String.class, String.class);
              acceptMethod.invoke(consumer, "qq-bound", String.valueOf(qqBound));
              acceptMethod.invoke(consumer, "discord-bound", String.valueOf(discordBound));
            }
            return null;
          }
          if ("estimatePotentialContexts".equals(method.getName())) {
            Class<?> immutableSetBuilder = Class.forName("net.luckperms.api.context.ImmutableContextSet")
                .getDeclaredClasses()[0];
            Object builder = immutableSetBuilder
                .getMethod("builder")
                .invoke(null);
            builder.getClass().getMethod("add", String.class, String.class).invoke(builder, "qq-bound", "true");
            builder.getClass().getMethod("add", String.class, String.class).invoke(builder, "qq-bound", "false");
            builder.getClass().getMethod("add", String.class, String.class).invoke(builder, "discord-bound", "true");
            builder.getClass().getMethod("add", String.class, String.class).invoke(builder, "discord-bound", "false");
            return builder.getClass().getMethod("build").invoke(builder);
          }
          return null;
        });
  }
}
