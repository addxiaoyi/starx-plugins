package io.github.addxiaoyi.starx.common.database;

import io.github.addxiaoyi.starx.common.model.Announcement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;

public class JdbcAnnouncementRepository {

  private static final String SELECT_COLUMNS = "id, title, content, created_by, created_at, expires_at";

  private final DataSource dataSource;

  public JdbcAnnouncementRepository(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public void create(Announcement announcement) {
    execute("INSERT INTO starx_announcements (id, title, content, created_by, created_at, expires_at) VALUES (?, ?, ?, ?, ?, ?)",
        ps -> {
          ps.setString(1, announcement.id());
          ps.setString(2, announcement.title());
          ps.setString(3, announcement.content());
          ps.setString(4, announcement.createdBy());
          ps.setLong(5, announcement.createdAt());
          if (announcement.expiresAt() != null) {
            ps.setLong(6, announcement.expiresAt());
          } else {
            ps.setNull(6, java.sql.Types.BIGINT);
          }
        });
  }

  public List<Announcement> findActive() {
    long now = System.currentTimeMillis();
    return queryList("SELECT " + SELECT_COLUMNS + " FROM starx_announcements WHERE expires_at IS NULL OR expires_at > ? ORDER BY created_at DESC",
        ps -> ps.setLong(1, now), this::map);
  }

  public Optional<Announcement> findById(String id) {
    return queryOne("SELECT " + SELECT_COLUMNS + " FROM starx_announcements WHERE id = ?",
        ps -> ps.setString(1, id), this::map);
  }

  public List<Announcement> findUnreadByPlayer(UUID playerUuid) {
    long now = System.currentTimeMillis();
    return queryList(
        "SELECT a.id, a.title, a.content, a.created_by, a.created_at, a.expires_at"
        + " FROM starx_announcements a"
        + " LEFT JOIN starx_announcement_reads r ON a.id = r.announcement_id AND r.player_uuid = ?"
        + " WHERE (a.expires_at IS NULL OR a.expires_at > ?)"
        + " AND r.announcement_id IS NULL"
        + " ORDER BY a.created_at DESC",
        ps -> { ps.setObject(1, playerUuid); ps.setLong(2, now); }, this::map);
  }

  public void markRead(String announcementId, UUID playerUuid) {
    execute("INSERT OR IGNORE INTO starx_announcement_reads (announcement_id, player_uuid, read_at) VALUES (?, ?, ?)",
        ps -> { ps.setString(1, announcementId); ps.setObject(2, playerUuid); ps.setLong(3, System.currentTimeMillis()); });
  }

  private Announcement map(ResultSet rs) throws SQLException {
    long exp = rs.getLong("expires_at");
    return new Announcement(
        rs.getString("id"),
        rs.getString("title"),
        rs.getString("content"),
        rs.getString("created_by"),
        rs.getLong("created_at"),
        rs.wasNull() ? null : exp);
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
