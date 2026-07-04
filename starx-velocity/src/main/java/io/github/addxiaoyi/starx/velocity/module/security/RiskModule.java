package io.github.addxiaoyi.starx.velocity.module.security;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.api.event.StarxEvent;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import io.github.addxiaoyi.starx.velocity.module.VelocityModule;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 风控引擎模块：IP 地理位置检查、ASN 检测、新设备登录检测。
 *
 * <p>自研实现，登录时对玩家 IP 进行风险评分，高风险 IP 触发额外验证（如 TOTP）。
 */
public final class RiskModule implements VelocityModule {

  private final StarxVelocityPlugin plugin;
  private final EventBus eventBus;
  private final Config config;

  private final Map<UUID, String> deviceRegistry = new ConcurrentHashMap<>();

  public RiskModule(StarxVelocityPlugin plugin, EventBus eventBus, Config config) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    this.config = Objects.requireNonNull(config, "config");
  }

  @Override
  public String name() {
    return "security.risk";
  }

  @Override
  public void onEnable() {
    plugin.proxy().getEventManager().register(plugin, new LoginListener());
  }

  @Override
  public void onDisable() {
    deviceRegistry.clear();
  }

  /** 对 IP 地址进行风险评分。 */
  int scoreIp(String ip) {
    int score = 0;
    // TODO: 集成 IP 地理位置 API（如 ip-api.com, MaxMind GeoIP）
    // TODO: 集成 ASN 数据库检查
    // TODO: 检查 IP 是否在已知的恶意 IP 列表中
    return score;
  }

  /** 判断风险评分是否达到高风险阈值。 */
  boolean isHighRisk(int score) {
    return score >= config.highRiskThreshold();
  }

  /** 判断是否需要 TOTP 验证。 */
  boolean requiresTotp(int score) {
    return config.requireTotpForHighRisk() && isHighRisk(score);
  }

  /** 检查是否为新设备登录。 */
  boolean isNewDevice(UUID playerId, String ip) {
    String registeredIp = deviceRegistry.get(playerId);
    return registeredIp == null || !registeredIp.equals(ip);
  }

  /** 注册设备 IP。 */
  void registerDevice(UUID playerId, String ip) {
    deviceRegistry.put(playerId, ip);
  }

  void onLogin(LoginEvent event) {
    InetSocketAddress address = event.getPlayer().getRemoteAddress();
    if (address == null) {
      return;
    }
    String ip = address.getAddress().getHostAddress();
    UUID playerId = event.getPlayer().getUniqueId();
    String username = event.getPlayer().getUsername();

    int riskScore = scoreIp(ip);

    if (config.checkNewDevice() && isNewDevice(playerId, ip)) {
      riskScore += 10;
    }

    if (isHighRisk(riskScore)) {
      eventBus.publish(
          new StarxEvent(
              SecurityEvents.RISK_HIGH,
              Map.of("uuid", playerId, "username", username, "ip", ip, "score", riskScore)));

      if (requiresTotp(riskScore)) {
        eventBus.publish(
            new StarxEvent(
                SecurityEvents.RISK_VERIFY_REQUIRED,
                Map.of("uuid", playerId, "username", username, "ip", ip)));
      }
    }

    registerDevice(playerId, ip);
  }

  public interface Config {
    int highRiskThreshold();

    boolean requireTotpForHighRisk();

    boolean checkNewDevice();

    boolean checkAsn();

    static Config defaultConfig() {
      return new Config() {
        @Override
        public int highRiskThreshold() {
          return 70;
        }

        @Override
        public boolean requireTotpForHighRisk() {
          return false;
        }

        @Override
        public boolean checkNewDevice() {
          return true;
        }

        @Override
        public boolean checkAsn() {
          return false;
        }
      };
    }
  }

  private final class LoginListener {
    @Subscribe
    public void onLogin(LoginEvent event) {
      RiskModule.this.onLogin(event);
    }
  }
}
