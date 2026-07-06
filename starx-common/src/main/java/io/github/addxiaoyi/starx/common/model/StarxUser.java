package io.github.addxiaoyi.starx.common.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** 用户数据库实体。 */
public final class StarxUser {

  private final UUID uuid;
  private final String username;
  private final String email;
  private final String passwordHash;
  private final String totpSecret;
  private final boolean premium;
  private final Instant createdAt;
  private final Instant lastLoginAt;
  private final String externalUserId;
  private final List<String> trustedDevices;
  private final String recoveryCodes;
  private final String sourceSystem;
  private final String migrationState;
  private final Instant passwordMigratedAt;
  private final String lastLoginIp;
  private final String lastLoginIsp;
  private final String lastLoginLocation;
  private final Long totalPlaytime;
  private final Instant lastLogoutAt;
  private final Boolean welcomeMessageShown;

  public StarxUser(
      UUID uuid,
      String username,
      String email,
      String passwordHash,
      String totpSecret,
      boolean premium,
      Instant createdAt,
      Instant lastLoginAt,
      String externalUserId,
      List<String> trustedDevices,
      String recoveryCodes,
      String sourceSystem,
      String migrationState,
      Instant passwordMigratedAt,
      String lastLoginIp,
      String lastLoginIsp,
      String lastLoginLocation,
      Long totalPlaytime,
      Instant lastLogoutAt,
      Boolean welcomeMessageShown) {
    this.uuid = uuid;
    this.username = username;
    this.email = email;
    this.passwordHash = passwordHash;
    this.totpSecret = totpSecret;
    this.premium = premium;
    this.createdAt = createdAt;
    this.lastLoginAt = lastLoginAt;
    this.externalUserId = externalUserId;
    this.trustedDevices =
        trustedDevices == null
            ? List.of()
            : Collections.unmodifiableList(new ArrayList<>(trustedDevices));
    this.recoveryCodes = recoveryCodes;
    this.sourceSystem = sourceSystem;
    this.migrationState = migrationState;
    this.passwordMigratedAt = passwordMigratedAt;
    this.lastLoginIp = lastLoginIp;
    this.lastLoginIsp = lastLoginIsp;
    this.lastLoginLocation = lastLoginLocation;
    this.totalPlaytime = totalPlaytime;
    this.lastLogoutAt = lastLogoutAt;
    this.welcomeMessageShown = welcomeMessageShown;
  }

  public UUID uuid() {
    return uuid;
  }

  public String username() {
    return username;
  }

  public String email() {
    return email;
  }

  public String passwordHash() {
    return passwordHash;
  }

  public String totpSecret() {
    return totpSecret;
  }

  public boolean premium() {
    return premium;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant lastLoginAt() {
    return lastLoginAt;
  }

  public String externalUserId() {
    return externalUserId;
  }

  public List<String> trustedDevices() {
    return trustedDevices;
  }

  public String recoveryCodes() {
    return recoveryCodes;
  }

  public String sourceSystem() {
    return sourceSystem;
  }

  public String migrationState() {
    return migrationState;
  }

  public Instant passwordMigratedAt() {
    return passwordMigratedAt;
  }

  public String lastLoginIp() {
    return lastLoginIp;
  }

  public String lastLoginIsp() {
    return lastLoginIsp;
  }

  public String lastLoginLocation() {
    return lastLoginLocation;
  }

  public Long totalPlaytime() {
    return totalPlaytime;
  }

  public Instant lastLogoutAt() {
    return lastLogoutAt;
  }

  public Boolean welcomeMessageShown() {
    return welcomeMessageShown;
  }
}
