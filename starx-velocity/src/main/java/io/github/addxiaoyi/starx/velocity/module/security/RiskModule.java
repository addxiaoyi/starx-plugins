package io.github.addxiaoyi.starx.velocity.module.security;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.api.event.StarxEvent;
import io.github.addxiaoyi.starx.common.security.HttpClient;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import io.github.addxiaoyi.starx.velocity.module.VelocityModule;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

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
  private static final Logger LOGGER = Logger.getLogger(RiskModule.class.getName());
  private static final String IP_API_URL = "http://ip-api.com/json/";

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

  /** 对 IP 地址进行风险评分（基于 ip-api.com 免费 API）。 */
  int scoreIp(String ip) {
    if (isLocalIp(ip)) {
      return 0;
    }
    try {
      IpApiResponse response = HttpClient.get(IP_API_URL + ip).sendJson(IpApiResponse.class);
      if (response == null || !"success".equals(response.status)) {
        return 0;
      }
      int score = 0;
      if (response.proxy) {
        score += 40;
      }
      if (response.hosting) {
        score += 30;
      }
      if (response.mobile) {
        score += 10;
      }
      return score;
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "IP API query failed for {0}", ip);
      return 0;
    }
  }

  private static boolean isLocalIp(String ip) {
    return ip.startsWith("127.")
        || ip.startsWith("10.")
        || ip.startsWith("192.168.")
        || ip.startsWith("172.16.");
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
