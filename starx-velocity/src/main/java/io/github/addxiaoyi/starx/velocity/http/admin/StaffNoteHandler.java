package io.github.addxiaoyi.starx.velocity.http.admin;

import io.github.addxiaoyi.starx.common.database.JdbcStaffNoteRepository;
import io.github.addxiaoyi.starx.common.model.StaffNote;
import io.github.addxiaoyi.starx.velocity.http.JsonHttpExchange;
import io.github.addxiaoyi.starx.velocity.http.RouteRegistrar;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class StaffNoteHandler implements AdminHandler {

  private final JdbcStaffNoteRepository repo;

  public StaffNoteHandler(JdbcStaffNoteRepository repo) {
    this.repo = Objects.requireNonNull(repo, "repo");
  }

  @Override
  public void register(RouteRegistrar routes) {
    routes.get("/v1/admin/notes", this::handleList);
    routes.post("/v1/admin/notes", this::handleCreate);
  }

  private void handleList(JsonHttpExchange ctx) throws IOException {
    String player = ctx.queryParam("player");
    List<StaffNote> result;
    if (player != null && !player.isBlank()) {
      result = repo.findByPlayer(UUID.fromString(player));
    } else {
      result = repo.findAll();
    }
    ctx.status(200).json(result);
  }

  private void handleCreate(JsonHttpExchange ctx) throws IOException {
    NoteRequest req = ctx.bodyAsClass(NoteRequest.class);
    if (req.targetUuid == null || req.note == null || req.note.isBlank()) {
      ctx.status(400).json(Map.of("error", "target_uuid and note are required"));
      return;
    }
    String severity = req.severity != null ? req.severity.toUpperCase() : "INFO";
    if (!severity.equals("INFO") && !severity.equals("WARNING") && !severity.equals("CRITICAL")) {
      ctx.status(400).json(Map.of("error", "severity must be INFO, WARNING, or CRITICAL"));
      return;
    }
    StaffNote note = new StaffNote(
        UUID.randomUUID().toString(), req.targetUuid, req.note, severity,
        req.staffUuid != null ? req.staffUuid : UUID.fromString("00000000-0000-0000-0000-000000000000"),
        System.currentTimeMillis());
    repo.addNote(note);
    ctx.status(201).json(Map.of("id", note.id(), "success", true));
  }

  static final class NoteRequest {
    public UUID targetUuid;
    public String note;
    public String severity;
    public UUID staffUuid;
  }
}
