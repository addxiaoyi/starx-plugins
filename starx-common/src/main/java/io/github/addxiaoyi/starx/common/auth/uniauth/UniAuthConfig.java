package io.github.addxiaoyi.starx.common.auth.uniauth;

import java.util.Objects;

/** UniAuth 服务配置。 */
public final class UniAuthConfig {

  private final boolean enabled;
  private final String apiUrl;
  private final String appId;
  private final String appSecret;
  private final int timeoutMs;
  private final boolean bridgeMode;

  public UniAuthConfig(
      boolean enabled,
      String apiUrl,
      String appId,
      String appSecret,
      int timeoutMs,
      boolean bridgeMode) {
    this.enabled = enabled;
    this.apiUrl = Objects.requireNonNull(apiUrl, "apiUrl");
    this.appId = Objects.requireNonNull(appId, "appId");
    this.appSecret = Objects.requireNonNull(appSecret, "appSecret");
    this.timeoutMs = timeoutMs <= 0 ? 5000 : timeoutMs;
    this.bridgeMode = bridgeMode;
  }

  public static UniAuthConfig defaults() {
    return new UniAuthConfig(false, "https://api.example.com/uniauth/", "", "", 5000, false);
  }

  public boolean enabled() {
    return enabled;
  }

  public String apiUrl() {
    return apiUrl;
  }

  public String appId() {
    return appId;
  }

  public String appSecret() {
    return appSecret;
  }

  public int timeoutMs() {
    return timeoutMs;
  }

  public boolean bridgeMode() {
    return bridgeMode;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;
    var that = (UniAuthConfig) obj;
    return this.enabled == that.enabled
        && Objects.equals(this.apiUrl, that.apiUrl)
        && Objects.equals(this.appId, that.appId)
        && Objects.equals(this.appSecret, that.appSecret)
        && this.timeoutMs == that.timeoutMs
        && this.bridgeMode == that.bridgeMode;
  }

  @Override
  public int hashCode() {
    return Objects.hash(enabled, apiUrl, appId, appSecret, timeoutMs, bridgeMode);
  }

  @Override
  public String toString() {
    return "UniAuthConfig["
        + "enabled="
        + enabled
        + ", "
        + "apiUrl="
        + apiUrl
        + ", "
        + "appId="
        + appId
        + ", "
        + "appSecret=***"
        + ", "
        + "timeoutMs="
        + timeoutMs
        + ", "
        + "bridgeMode="
        + bridgeMode
        + ']';
  }
}
