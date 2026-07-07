package io.github.addxiaoyi.starx.velocity.http.admin;

import io.github.addxiaoyi.starx.common.database.JdbcPunishmentRepository;
import io.github.addxiaoyi.starx.common.model.Punishment;
import io.github.addxiaoyi.starx.velocity.http.JsonHttpExchange;
import io.github.addxiaoyi.starx.velocity.http.RouteRegistrar;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class PunishmentHandler implements AdminHandler {

  private final JdbcPunishmentRepository repo;

  public PunishmentHandler(JdbcPunishmentRepository repo) {
    this.repo = Objects.requireNonNull(repo, "repo");
  }

  @Override
  public void register(RouteRegistrar routes) {
    routes.get("/v1/admin/punishments", this::handleList);
    routes.post("/v1/admin/punishments", this::handleCreate);
  }

  private void handleList(JsonHttpExchange ctx) throws IOException {
    String player = ctx.queryParam("player");
    List<Punishment> result;
    if (player != null && !player.isBlank()) {
      result = repo.findByPlayer(UUID.fromString(player));
    } else {
      result = repo.findAll();
    }
    ctx.status(200).json(result);
  }

  private void handleCreate(JsonHttpExchange ctx) throws IOException {
    PunishmentRequest req = ctx.bodyAsClass(PunishmentRequest.class);
    if (req.targetUuid == null || req.type == null || req.staffUuid == null) {
      ctx.status(400).json(Map.of("error", "target_uuid, type, staff_uuid are required"));
      return;
    }
    Punishment p =
        new Punishment(
            UUID.randomUUID().toString(),
            req.targetUuid,
            req.targetName,
            req.type,
            req.reason,
            req.staffUuid,
            req.staffName,
            System.currentTimeMillis(),
            req.expiresAt,
            true);
    repo.record(p);
    ctx.status(201).json(Map.of("id", p.id(), "success", true));
  }

  static final class PunishmentRequest {
    public UUID targetUuid;
    public String targetName;
    public String type;
    public String reason;
    public UUID staffUuid;
    public String staffName;
    public Long expiresAt;
  }
}
