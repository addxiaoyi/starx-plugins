package io.github.addxiaoyi.starx.common.auth;

import java.security.SecureRandom;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class BindingVerificationService {

  private static final SecureRandom RANDOM = new SecureRandom();
  private static final long CODE_TTL_MS = 300_000;

  private final ConcurrentMap<String, PendingCode> pendingCodes = new ConcurrentHashMap<>();

  public String generateCode(UUID playerUuid) {
    String code = String.format("%06d", RANDOM.nextInt(1_000_000));
    pendingCodes.put(code, new PendingCode(playerUuid, System.currentTimeMillis() + CODE_TTL_MS));
    return code;
  }

  public UUID verifyCode(String code) {
    PendingCode pc = pendingCodes.remove(code);
    if (pc == null || System.currentTimeMillis() > pc.expiresAt) {
      return null;
    }
    return pc.playerUuid;
  }

  private record PendingCode(UUID playerUuid, long expiresAt) {}
}
