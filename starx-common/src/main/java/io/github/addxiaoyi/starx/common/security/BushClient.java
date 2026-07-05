package io.github.addxiaoyi.starx.common.security;

import java.util.logging.Logger;

/** 远程 IP 黑名单查询客户端，通过 Blossom API 检查 IP 是否在黑名单中。 */
public final class BushClient {

  private static final Logger logger = Logger.getLogger(BushClient.class.getName());
  private static final String BLOSSOM_URL = "https://blossom.pvphub.co/prd/";

  private final String baseUrl;

  public BushClient() {
    this(BLOSSOM_URL);
  }

  public BushClient(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  /** 查询指定 IP 是否在黑名单中。 */
  public boolean isIpBlacklisted(String ip) {
    IpInfo result =
        HttpClient.post(baseUrl + "ip").bodyJson(new IpRequest(ip)).sendJson(IpInfo.class);
    return result != null;
  }

  static final class IpRequest {
    final String ip;

    IpRequest(String ip) {
      this.ip = ip;
    }
  }

  static final class IpInfo {
    String ip;
    String reason;
  }
}
