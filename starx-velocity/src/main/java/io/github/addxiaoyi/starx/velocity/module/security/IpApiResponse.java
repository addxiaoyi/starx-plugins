package io.github.addxiaoyi.starx.velocity.module.security;

/** ip-api.com JSON 响应模型。 */
final class IpApiResponse {
  String status;
  String country;
  String countryCode;
  String region;
  String city;
  String isp;
  String org;
  String as;
  boolean proxy;
  boolean hosting;
  boolean mobile;
  String query;
}
