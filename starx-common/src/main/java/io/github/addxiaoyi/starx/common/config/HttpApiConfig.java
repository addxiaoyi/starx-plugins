package io.github.addxiaoyi.starx.common.config;

/**
 * HTTP API 监听配置。
 *
 * @param bind HTTP 绑定地址
 * @param port 监听端口
 * @param apiKey 请求校验密钥
 */
public record HttpApiConfig(String bind, int port, String apiKey) {

  public HttpApiConfig {
    bind = bind == null || bind.isBlank() ? "0.0.0.0" : bind;
    port = port <= 0 ? 8080 : port;
    apiKey = apiKey == null ? "" : apiKey;
  }

  public static HttpApiConfig defaults() {
    return new HttpApiConfig("0.0.0.0", 8080, "");
  }
}
