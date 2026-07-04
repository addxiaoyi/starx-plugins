package io.github.addxiaoyi.starx.velocity.module.auth;

import com.velocitypowered.api.event.Subscribe;
import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.api.event.EventTypes;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import io.github.addxiaoyi.starx.velocity.module.VelocityModule;
import java.util.Map;
import net.elytrium.limboapi.api.event.LoginLimboRegisterEvent;

/** 认证模块：在 LimboAPI 注册阶段拦截未登录玩家并送入 Limbo。 */
public final class AuthModule implements VelocityModule {

  private final StarxVelocityPlugin plugin;
  private final EventBus eventBus;

  public AuthModule(StarxVelocityPlugin plugin, EventBus eventBus) {
    this.plugin = plugin;
    this.eventBus = eventBus;
  }

  @Override
  public String name() {
    return "auth";
  }

  @Override
  public void onEnable() {
    plugin.proxy().getEventManager().register(plugin, new LoginListener());
  }

  private final class LoginListener {

    @Subscribe
    public void onLoginLimboRegister(LoginLimboRegisterEvent event) {
      // TODO: 创建 Limbo 并将未登录玩家重定向进去
      eventBus.publish(
          EventTypes.PLAYER_LOGIN_START,
          Map.of("uuid", event.getPlayer().getUniqueId().toString()));
    }
  }
}
