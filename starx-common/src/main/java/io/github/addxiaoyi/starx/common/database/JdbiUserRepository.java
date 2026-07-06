package io.github.addxiaoyi.starx.common.database;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.github.addxiaoyi.starx.api.dto.UserDto;
import io.github.addxiaoyi.starx.api.repository.UserRepository;
import io.github.addxiaoyi.starx.common.model.StarxUser;
import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

/** 基于 JDBI 的 {@link UserRepository} 实现，内部使用 {@link StarxUser} 实体持久化。 */
public class JdbiUserRepository implements UserRepository {

  private static final String SELECT_COLUMNS =
      "uuid, username, email, password_hash, totp_secret, premium, created_at, last_login_at,"
          + " external_user_id, trusted_devices, COALESCE(recovery_codes, '') as recovery_codes,"
          + " source_system, migration_state, password_migrated_at";
  private static final String SELECT_BY_UUID =
      "SELECT " + SELECT_COLUMNS + " FROM starx_users WHERE uuid = ?";
  private static final String SELECT_BY_USERNAME =
      "SELECT " + SELECT_COLUMNS + " FROM starx_users WHERE username = ?";
  private static final String SELECT_BY_EMAIL =
      "SELECT " + SELECT_COLUMNS + " FROM starx_users WHERE email = ?";
  private static final String SELECT_ALL = "SELECT " + SELECT_COLUMNS + " FROM starx_users";
  private static final String DELETE_BY_UUID = "DELETE FROM starx_users WHERE uuid = ?";
  private static final Type TRUSTED_DEVICES_TYPE = new TypeToken<List<String>>() {}.getType();

  private final Jdbi jdbi;
  private final Gson gson = new Gson();

  public JdbiUserRepository(Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  @Override
  public Optional<UserDto> findByUuid(UUID uuid) {
    return findFullByUuid(uuid).map(this::toDto);
  }

  @Override
  public Optional<UserDto> findByUsername(String username) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(SELECT_BY_USERNAME)
                .bind(0, username)
                .map((rs, ctx) -> toDto(mapUser(rs)))
                .findOne());
  }

  @Override
  public Optional<UserDto> findByEmail(String email) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(SELECT_BY_EMAIL)
                .bind(0, email)
                .map((rs, ctx) -> toDto(mapUser(rs)))
                .findOne());
  }

  @Override
  public boolean existsByUsername(String username) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery("SELECT 1 FROM starx_users WHERE username = ?")
                .bind(0, username)
                .map((rs, ctx) -> 1)
                .findOne()
                .isPresent());
  }

  public List<UserDto> findAll() {
    return jdbi.withHandle(
        handle -> handle.createQuery(SELECT_ALL).map((rs, ctx) -> toDto(mapUser(rs))).list());
  }

  @Override
  public void save(UserDto user) {
    jdbi.useTransaction(
        handle -> {
          Optional<StarxUser> existing = findFullByUuid(handle, user.uuid());
          if (existing.isPresent()) {
            updateFromDto(
                handle,
                user,
                existing.get().passwordHash(),
                existing.get().totpSecret(),
                existing.get().trustedDevices());
          } else {
            insertFromDto(handle, user);
          }
        });
  }

  /**
   * 保存完整的用户实体，包含密码哈希与 TOTP 密钥。
   *
   * @param user 完整用户实体
   */
  public void saveUser(StarxUser user) {
    jdbi.useTransaction(
        handle -> {
          Optional<StarxUser> existing = findFullByUuid(handle, user.uuid());
          if (existing.isPresent()) {
            update(handle, user);
          } else {
            insert(handle, user);
          }
        });
  }

  @Override
  public void delete(UUID uuid) {
    jdbi.useHandle(handle -> handle.createUpdate(DELETE_BY_UUID).bind(0, uuid).execute());
  }

  public Optional<StarxUser> findFullByUuid(UUID uuid) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(SELECT_BY_UUID)
                .bind(0, uuid.toString())
                .map((rs, ctx) -> mapUser(rs))
                .findOne());
  }

  public Optional<StarxUser> findFullByUsername(String username) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(SELECT_BY_USERNAME)
                .bind(0, username)
                .map((rs, ctx) -> mapUser(rs))
                .findOne());
  }

  public boolean existsByUuid(UUID uuid) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery("SELECT 1 FROM starx_users WHERE uuid = ?")
                .bind(0, uuid)
                .map((rs, ctx) -> 1)
                .findOne()
                .isPresent());
  }

  public boolean existsByUsernameOrUuid(String username, UUID uuid) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery("SELECT 1 FROM starx_users WHERE username = ? OR uuid = ?")
                .bind(0, username)
                .bind(1, uuid)
                .map((rs, ctx) -> 1)
                .findOne()
                .isPresent());
  }

  public Optional<String> findTotpSecretByUuid(UUID uuid) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery("SELECT totp_secret FROM starx_users WHERE uuid = ?")
                .bind(0, uuid)
                .map((rs, ctx) -> rs.getString("totp_secret"))
                .findOne());
  }

  public Optional<String> findTrustedDevicesByUuid(UUID uuid) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery("SELECT trusted_devices FROM starx_users WHERE uuid = ?")
                .bind(0, uuid)
                .map((rs, ctx) -> rs.getString("trusted_devices"))
                .findOne());
  }

  public Optional<String> findPasswordHashByUuid(UUID uuid) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery("SELECT password_hash FROM starx_users WHERE uuid = ?")
                .bind(0, uuid)
                .map((rs, ctx) -> rs.getString("password_hash"))
                .findOne());
  }

  public void updatePasswordHash(UUID uuid, String passwordHash) {
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate("UPDATE starx_users SET password_hash = ? WHERE uuid = ?")
                .bind(0, passwordHash)
                .bind(1, uuid)
                .execute());
  }

  public void updatePassword(UUID uuid, String passwordHash) {
    updatePasswordHash(uuid, passwordHash);
  }

  public void create(StarxUser user) {
    jdbi.useHandle(handle -> insert(handle, user));
  }

  public void updateTotpSecret(UUID uuid, String totpSecret) {
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate("UPDATE starx_users SET totp_secret = ? WHERE uuid = ?")
                .bind(0, totpSecret)
                .bind(1, uuid)
                .execute());
  }

  public void updateEmail(UUID uuid, String email) {
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate("UPDATE starx_users SET email = ? WHERE uuid = ?")
                .bind(0, email)
                .bind(1, uuid)
                .execute());
  }

  public void updateLastLogin(UUID uuid, java.time.Instant lastLogin) {
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate("UPDATE starx_users SET last_login_at = ? WHERE uuid = ?")
                .bind(0, java.sql.Timestamp.from(lastLogin))
                .bind(1, uuid)
                .execute());
  }

  public void updatePremium(UUID uuid, boolean premium) {
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate("UPDATE starx_users SET premium = ? WHERE uuid = ?")
                .bind(0, premium)
                .bind(1, uuid)
                .execute());
  }

  public void updateTrustedDevices(UUID uuid, List<String> trustedDevices) {
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate("UPDATE starx_users SET trusted_devices = ? WHERE uuid = ?")
                .bind(0, toJson(trustedDevices))
                .bind(1, uuid)
                .execute());
  }

  public void updateRecoveryCodes(UUID uuid, String recoveryCodes) {
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate("UPDATE starx_users SET recovery_codes = ? WHERE uuid = ?")
                .bind(0, recoveryCodes)
                .bind(1, uuid)
                .execute());
  }

  public void updateMigrationState(UUID uuid, String migrationState) {
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate("UPDATE starx_users SET migration_state = ? WHERE uuid = ?")
                .bind(0, migrationState)
                .bind(1, uuid)
                .execute());
  }

  public void updatePasswordMigratedAt(UUID uuid, Instant passwordMigratedAt) {
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate("UPDATE starx_users SET password_migrated_at = ? WHERE uuid = ?")
                .bind(0, passwordMigratedAt != null ? Timestamp.from(passwordMigratedAt) : null)
                .bind(1, uuid)
                .execute());
  }

  public void updateSourceSystem(UUID uuid, String sourceSystem) {
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate("UPDATE starx_users SET source_system = ? WHERE uuid = ?")
                .bind(0, sourceSystem)
                .bind(1, uuid)
                .execute());
  }

  public void markPasswordMigrated(UUID uuid, String passwordHash, Instant migratedAt) {
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate(
                    "UPDATE starx_users SET password_hash = ?, password_migrated_at = ?, migration_state = ? WHERE uuid = ?")
                .bind(0, passwordHash)
                .bind(1, Timestamp.from(migratedAt))
                .bind(2, "completed")
                .bind(3, uuid)
                .execute());
  }

  /**
   * 统计指定迁移状态的用户数量。
   *
   * @param migrationState 迁移状态
   * @return 用户数量
   */
  public int countByMigrationState(String migrationState) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery("SELECT COUNT(*) FROM starx_users WHERE migration_state = ?")
                .bind(0, migrationState)
                .mapTo(Integer.class)
                .one());
  }

  /**
   * 统计指定来源系统的用户数量。
   *
   * @param sourceSystem 来源系统
   * @return 用户数量
   */
  public int countBySourceSystem(String sourceSystem) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery("SELECT COUNT(*) FROM starx_users WHERE source_system = ?")
                .bind(0, sourceSystem)
                .mapTo(Integer.class)
                .one());
  }

  /**
   * 统计指定来源系统和迁移状态的用户数量。
   *
   * @param sourceSystem 来源系统
   * @param migrationState 迁移状态
   * @return 用户数量
   */
  public int countBySourceSystemAndMigrationState(String sourceSystem, String migrationState) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery("SELECT COUNT(*) FROM starx_users WHERE source_system = ? AND migration_state = ?")
                .bind(0, sourceSystem)
                .bind(1, migrationState)
                .mapTo(Integer.class)
                .one());
  }

  /**
   * 获取所有用户数量。
   *
   * @return 用户总数
   */
  public int countAll() {
    return jdbi.withHandle(
        handle ->
            handle.createQuery("SELECT COUNT(*) FROM starx_users").mapTo(Integer.class).one());
  }

  /**
   * 查询指定来源系统的所有用户。
   *
   * @param sourceSystem 来源系统
   * @return 用户列表
   */
  public List<StarxUser> findBySourceSystem(String sourceSystem) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(
                    "SELECT " + SELECT_COLUMNS + " FROM starx_users WHERE source_system = ?")
                .bind(0, sourceSystem)
                .map((rs, ctx) -> mapUser(rs))
                .list());
  }

  /**
   * 查询指定迁移状态的所有用户。
   *
   * @param migrationState 迁移状态
   * @return 用户列表
   */
  public List<StarxUser> findByMigrationState(String migrationState) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(
                    "SELECT " + SELECT_COLUMNS + " FROM starx_users WHERE migration_state = ?")
                .bind(0, migrationState)
                .map((rs, ctx) -> mapUser(rs))
                .list());
  }

  private Optional<StarxUser> findFullByUuid(Handle handle, UUID uuid) {
    return handle.createQuery(SELECT_BY_UUID).bind(0, uuid).map((rs, ctx) -> mapUser(rs)).findOne();
  }

  private void insertFromDto(Handle handle, UserDto user) {
    Instant now = user.createdAt() != null ? user.createdAt() : Instant.now();
    handle
        .createUpdate(
            "INSERT INTO starx_users (uuid, username, email, password_hash, totp_secret, premium, created_at, last_login_at, external_user_id, trusted_devices) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
        .bind(0, user.uuid())
        .bind(1, user.username())
        .bind(2, user.email())
        .bind(3, (Object) null)
        .bind(4, (Object) null)
        .bind(5, user.premium())
        .bind(6, Timestamp.from(now))
        .bind(7, user.lastLoginAt() != null ? Timestamp.from(user.lastLoginAt()) : null)
        .bind(8, user.externalUserId())
        .bind(9, (Object) null)
        .execute();
  }

  private void updateFromDto(
      Handle handle,
      UserDto user,
      String existingPasswordHash,
      String existingTotpSecret,
      List<String> existingTrustedDevices) {
    handle
        .createUpdate(
            "UPDATE starx_users SET username = ?, email = ?, password_hash = ?, totp_secret = ?, premium = ?, created_at = ?, last_login_at = ?, external_user_id = ?, trusted_devices = ? WHERE uuid = ?")
        .bind(0, user.username())
        .bind(1, user.email())
        .bind(2, existingPasswordHash)
        .bind(3, existingTotpSecret)
        .bind(4, user.premium())
        .bind(
            5,
            user.createdAt() != null
                ? Timestamp.from(user.createdAt())
                : Timestamp.from(Instant.now()))
        .bind(6, user.lastLoginAt() != null ? Timestamp.from(user.lastLoginAt()) : null)
        .bind(7, user.externalUserId())
        .bind(8, toJson(existingTrustedDevices))
        .bind(9, user.uuid())
        .execute();
  }

  private void insert(Handle handle, StarxUser user) {
    handle
        .createUpdate(
            "INSERT INTO starx_users (uuid, username, email, password_hash, totp_secret, premium, created_at, last_login_at, external_user_id, trusted_devices, recovery_codes, source_system, migration_state, password_migrated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
        .bind(0, user.uuid())
        .bind(1, user.username())
        .bind(2, user.email())
        .bind(3, user.passwordHash())
        .bind(4, user.totpSecret())
        .bind(5, user.premium())
        .bind(6, Timestamp.from(user.createdAt()))
        .bind(7, user.lastLoginAt() != null ? Timestamp.from(user.lastLoginAt()) : null)
        .bind(8, user.externalUserId())
        .bind(9, toJson(user.trustedDevices()))
        .bind(10, user.recoveryCodes())
        .bind(11, user.sourceSystem())
        .bind(12, user.migrationState())
        .bind(
            13,
            user.passwordMigratedAt() != null ? Timestamp.from(user.passwordMigratedAt()) : null)
        .execute();
  }

  private void update(Handle handle, StarxUser user) {
    handle
        .createUpdate(
            "UPDATE starx_users SET username = ?, email = ?, password_hash = ?, totp_secret = ?, premium = ?, created_at = ?, last_login_at = ?, external_user_id = ?, trusted_devices = ?, recovery_codes = ?, source_system = ?, migration_state = ?, password_migrated_at = ? WHERE uuid = ?")
        .bind(0, user.username())
        .bind(1, user.email())
        .bind(2, user.passwordHash())
        .bind(3, user.totpSecret())
        .bind(4, user.premium())
        .bind(5, Timestamp.from(user.createdAt()))
        .bind(6, user.lastLoginAt() != null ? Timestamp.from(user.lastLoginAt()) : null)
        .bind(7, user.externalUserId())
        .bind(8, toJson(user.trustedDevices()))
        .bind(9, user.recoveryCodes())
        .bind(10, user.sourceSystem())
        .bind(11, user.migrationState())
        .bind(
            12,
            user.passwordMigratedAt() != null ? Timestamp.from(user.passwordMigratedAt()) : null)
        .bind(13, user.uuid())
        .execute();
  }

  private UserDto toDto(StarxUser user) {
    return UserDto.builder()
        .uuid(user.uuid())
        .username(user.username())
        .email(user.email())
        .premium(user.premium())
        .createdAt(user.createdAt())
        .lastLoginAt(user.lastLoginAt())
        .externalUserId(user.externalUserId())
        .build();
  }

  private StarxUser mapUser(ResultSet rs) throws SQLException {
    Timestamp createdAt = rs.getTimestamp("created_at");
    Timestamp lastLoginAt = rs.getTimestamp("last_login_at");
    Timestamp passwordMigratedAt = rs.getTimestamp("password_migrated_at");
    return new StarxUser(
        UUID.fromString(rs.getString("uuid")),
        rs.getString("username"),
        rs.getString("email"),
        rs.getString("password_hash"),
        rs.getString("totp_secret"),
        rs.getBoolean("premium"),
        createdAt != null ? createdAt.toInstant() : null,
        lastLoginAt != null ? lastLoginAt.toInstant() : null,
        rs.getString("external_user_id"),
        parseTrustedDevices(rs.getString("trusted_devices")),
        rs.getString("recovery_codes"),
        rs.getString("source_system"),
        rs.getString("migration_state"),
        passwordMigratedAt != null ? passwordMigratedAt.toInstant() : null);
  }

  private String toJson(List<String> trustedDevices) {
    if (trustedDevices == null || trustedDevices.isEmpty()) {
      return null;
    }
    return gson.toJson(trustedDevices);
  }

  private List<String> parseTrustedDevices(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    List<String> parsed = gson.fromJson(json, TRUSTED_DEVICES_TYPE);
    return parsed == null ? List.of() : List.copyOf(parsed);
  }
}
