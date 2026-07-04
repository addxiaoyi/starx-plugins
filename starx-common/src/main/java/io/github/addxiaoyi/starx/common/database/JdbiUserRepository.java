package io.github.addxiaoyi.starx.common.database;

import io.github.addxiaoyi.starx.api.dto.UserDto;
import io.github.addxiaoyi.starx.api.repository.UserRepository;
import io.github.addxiaoyi.starx.common.model.StarxUser;
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

  private static final String SELECT_BY_UUID =
      "SELECT uuid, username, email, password_hash, totp_secret, premium, created_at, last_login_at, external_user_id FROM starx_users WHERE uuid = ?";
  private static final String SELECT_BY_USERNAME =
      "SELECT uuid, username, email, password_hash, totp_secret, premium, created_at, last_login_at, external_user_id FROM starx_users WHERE username = ?";
  private static final String SELECT_BY_EMAIL =
      "SELECT uuid, username, email, password_hash, totp_secret, premium, created_at, last_login_at, external_user_id FROM starx_users WHERE email = ?";
  private static final String SELECT_ALL =
      "SELECT uuid, username, email, password_hash, totp_secret, premium, created_at, last_login_at, external_user_id FROM starx_users";
  private static final String DELETE_BY_UUID = "DELETE FROM starx_users WHERE uuid = ?";

  private final Jdbi jdbi;

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
            updateFromDto(handle, user, existing.get().passwordHash(), existing.get().totpSecret());
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
    return jdbi.withHandle(handle -> findFullByUuid(handle, uuid));
  }

  private Optional<StarxUser> findFullByUuid(Handle handle, UUID uuid) {
    return handle.createQuery(SELECT_BY_UUID).bind(0, uuid).map((rs, ctx) -> mapUser(rs)).findOne();
  }

  private void insertFromDto(Handle handle, UserDto user) {
    Instant now = user.createdAt() != null ? user.createdAt() : Instant.now();
    handle
        .createUpdate(
            "INSERT INTO starx_users (uuid, username, email, password_hash, totp_secret, premium, created_at, last_login_at, external_user_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")
        .bind(0, user.uuid())
        .bind(1, user.username())
        .bind(2, user.email())
        .bind(3, (Object) null)
        .bind(4, (Object) null)
        .bind(5, user.premium())
        .bind(6, Timestamp.from(now))
        .bind(7, user.lastLoginAt() != null ? Timestamp.from(user.lastLoginAt()) : null)
        .bind(8, user.externalUserId())
        .execute();
  }

  private void updateFromDto(
      Handle handle, UserDto user, String existingPasswordHash, String existingTotpSecret) {
    handle
        .createUpdate(
            "UPDATE starx_users SET username = ?, email = ?, password_hash = ?, totp_secret = ?, premium = ?, created_at = ?, last_login_at = ?, external_user_id = ? WHERE uuid = ?")
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
        .bind(8, user.uuid())
        .execute();
  }

  private void insert(Handle handle, StarxUser user) {
    handle
        .createUpdate(
            "INSERT INTO starx_users (uuid, username, email, password_hash, totp_secret, premium, created_at, last_login_at, external_user_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")
        .bind(0, user.uuid())
        .bind(1, user.username())
        .bind(2, user.email())
        .bind(3, user.passwordHash())
        .bind(4, user.totpSecret())
        .bind(5, user.premium())
        .bind(6, Timestamp.from(user.createdAt()))
        .bind(7, user.lastLoginAt() != null ? Timestamp.from(user.lastLoginAt()) : null)
        .bind(8, user.externalUserId())
        .execute();
  }

  private void update(Handle handle, StarxUser user) {
    handle
        .createUpdate(
            "UPDATE starx_users SET username = ?, email = ?, password_hash = ?, totp_secret = ?, premium = ?, created_at = ?, last_login_at = ?, external_user_id = ? WHERE uuid = ?")
        .bind(0, user.username())
        .bind(1, user.email())
        .bind(2, user.passwordHash())
        .bind(3, user.totpSecret())
        .bind(4, user.premium())
        .bind(5, Timestamp.from(user.createdAt()))
        .bind(6, user.lastLoginAt() != null ? Timestamp.from(user.lastLoginAt()) : null)
        .bind(7, user.externalUserId())
        .bind(8, user.uuid())
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

  private static StarxUser mapUser(ResultSet rs) throws SQLException {
    Timestamp createdAt = rs.getTimestamp("created_at");
    Timestamp lastLoginAt = rs.getTimestamp("last_login_at");
    return new StarxUser(
        UUID.fromString(rs.getString("uuid")),
        rs.getString("username"),
        rs.getString("email"),
        rs.getString("password_hash"),
        rs.getString("totp_secret"),
        rs.getBoolean("premium"),
        createdAt != null ? createdAt.toInstant() : null,
        lastLoginAt != null ? lastLoginAt.toInstant() : null,
        rs.getString("external_user_id"));
  }
}
