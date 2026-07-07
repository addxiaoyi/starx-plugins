package io.github.addxiaoyi.starx.velocity.http.admin;

import io.github.addxiaoyi.starx.common.database.JdbcReportRepository;
import io.github.addxiaoyi.starx.common.model.Report;
import io.github.addxiaoyi.starx.velocity.http.JsonHttpExchange;
import io.github.addxiaoyi.starx.velocity.http.RouteRegistrar;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class ReportHandler implements AdminHandler {

  private final JdbcReportRepository repo;

  public ReportHandler(JdbcReportRepository repo) {
    this.repo = Objects.requireNonNull(repo, "repo");
  }

  @Override
  public void register(RouteRegistrar routes) {
    routes.get("/v1/admin/reports", this::handleList);
    routes.post("/v1/admin/reports", this::handleCreate);
    routes.post("/v1/admin/reports/resolve", this::handleResolve);
    routes.post("/v1/admin/reports/dismiss", this::handleDismiss);
  }

  private void handleList(JsonHttpExchange ctx) throws IOException {
    String status = ctx.queryParam("status");
    List<Report> result;
    if (status != null && !status.isBlank()) {
      result = repo.findByStatus(status.toUpperCase());
    } else {
      result = repo.findAll();
    }
    ctx.status(200).json(result);
  }

  private void handleCreate(JsonHttpExchange ctx) throws IOException {
    ReportRequest req = ctx.bodyAsClass(ReportRequest.class);
    if (req.reporterUuid == null || req.targetUuid == null || req.category == null) {
      ctx.status(400).json(Map.of("error", "reporter_uuid, target_uuid, category are required"));
      return;
    }
    Report r =
        new Report(
            UUID.randomUUID().toString(),
            req.reporterUuid,
            req.targetUuid,
            req.category.toUpperCase(),
            req.details,
            "PENDING",
            null,
            null);
    repo.create(r);
    ctx.status(201).json(Map.of("id", r.id(), "success", true));
  }

  private void handleResolve(JsonHttpExchange ctx) throws IOException {
    ActionRequest req = ctx.bodyAsClass(ActionRequest.class);
    if (req.id == null || req.resolvedBy == null) {
      ctx.status(400).json(Map.of("error", "id and resolved_by are required"));
      return;
    }
    repo.resolve(req.id, req.resolvedBy);
    ctx.status(200).json(Map.of("success", true));
  }

  private void handleDismiss(JsonHttpExchange ctx) throws IOException {
    ActionRequest req = ctx.bodyAsClass(ActionRequest.class);
    if (req.id == null || req.resolvedBy == null) {
      ctx.status(400).json(Map.of("error", "id and resolved_by are required"));
      return;
    }
    repo.dismiss(req.id, req.resolvedBy);
    ctx.status(200).json(Map.of("success", true));
  }

  static final class ReportRequest {
    public UUID reporterUuid;
    public UUID targetUuid;
    public String category;
    public String details;
  }

  static final class ActionRequest {
    public String id;
    public String resolvedBy;
  }
}
