package io.github.addxiaoyi.starx.common.database;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.github.addxiaoyi.starx.api.dto.UserDto;
import io.github.addxiaoyi.starx.api.repository.UserRepository;
import io.github.addxiaoyi.starx.common.model.StarxUser;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;

public class JdbcUserRepository implements UserRepository {

  private static final String SELECT_COLUMNS =
      "uuid, username, email, password_hash, totp_secret, premium, created_at, last_login_at,"
          + " external_user_id, trusted_devices, COALESCE(recovery_codes, '') as recovery_codes,"
          + " source_system, migration_state, password_migrated_at, last_login_ip, last_login_isp,"
          + " last_login_location, total_playtime, last_logout_at, welcome_message_shown";
  private static final String SELECT_BY_UUID =
      "SELECT " + SELECT_COLUMNS + " FROM starx_users WHERE uuid = ?";
  private static final String SELECT_BY_USERNAME =
      "SELECT " + SELECT_COLUMNS + " FROM starx_users WHERE username = ?";
  private static final String SELECT_BY_EMAIL =
      "SELECT " + SELECT_COLUMNS + " FROM starx_users WHERE email = ?";
  private static final String SELECT_ALL = "SELECT " + SELECT_COLUMNS + " FROM starx_users";
  private static final String DELETE_BY_UUID = "DELETE FROM starx_users WHERE uuid = ?";
  private static final Type TRUSTED_DEVICES_TYPE = new TypeToken<List<String>>() {}.getType();

  private final DataSource dataSource;
  private final Gson gson = new Gson();

  public JdbcUserRepository(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public Optional<UserDto> findByUuid(UUID uuid) {
    return findFullByUuid(uuid).map(this::toDto);
  }

  @Override
  public Optional<UserDto> findByUsername(String username) {
    return queryOne(
        SELECT_BY_USERNAME, stmt -> stmt.setString(1, username), rs -> toDto(mapUser(rs)));
  }

  @Override
  public Optional<UserDto> findByEmail(String email) {
    return queryOne(SELECT_BY_EMAIL, stmt -> stmt.setString(1, email), rs -> toDto(mapUser(rs)));
  }

  @Override
  public boolean existsByUsername(String username) {
    return queryOne(
            "SELECT 1 FROM starx_users WHERE username = ?",
            stmt -> stmt.setString(1, username),
            rs -> 1)
        .isPresent();
  }

  public List<UserDto> findAll() {
    return queryList(SELECT_ALL, stmt -> {}, rs -> toDto(mapUser(rs)));
  }

  @Override
  public void save(UserDto user) {
    withTransaction(
        conn -> {
          Optional<StarxUser> existing = findFullByUuid(conn, user.uuid());
          if (existing.isPresent()) {
            updateFromDto(
                conn,
                user,
                existing.get().passwordHash(),
                existing.get().totpSecret(),
                existing.get().trustedDevices());
          } else {
            insertFromDto(conn, user);
          }
        });
  }

  public void saveUser(StarxUser user) {
    withTransaction(
        conn -> {
          Optional<StarxUser> existing = findFullByUuid(conn, user.uuid());
          if (existing.isPresent()) {
            update(conn, user);
          } else {
            insert(conn, user);
          }
        });
  }

  @Override
  public void delete(UUID uuid) {
    execute(DELETE_BY_UUID, stmt -> stmt.setObject(1, uuid));
  }

  public Optional<StarxUser> findFullByUuid(UUID uuid) {
    return queryOne(SELECT_BY_UUID, stmt -> stmt.setString(1, uuid.toString()), rs -> mapUser(rs));
  }

  public Optional<StarxUser> findFullByUsername(String username) {
    return queryOne(SELECT_BY_USERNAME, stmt -> stmt.setString(1, username), rs -> mapUser(rs));
  }

  public boolean existsByUuid(UUID uuid) {
    return queryOne(
            "SELECT 1 FROM starx_users WHERE uuid = ?", stmt -> stmt.setObject(1, uuid), rs -> 1)
        .isPresent();
  }

  public boolean existsByUsernameOrUuid(String username, UUID uuid) {
    return queryOne(
            "SELECT 1 FROM starx_users WHERE username = ? OR uuid = ?",
            stmt -> {
              stmt.setString(1, username);
              stmt.setObject(2, uuid);
            },
            rs -> 1)
        .isPresent();
  }

  public Optional<String> findTotpSecretByUuid(UUID uuid) {
    return queryOne(
        "SELECT totp_secret FROM starx_users WHERE uuid = ?",
        stmt -> stmt.setObject(1, uuid),
        rs -> rs.getString("totp_secret"));
  }

  public Optional<String> findTrustedDevicesByUuid(UUID uuid) {
    return queryOne(
        "SELECT trusted_devices FROM starx_users WHERE uuid = ?",
        stmt -> stmt.setObject(1, uuid),
        rs -> rs.getString("trusted_devices"));
  }

  public Optional<String> findPasswordHashByUuid(UUID uuid) {
    return queryOne(
        "SELECT password_hash FROM starx_users WHERE uuid = ?",
        stmt -> stmt.setObject(1, uuid),
        rs -> rs.getString("password_hash"));
  }

  public void updatePasswordHash(UUID uuid, String passwordHash) {
    execute(
        "UPDATE starx_users SET password_hash = ? WHERE uuid = ?",
        stmt -> {
          stmt.setString(1, passwordHash);
          stmt.setObject(2, uuid);
        });
  }

  public void updatePassword(UUID uuid, String passwordHash) {
    updatePasswordHash(uuid, passwordHash);
  }

  public void create(StarxUser user) {
    withTransaction(conn -> insert(conn, user));
  }

  public void updateTotpSecret(UUID uuid, String totpSecret) {
    execute(
        "UPDATE starx_users SET totp_secret = ? WHERE uuid = ?",
        stmt -> {
          stmt.setString(1, totpSecret);
          stmt.setObject(2, uuid);
        });
  }

  public void updateEmail(UUID uuid, String email) {
    execute(
        "UPDATE starx_users SET email = ? WHERE uuid = ?",
        stmt -> {
          stmt.setString(1, email);
          stmt.setObject(2, uuid);
        });
  }

  public void updateLastLogin(UUID uuid, Instant lastLogin) {
    execute(
        "UPDATE starx_users SET last_login_at = ? WHERE uuid = ?",
        stmt -> {
          stmt.setTimestamp(1, Timestamp.from(lastLogin));
          stmt.setObject(2, uuid);
        });
  }

  public void updatePremium(UUID uuid, boolean premium) {
    execute(
        "UPDATE starx_users SET premium = ? WHERE uuid = ?",
        stmt -> {
          stmt.setBoolean(1, premium);
          stmt.setObject(2, uuid);
        });
  }

  public void updateTrustedDevices(UUID uuid, List<String> trustedDevices) {
    execute(
        "UPDATE starx_users SET trusted_devices = ? WHERE uuid = ?",
        stmt -> {
          stmt.setString(1, toJson(trustedDevices));
          stmt.setObject(2, uuid);
        });
  }

  public void updateRecoveryCodes(UUID uuid, String recoveryCodes) {
    execute(
        "UPDATE starx_users SET recovery_codes = ? WHERE uuid = ?",
        stmt -> {
          stmt.setString(1, recoveryCodes);
          stmt.setObject(2, uuid);
        });
  }

  public void updateMigrationState(UUID uuid, String migrationState) {
    execute(
        "UPDATE starx_users SET migration_state = ? WHERE uuid = ?",
        stmt -> {
          stmt.setString(1, migrationState);
          stmt.setObject(2, uuid);
        });
  }

  public void updatePasswordMigratedAt(UUID uuid, Instant passwordMigratedAt) {
    execute(
        "UPDATE starx_users SET password_migrated_at = ? WHERE uuid = ?",
        stmt -> {
          stmt.setTimestamp(
              1, passwordMigratedAt != null ? Timestamp.from(passwordMigratedAt) : null);
          stmt.setObject(2, uuid);
        });
  }

  public void updateLoginInfo(UUID uuid, String ip, String isp, String location) {
    execute(
        "UPDATE starx_users SET last_login_ip = ?, last_login_isp = ?, last_login_location = ? WHERE uuid = ?",
        stmt -> {
          stmt.setString(1, ip);
          stmt.setString(2, isp);
          stmt.setString(3, location);
          stmt.setObject(4, uuid);
        });
  }

  public void updateTotalPlaytime(UUID uuid, long additionalPlaytime) {
    execute(
        "UPDATE starx_users SET total_playtime = COALESCE(total_playtime, 0) + ? WHERE uuid = ?",
        stmt -> {
          stmt.setLong(1, additionalPlaytime);
          stmt.setObject(2, uuid);
        });
  }

  public void updateLastLogout(UUID uuid, Instant lastLogoutAt) {
    execute(
        "UPDATE starx_users SET last_logout_at = ? WHERE uuid = ?",
        stmt -> {
          stmt.setTimestamp(1, lastLogoutAt != null ? Timestamp.from(lastLogoutAt) : null);
          stmt.setObject(2, uuid);
        });
  }

  public void markWelcomeMessageShown(UUID uuid) {
    execute(
        "UPDATE starx_users SET welcome_message_shown = TRUE WHERE uuid = ?",
        stmt -> stmt.setObject(1, uuid));
  }

  public void updateSourceSystem(UUID uuid, String sourceSystem) {
    execute(
        "UPDATE starx_users SET source_system = ? WHERE uuid = ?",
        stmt -> {
          stmt.setString(1, sourceSystem);
          stmt.setObject(2, uuid);
        });
  }

  public void markPasswordMigrated(UUID uuid, String passwordHash, Instant migratedAt) {
    execute(
        "UPDATE starx_users SET password_hash = ?, password_migrated_at = ?, migration_state = ? WHERE uuid = ?",
        stmt -> {
          stmt.setString(1, passwordHash);
          stmt.setTimestamp(2, Timestamp.from(migratedAt));
          stmt.setString(3, "completed");
          stmt.setObject(4, uuid);
        });
  }

  public int countByMigrationState(String migrationState) {
    return queryOne(
            "SELECT COUNT(*) FROM starx_users WHERE migration_state = ?",
            stmt -> stmt.setString(1, migrationState),
            rs -> rs.getInt(1))
        .orElse(0);
  }

  public int countBySourceSystem(String sourceSystem) {
    return queryOne(
            "SELECT COUNT(*) FROM starx_users WHERE source_system = ?",
            stmt -> stmt.setString(1, sourceSystem),
            rs -> rs.getInt(1))
        .orElse(0);
  }

  public int countBySourceSystemAndMigrationState(String sourceSystem, String migrationState) {
    return queryOne(
            "SELECT COUNT(*) FROM starx_users WHERE source_system = ? AND migration_state = ?",
            stmt -> {
              stmt.setString(1, sourceSystem);
              stmt.setString(2, migrationState);
            },
            rs -> rs.getInt(1))
        .orElse(0);
  }

  public int countAll() {
    return queryOne("SELECT COUNT(*) FROM starx_users", stmt -> {}, rs -> rs.getInt(1)).orElse(0);
  }

  public List<StarxUser> findBySourceSystem(String sourceSystem) {
    return queryList(
        "SELECT " + SELECT_COLUMNS + " FROM starx_users WHERE source_system = ?",
        stmt -> stmt.setString(1, sourceSystem),
        rs -> mapUser(rs));
  }

  public List<StarxUser> findByMigrationState(String migrationState) {
    return queryList(
        "SELECT " + SELECT_COLUMNS + " FROM starx_users WHERE migration_state = ?",
        stmt -> stmt.setString(1, migrationState),
        rs -> mapUser(rs));
  }

  private Optional<StarxUser> findFullByUuid(Connection conn, UUID uuid) throws SQLException {
    try (PreparedStatement ps = conn.prepareStatement(SELECT_BY_UUID)) {
      ps.setString(1, uuid.toString());
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? Optional.of(mapUser(rs)) : Optional.empty();
      }
    }
  }

  private void insertFromDto(Connection conn, UserDto user) throws SQLException {
    Instant now = user.createdAt() != null ? user.createdAt() : Instant.now();
    try (PreparedStatement ps =
        conn.prepareStatement(
            "INSERT INTO starx_users (uuid, username, email, password_hash, totp_secret, premium, created_at, last_login_at, external_user_id, trusted_devices) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
      ps.setObject(1, user.uuid());
      ps.setString(2, user.username());
      ps.setString(3, user.email());
      ps.setNull(4, java.sql.Types.VARCHAR);
      ps.setNull(5, java.sql.Types.VARCHAR);
      ps.setBoolean(6, user.premium());
      ps.setTimestamp(7, Timestamp.from(now));
      ps.setTimestamp(8, user.lastLoginAt() != null ? Timestamp.from(user.lastLoginAt()) : null);
      ps.setString(9, user.externalUserId());
      ps.setNull(10, java.sql.Types.VARCHAR);
      ps.executeUpdate();
    }
  }

  private void updateFromDto(
      Connection conn,
      UserDto user,
      String existingPasswordHash,
      String existingTotpSecret,
      List<String> existingTrustedDevices)
      throws SQLException {
    try (PreparedStatement ps =
        conn.prepareStatement(
            "UPDATE starx_users SET username = ?, email = ?, password_hash = ?, totp_secret = ?, premium = ?, created_at = ?, last_login_at = ?, external_user_id = ?, trusted_devices = ? WHERE uuid = ?")) {
      ps.setString(1, user.username());
      ps.setString(2, user.email());
      ps.setString(3, existingPasswordHash);
      ps.setString(4, existingTotpSecret);
      ps.setBoolean(5, user.premium());
      ps.setTimestamp(
          6,
          user.createdAt() != null
              ? Timestamp.from(user.createdAt())
              : Timestamp.from(Instant.now()));
      ps.setTimestamp(7, user.lastLoginAt() != null ? Timestamp.from(user.lastLoginAt()) : null);
      ps.setString(8, user.externalUserId());
      ps.setString(9, toJson(existingTrustedDevices));
      ps.setObject(10, user.uuid());
      ps.executeUpdate();
    }
  }

  private void insert(Connection conn, StarxUser user) throws SQLException {
    try (PreparedStatement ps =
        conn.prepareStatement(
            "INSERT INTO starx_users (uuid, username, email, password_hash, totp_secret, premium, created_at, last_login_at, external_user_id, trusted_devices, recovery_codes, source_system, migration_state, password_migrated_at, last_login_ip, last_login_isp, last_login_location, total_playtime, last_logout_at, welcome_message_shown) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
      ps.setObject(1, user.uuid());
      ps.setString(2, user.username());
      ps.setString(3, user.email());
      ps.setString(4, user.passwordHash());
      ps.setString(5, user.totpSecret());
      ps.setBoolean(6, user.premium());
      ps.setTimestamp(7, Timestamp.from(user.createdAt()));
      ps.setTimestamp(8, user.lastLoginAt() != null ? Timestamp.from(user.lastLoginAt()) : null);
      ps.setString(9, user.externalUserId());
      ps.setString(10, toJson(user.trustedDevices()));
      ps.setString(11, user.recoveryCodes());
      ps.setString(12, user.sourceSystem());
      ps.setString(13, user.migrationState());
      ps.setTimestamp(
          14, user.passwordMigratedAt() != null ? Timestamp.from(user.passwordMigratedAt()) : null);
      ps.setString(15, user.lastLoginIp());
      ps.setString(16, user.lastLoginIsp());
      ps.setString(17, user.lastLoginLocation());
      ps.setLong(18, user.totalPlaytime());
      ps.setTimestamp(19, user.lastLogoutAt() != null ? Timestamp.from(user.lastLogoutAt()) : null);
      ps.setBoolean(20, user.welcomeMessageShown());
      ps.executeUpdate();
    }
  }

  private void update(Connection conn, StarxUser user) throws SQLException {
    try (PreparedStatement ps =
        conn.prepareStatement(
            "UPDATE starx_users SET username = ?, email = ?, password_hash = ?, totp_secret = ?, premium = ?, created_at = ?, last_login_at = ?, external_user_id = ?, trusted_devices = ?, recovery_codes = ?, source_system = ?, migration_state = ?, password_migrated_at = ?, last_login_ip = ?, last_login_isp = ?, last_login_location = ?, total_playtime = ?, last_logout_at = ?, welcome_message_shown = ? WHERE uuid = ?")) {
      ps.setString(1, user.username());
      ps.setString(2, user.email());
      ps.setString(3, user.passwordHash());
      ps.setString(4, user.totpSecret());
      ps.setBoolean(5, user.premium());
      ps.setTimestamp(6, Timestamp.from(user.createdAt()));
      ps.setTimestamp(7, user.lastLoginAt() != null ? Timestamp.from(user.lastLoginAt()) : null);
      ps.setString(8, user.externalUserId());
      ps.setString(9, toJson(user.trustedDevices()));
      ps.setString(10, user.recoveryCodes());
      ps.setString(11, user.sourceSystem());
      ps.setString(12, user.migrationState());
      ps.setTimestamp(
          13, user.passwordMigratedAt() != null ? Timestamp.from(user.passwordMigratedAt()) : null);
      ps.setString(14, user.lastLoginIp());
      ps.setString(15, user.lastLoginIsp());
      ps.setString(16, user.lastLoginLocation());
      ps.setLong(17, user.totalPlaytime());
      ps.setTimestamp(18, user.lastLogoutAt() != null ? Timestamp.from(user.lastLogoutAt()) : null);
      ps.setBoolean(19, user.welcomeMessageShown());
      ps.setObject(20, user.uuid());
      ps.executeUpdate();
    }
  }

  private <T> Optional<T> queryOne(String sql, ParamBinder binder, RowMapper<T> mapper) {
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      binder.bind(ps);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? Optional.of(mapper.map(rs)) : Optional.empty();
      }
    } catch (SQLException e) {
      throw new RuntimeException("Query failed: " + sql, e);
    }
  }

  private <T> List<T> queryList(String sql, ParamBinder binder, RowMapper<T> mapper) {
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      binder.bind(ps);
      try (ResultSet rs = ps.executeQuery()) {
        List<T> results = new ArrayList<>();
        while (rs.next()) {
          results.add(mapper.map(rs));
        }
        return results;
      }
    } catch (SQLException e) {
      throw new RuntimeException("Query failed: " + sql, e);
    }
  }

  private void execute(String sql, ParamBinder binder) {
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      binder.bind(ps);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Execute failed: " + sql, e);
    }
  }

  private void withTransaction(TransactionBody body) {
    try (Connection conn = dataSource.getConnection()) {
      conn.setAutoCommit(false);
      try {
        body.execute(conn);
        conn.commit();
      } catch (Exception e) {
        conn.rollback();
        throw new RuntimeException("Transaction failed", e);
      }
    } catch (SQLException e) {
      throw new RuntimeException("Transaction failed", e);
    }
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
    Timestamp lastLogoutAt = rs.getTimestamp("last_logout_at");
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
        passwordMigratedAt != null ? passwordMigratedAt.toInstant() : null,
        rs.getString("last_login_ip"),
        rs.getString("last_login_isp"),
        rs.getString("last_login_location"),
        rs.getLong("total_playtime"),
        lastLogoutAt != null ? lastLogoutAt.toInstant() : null,
        rs.getBoolean("welcome_message_shown"));
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

  @FunctionalInterface
  private interface ParamBinder {
    void bind(PreparedStatement ps) throws SQLException;
  }

  @FunctionalInterface
  private interface RowMapper<T> {
    T map(ResultSet rs) throws SQLException;
  }

  @FunctionalInterface
  private interface TransactionBody {
    void execute(Connection conn) throws SQLException;
  }
}
