package io.github.addxiaoyi.starx.api.event;

/** 跨模块事件类型常量。命名与网站后端 event-bus/events.js 保持一致。 */
public final class EventTypes {

  private EventTypes() {}

  // 认证相关
  public static final String PLAYER_LOGIN_START = "player:login:start";
  public static final String PLAYER_LOGIN_SUCCESS = "player:login:success";
  public static final String PLAYER_LOGIN_FAILED = "player:login:failed";
  public static final String PLAYER_LOGOUT = "player:logout";
  public static final String PLAYER_REGISTER = "player:register";
  public static final String PLAYER_TOTP_ENABLED = "player:totp:enabled";
  public static final String PLAYER_TOTP_DISABLED = "player:totp:disabled";

  // 安全相关
  public static final String SECURITY_ALERT = "security:alert";
  public static final String BOT_CHECK_FAILED = "security:bot:failed";
  public static final String PLAYER_BRUTE_FORCE = "player:brute-force";

  // 皮肤相关
  public static final String SKIN_REFRESH_REQUEST = "skin:refresh:request";
  public static final String SKIN_APPLIED = "skin:applied";
  public static final String SKIN_UPDATED = "skin:updated";

  // 网站联动
  public static final String LINK_EXTERNAL_USER = "link:external-user";
  public static final String UNLINK_EXTERNAL_USER = "unlink:external-user";
  public static final String ADMIN_KICK_PLAYER = "admin:kick:player";
  public static final String ADMIN_BAN_PLAYER = "admin:ban:player";
  public static final String ADMIN_RESET_PASSWORD = "admin:reset:password";
  public static final String ADMIN_BIND_EMAIL = "admin:bind:email";

  // 代理/后端通信
  public static final String SYNC_PLAYER_STATE = "sync:player:state";
  public static final String SYNC_CONFIG = "sync:config";

  // 统计报告
  public static final String PLAN_STATS_REPORT = "plan:stats:report";
}
