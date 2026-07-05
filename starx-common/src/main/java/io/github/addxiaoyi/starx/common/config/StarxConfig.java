package io.github.addxiaoyi.starx.common.config;

import io.github.addxiaoyi.starx.common.auth.uniauth.UniAuthConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * starx-common 顶层配置。支持默认值与配置合并。
 *
 * @param httpApi HTTP API 配置
 * @param database 数据库配置
 * @param uniauth UniAuth 配置
 * @param modules 模块开关映射
 */
public record StarxConfig(
    HttpApiConfig httpApi,
    DatabaseConfig database,
    UniAuthConfig uniauth,
    Map<String, ModuleConfig> modules) {

  public StarxConfig {
    httpApi = httpApi == null ? HttpApiConfig.defaults() : httpApi;
    database = database == null ? DatabaseConfig.defaults() : database;
    uniauth = uniauth == null ? UniAuthConfig.defaults() : uniauth;
    modules = modules == null ? Map.of() : Map.copyOf(modules);
  }

  public static StarxConfig defaults() {
    return new StarxConfig(
        HttpApiConfig.defaults(), DatabaseConfig.defaults(), UniAuthConfig.defaults(), Map.of());
  }

  /**
   * 将另一份配置作为覆盖层合并到当前配置。覆盖层中的非默认值会替换当前值，模块映射以覆盖层为准。
   *
   * @param overlay 覆盖层配置
   * @return 合并后的新配置
   */
  public StarxConfig merge(StarxConfig overlay) {
    Objects.requireNonNull(overlay, "overlay");
    HttpApiConfig mergedHttpApi =
        HttpApiConfig.defaults().equals(overlay.httpApi) ? this.httpApi : overlay.httpApi;
    DatabaseConfig mergedDatabase =
        DatabaseConfig.defaults().equals(overlay.database) ? this.database : overlay.database;
    UniAuthConfig mergedUniAuth =
        UniAuthConfig.defaults().equals(overlay.uniauth) ? this.uniauth : overlay.uniauth;
    Map<String, ModuleConfig> mergedModules = new HashMap<>(this.modules);
    mergedModules.putAll(overlay.modules);
    return new StarxConfig(mergedHttpApi, mergedDatabase, mergedUniAuth, mergedModules);
  }

  public boolean isModuleEnabled(String name) {
    return modules.getOrDefault(name, new ModuleConfig(false)).enabled();
  }
}
