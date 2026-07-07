package io.github.addxiaoyi.starx.common.database;

import io.github.addxiaoyi.starx.common.model.Report;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;

public class JdbcReportRepository {

  private static final String SELECT_COLUMNS =
      "id, reporter_uuid, target_uuid, category, details, status, resolved_by, resolved_at";

  private final DataSource dataSource;

  public JdbcReportRepository(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public void create(Report report) {
    execute(
        "INSERT INTO starx_reports (id, reporter_uuid, target_uuid, category, details, status) VALUES (?, ?, ?, ?, ?, ?)",
        ps -> {
          ps.setString(1, report.id());
          ps.setObject(2, report.reporterUuid());
          ps.setObject(3, report.targetUuid());
          ps.setString(4, report.category());
          ps.setString(5, report.details());
          ps.setString(6, report.status());
        });
  }

  public List<Report> findByStatus(String status) {
    return queryList(
        "SELECT " + SELECT_COLUMNS + " FROM starx_reports WHERE status = ? ORDER BY id DESC",
        ps -> ps.setString(1, status),
        this::map);
  }

  public List<Report> findByTarget(UUID targetUuid) {
    return queryList(
        "SELECT " + SELECT_COLUMNS + " FROM starx_reports WHERE target_uuid = ? ORDER BY id DESC",
        ps -> ps.setObject(1, targetUuid),
        this::map);
  }

  public List<Report> findByReporter(UUID reporterUuid) {
    return queryList(
        "SELECT " + SELECT_COLUMNS + " FROM starx_reports WHERE reporter_uuid = ? ORDER BY id DESC",
        ps -> ps.setObject(1, reporterUuid),
        this::map);
  }

  public Optional<Report> findById(String id) {
    return queryOne(
        "SELECT " + SELECT_COLUMNS + " FROM starx_reports WHERE id = ?",
        ps -> ps.setString(1, id),
        this::map);
  }

  public List<Report> findAll() {
    return queryList(
        "SELECT " + SELECT_COLUMNS + " FROM starx_reports ORDER BY id DESC", ps -> {}, this::map);
  }

  public void resolve(String id, String resolvedBy) {
    execute(
        "UPDATE starx_reports SET status = 'RESOLVED', resolved_by = ?, resolved_at = ? WHERE id = ?",
        ps -> {
          ps.setString(1, resolvedBy);
          ps.setLong(2, System.currentTimeMillis());
          ps.setString(3, id);
        });
  }

  public void dismiss(String id, String resolvedBy) {
    execute(
        "UPDATE starx_reports SET status = 'DISMISSED', resolved_by = ?, resolved_at = ? WHERE id = ?",
        ps -> {
          ps.setString(1, resolvedBy);
          ps.setLong(2, System.currentTimeMillis());
          ps.setString(3, id);
        });
  }

  private Report map(ResultSet rs) throws SQLException {
    long resolvedAt = rs.getLong("resolved_at");
    return new Report(
        rs.getString("id"),
        UUID.fromString(rs.getString("reporter_uuid")),
        UUID.fromString(rs.getString("target_uuid")),
        rs.getString("category"),
        rs.getString("details"),
        rs.getString("status"),
        rs.getString("resolved_by"),
        rs.wasNull() ? null : resolvedAt);
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
