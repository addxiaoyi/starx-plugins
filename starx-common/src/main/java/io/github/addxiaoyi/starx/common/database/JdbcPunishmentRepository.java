package io.github.addxiaoyi.starx.common.database;

import io.github.addxiaoyi.starx.common.model.Punishment;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;

public class JdbcPunishmentRepository {

  private static final String SELECT_COLUMNS =
      "id, target_uuid, target_name, type, reason, staff_uuid, staff_name, created_at, expires_at, active";

  private final DataSource dataSource;

  public JdbcPunishmentRepository(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public void record(Punishment punishment) {
    execute("INSERT INTO starx_punishments (id, target_uuid, target_name, type, reason, "
        + "staff_uuid, staff_name, created_at, expires_at, active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        ps -> {
          ps.setString(1, punishment.id());
          ps.setObject(2, punishment.targetUuid());
          ps.setString(3, punishment.targetName());
          ps.setString(4, punishment.type());
          ps.setString(5, punishment.reason());
          ps.setObject(6, punishment.staffUuid());
          ps.setString(7, punishment.staffName());
          ps.setLong(8, punishment.createdAt());
          if (punishment.expiresAt() != null) {
            ps.setLong(9, punishment.expiresAt());
          } else {
            ps.setNull(9, java.sql.Types.BIGINT);
          }
          ps.setBoolean(10, punishment.active());
        });
  }

  public List<Punishment> findByPlayer(UUID targetUuid) {
    return queryList("SELECT " + SELECT_COLUMNS + " FROM starx_punishments WHERE target_uuid = ? ORDER BY created_at DESC",
        ps -> ps.setObject(1, targetUuid), this::map);
  }

  public List<Punishment> findByType(String type) {
    return queryList("SELECT " + SELECT_COLUMNS + " FROM starx_punishments WHERE type = ? ORDER BY created_at DESC",
        ps -> ps.setString(1, type), this::map);
  }

  public List<Punishment> findActive() {
    return queryList("SELECT " + SELECT_COLUMNS + " FROM starx_punishments WHERE active = TRUE ORDER BY created_at DESC",
        ps -> {}, this::map);
  }

  public Optional<Punishment> findById(String id) {
    return queryOne("SELECT " + SELECT_COLUMNS + " FROM starx_punishments WHERE id = ?",
        ps -> ps.setString(1, id), this::map);
  }

  public void deactivate(String id) {
    execute("UPDATE starx_punishments SET active = FALSE WHERE id = ?", ps -> ps.setString(1, id));
  }

  public List<Punishment> findAll() {
    return queryList("SELECT " + SELECT_COLUMNS + " FROM starx_punishments ORDER BY created_at DESC",
        ps -> {}, this::map);
  }

  private Punishment map(ResultSet rs) throws SQLException {
    long exp = rs.getLong("expires_at");
    return new Punishment(
        rs.getString("id"),
        UUID.fromString(rs.getString("target_uuid")),
        rs.getString("target_name"),
        rs.getString("type"),
        rs.getString("reason"),
        UUID.fromString(rs.getString("staff_uuid")),
        rs.getString("staff_name"),
        rs.getLong("created_at"),
        rs.wasNull() ? null : exp,
        rs.getBoolean("active"));
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
        while (rs.next()) results.add(mapper.map(rs));
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

  @FunctionalInterface
  private interface ParamBinder { void bind(PreparedStatement ps) throws SQLException; }

  @FunctionalInterface
  private interface RowMapper<T> { T map(ResultSet rs) throws SQLException; }
}
