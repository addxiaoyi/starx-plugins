package io.github.addxiaoyi.starx.common.database;

import io.github.addxiaoyi.starx.common.model.StaffVote;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;

public class JdbcVoteRepository {

  private final DataSource dataSource;

  public JdbcVoteRepository(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public void create(StaffVote vote) {
    execute("INSERT INTO starx_staff_votes (id, target_uuid, target_name, reason, vote_type, "
        + "status, initiator_uuid, initiator_name, yes_votes, no_votes, required_yes, "
        + "expires_at, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        ps -> {
          ps.setString(1, vote.id());
          ps.setObject(2, vote.targetUuid());
          ps.setString(3, vote.targetName());
          ps.setString(4, vote.reason());
          ps.setString(5, vote.voteType());
          ps.setString(6, vote.status());
          ps.setObject(7, vote.initiatorUuid());
          ps.setString(8, vote.initiatorName());
          ps.setInt(9, vote.yesVotes());
          ps.setInt(10, vote.noVotes());
          ps.setInt(11, vote.requiredYes());
          ps.setLong(12, vote.expiresAt());
          ps.setLong(13, vote.createdAt());
        });
  }

  public Optional<StaffVote> findById(String id) {
    return queryOne("SELECT * FROM starx_staff_votes WHERE id = ?", ps -> ps.setString(1, id), this::map);
  }

  public Optional<StaffVote> findActive() {
    long now = System.currentTimeMillis();
    return queryOne("SELECT * FROM starx_staff_votes WHERE status = 'ACTIVE' AND expires_at > ? "
        + "ORDER BY created_at DESC LIMIT 1", ps -> ps.setLong(1, now), this::map);
  }

  public List<StaffVote> findByInitiator(UUID initiatorUuid) {
    return queryMany("SELECT * FROM starx_staff_votes WHERE initiator_uuid = ? ORDER BY created_at DESC",
        ps -> ps.setObject(1, initiatorUuid), this::map);
  }

  public List<StaffVote> findAllActive() {
    long now = System.currentTimeMillis();
    return queryMany("SELECT * FROM starx_staff_votes WHERE status = 'ACTIVE' AND expires_at > ? "
        + "ORDER BY created_at DESC", ps -> ps.setLong(1, now), this::map);
  }

  public void updateStatus(String id, String status, Long resolvedAt) {
    if (resolvedAt != null) {
      execute("UPDATE starx_staff_votes SET status = ?, resolved_at = ? WHERE id = ?",
          ps -> { ps.setString(1, status); ps.setLong(2, resolvedAt); ps.setString(3, id); });
    } else {
      execute("UPDATE starx_staff_votes SET status = ? WHERE id = ?",
          ps -> { ps.setString(1, status); ps.setString(2, id); });
    }
  }

  public void castVote(String voteId, UUID voterUuid, boolean yes) {
    execute("INSERT INTO starx_staff_vote_records (vote_id, voter_uuid, vote, voted_at) VALUES (?, ?, ?, ?)",
        ps -> {
          ps.setString(1, voteId);
          ps.setObject(2, voterUuid);
          ps.setString(3, yes ? "YES" : "NO");
          ps.setLong(4, System.currentTimeMillis());
        });
    execute("UPDATE starx_staff_votes SET "
        + (yes ? "yes_votes = yes_votes + 1" : "no_votes = no_votes + 1")
        + " WHERE id = ?", ps -> ps.setString(1, voteId));
  }

  public boolean hasVoted(String voteId, UUID voterUuid) {
    return queryOne("SELECT 1 FROM starx_staff_vote_records WHERE vote_id = ? AND voter_uuid = ?",
        ps -> { ps.setString(1, voteId); ps.setObject(2, voterUuid); },
        rs -> true).isPresent();
  }

  public int countYes(String voteId) {
    return queryOne("SELECT COUNT(*) FROM starx_staff_vote_records WHERE vote_id = ? AND vote = 'YES'",
        ps -> ps.setString(1, voteId), rs -> rs.getInt(1)).orElse(0);
  }

  private StaffVote map(ResultSet rs) throws SQLException {
    Long resolvedAt = rs.getLong("resolved_at");
    if (rs.wasNull()) resolvedAt = null;
    return new StaffVote(
        rs.getString("id"),
        UUID.fromString(rs.getString("target_uuid")),
        rs.getString("target_name"),
        rs.getString("reason"),
        rs.getString("vote_type"),
        rs.getString("status"),
        UUID.fromString(rs.getString("initiator_uuid")),
        rs.getString("initiator_name"),
        rs.getInt("yes_votes"),
        rs.getInt("no_votes"),
        rs.getInt("required_yes"),
        rs.getLong("expires_at"),
        rs.getLong("created_at"),
        resolvedAt);
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

  private <T> List<T> queryMany(String sql, ParamBinder binder, RowMapper<T> mapper) {
    List<T> results = new ArrayList<>();
    try (Connection conn = dataSource.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      binder.bind(ps);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) results.add(mapper.map(rs));
      }
    } catch (SQLException e) {
      throw new RuntimeException("Query failed: " + sql, e);
    }
    return results;
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
