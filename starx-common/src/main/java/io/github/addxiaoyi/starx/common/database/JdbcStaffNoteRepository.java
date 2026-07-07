package io.github.addxiaoyi.starx.common.database;

import io.github.addxiaoyi.starx.common.model.StaffNote;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;

public class JdbcStaffNoteRepository {

  private static final String SELECT_COLUMNS =
      "id, target_uuid, note, severity, staff_uuid, created_at";

  private final DataSource dataSource;

  public JdbcStaffNoteRepository(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public void addNote(StaffNote note) {
    execute(
        "INSERT INTO starx_staff_notes (id, target_uuid, note, severity, staff_uuid, created_at) VALUES (?, ?, ?, ?, ?, ?)",
        ps -> {
          ps.setString(1, note.id());
          ps.setObject(2, note.targetUuid());
          ps.setString(3, note.note());
          ps.setString(4, note.severity());
          ps.setObject(5, note.staffUuid());
          ps.setLong(6, note.createdAt());
        });
  }

  public List<StaffNote> findByPlayer(UUID targetUuid) {
    return queryList(
        "SELECT "
            + SELECT_COLUMNS
            + " FROM starx_staff_notes WHERE target_uuid = ? ORDER BY created_at DESC",
        ps -> ps.setObject(1, targetUuid),
        this::map);
  }

  public List<StaffNote> findAll() {
    return queryList(
        "SELECT " + SELECT_COLUMNS + " FROM starx_staff_notes ORDER BY created_at DESC",
        ps -> {},
        this::map);
  }

  public Optional<StaffNote> findById(String id) {
    return queryOne(
        "SELECT " + SELECT_COLUMNS + " FROM starx_staff_notes WHERE id = ?",
        ps -> ps.setString(1, id),
        this::map);
  }

  private StaffNote map(ResultSet rs) throws SQLException {
    return new StaffNote(
        rs.getString("id"),
        UUID.fromString(rs.getString("target_uuid")),
        rs.getString("note"),
        rs.getString("severity"),
        UUID.fromString(rs.getString("staff_uuid")),
        rs.getLong("created_at"));
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
  private interface ParamBinder {
    void bind(PreparedStatement ps) throws SQLException;
  }

  @FunctionalInterface
  private interface RowMapper<T> {
    T map(ResultSet rs) throws SQLException;
  }
}
