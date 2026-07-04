package io.github.addxiaoyi.starx.velocity.module.security;

/** 安全模块事件类型常量。 */
public final class SecurityEvents {

  private SecurityEvents() {}

  /** 通用安全告警。 */
  public static final String SECURITY_ALERT = "security:alert";

  /** 检测到机器人连接。 */
  public static final String BOT_DETECTED = "security:bot:detected";

  /** 检测到崩溃攻击尝试。 */
  public static final String CRASH_ATTEMPT = "security:crash:attempt";

  /** 高风险连接评分。 */
  public static final String RISK_HIGH = "security:risk:high";

  /** 需要额外验证（如 TOTP）。 */
  public static final String RISK_VERIFY_REQUIRED = "security:risk:verify:required";

  /** 连接速率超限。 */
  public static final String RATE_LIMIT_EXCEEDED = "security:rate:limit:exceeded";

  /** 可疑数据包被拦截。 */
  public static final String SUSPICIOUS_PACKET = "security:packet:suspicious";

  /** 反作弊检测事件。 */
  public static final String ANTICHEAT_DETECTION = "security:anticheat:detection";
}