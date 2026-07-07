package io.github.addxiaoyi.starx.velocity.http.admin;

import io.github.addxiaoyi.starx.common.database.JdbcAnnouncementRepository;
import io.github.addxiaoyi.starx.common.model.Announcement;
import io.github.addxiaoyi.starx.velocity.http.JsonHttpExchange;
import io.github.addxiaoyi.starx.velocity.http.RouteRegistrar;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class AnnouncementHandler implements AdminHandler {

  private final JdbcAnnouncementRepository repo;

  public AnnouncementHandler(JdbcAnnouncementRepository repo) {
    this.repo = Objects.requireNonNull(repo, "repo");
  }

  @Override
  public void register(RouteRegistrar routes) {
    routes.post("/v1/admin/announcements", this::handleCreate);
    routes.get("/v1/admin/announcements", this::handleList);
    routes.post("/v1/admin/announcements/read", this::handleRead);
  }

  private void handleCreate(JsonHttpExchange ctx) throws IOException {
    AnnouncementRequest req = ctx.bodyAsClass(AnnouncementRequest.class);
    if (req.title == null || req.title.isBlank() || req.content == null || req.content.isBlank()) {
      ctx.status(400).json(Map.of("error", "title and content are required"));
      return;
    }
    Announcement a =
        new Announcement(
            UUID.randomUUID().toString(),
            req.title,
            req.content,
            req.createdBy != null ? req.createdBy : "console",
            System.currentTimeMillis(),
            req.expiresAt);
    repo.create(a);
    ctx.status(201).json(Map.of("id", a.id(), "success", true));
  }

  private void handleList(JsonHttpExchange ctx) throws IOException {
    String player = ctx.queryParam("player");
    List<Announcement> result;
    if (player != null && !player.isBlank()) {
      result = repo.findUnreadByPlayer(UUID.fromString(player));
    } else {
      result = repo.findActive();
    }
    ctx.status(200).json(result);
  }

  private void handleRead(JsonHttpExchange ctx) throws IOException {
    ReadRequest req = ctx.bodyAsClass(ReadRequest.class);
    if (req.announcementId == null || req.playerUuid == null) {
      ctx.status(400).json(Map.of("error", "announcement_id and player_uuid are required"));
      return;
    }
    repo.markRead(req.announcementId, req.playerUuid);
    ctx.status(200).json(Map.of("success", true));
  }

  static final class AnnouncementRequest {
    public String title;
    public String content;
    public String createdBy;
    public Long expiresAt;
  }

  static final class ReadRequest {
    public String announcementId;
    public UUID playerUuid;
  }
}
