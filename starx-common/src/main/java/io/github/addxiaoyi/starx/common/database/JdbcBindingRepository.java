package io.github.addxiaoyi.starx.common.database;

import io.github.addxiaoyi.starx.common.model.PlayerBinding;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;

public class JdbcBindingRepository {

  private final DataSource dataSource;

  public JdbcBindingRepository(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public Optional<PlayerBinding> findByPlayer(UUID playerUuid) {
    return queryOne("SELECT player_uuid, qq_id, discord_id, created_at FROM starx_player_bindings WHERE player_uuid = ?",
        ps -> ps.setObject(1, playerUuid), this::map);
  }

  public Optional<PlayerBinding> findByQq(String qqId) {
    return queryOne("SELECT player_uuid, qq_id, discord_id, created_at FROM starx_player_bindings WHERE qq_id = ?",
        ps -> ps.setString(1, qqId), this::map);
  }

  public Optional<PlayerBinding> findByDiscord(String discordId) {
    return queryOne("SELECT player_uuid, qq_id, discord_id, created_at FROM starx_player_bindings WHERE discord_id = ?",
        ps -> ps.setString(1, discordId), this::map);
  }

  public void save(PlayerBinding binding) {
    withTransaction(conn -> {
      try (PreparedStatement ps = conn.prepareStatement(
          "SELECT 1 FROM starx_player_bindings WHERE player_uuid = ?")) {
        ps.setObject(1, binding.playerUuid());
        try (ResultSet rs = ps.executeQuery()) {
          if (rs.next()) {
            try (PreparedStatement update = conn.prepareStatement(
                "UPDATE starx_player_bindings SET qq_id = ?, discord_id = ? WHERE player_uuid = ?")) {
              update.setString(1, binding.qqId());
              update.setString(2, binding.discordId());
              update.setObject(3, binding.playerUuid());
              update.executeUpdate();
            }
          } else {
            try (PreparedStatement insert = conn.prepareStatement(
                "INSERT INTO starx_player_bindings (player_uuid, qq_id, discord_id, created_at) VALUES (?, ?, ?, ?)")) {
              insert.setObject(1, binding.playerUuid());
              insert.setString(2, binding.qqId());
              insert.setString(3, binding.discordId());
              insert.setLong(4, binding.createdAt());
              insert.executeUpdate();
            }
          }
        }
      }
    });
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

  private PlayerBinding map(ResultSet rs) throws SQLException {
    return new PlayerBinding(
        UUID.fromString(rs.getString("player_uuid")),
        rs.getString("qq_id"),
        rs.getString("discord_id"),
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

  @FunctionalInterface
  private interface TransactionBody { void execute(Connection conn) throws Exception; }
}
