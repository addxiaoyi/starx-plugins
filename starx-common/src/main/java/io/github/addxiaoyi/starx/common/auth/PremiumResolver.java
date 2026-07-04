package io.github.addxiaoyi.starx.common.auth;

import java.util.UUID;

/** 判断玩家是否为正版/Yggdrasil 账户。 */
public final class PremiumResolver {

  /**
   * 判断玩家是否为正版/Yggdrasil 账户。
   *
   * @param uuid 玩家 UUID
   * @param onlineMode 代理是否运行于在线模式（Yggdrasil 视为在线模式）
   * @return 是否正版
   */
  public boolean isPremium(UUID uuid, boolean onlineMode) {
    return onlineMode && uuid.version() == 4;
  }
}
