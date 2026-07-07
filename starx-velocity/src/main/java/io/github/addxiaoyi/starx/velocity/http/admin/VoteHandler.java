package io.github.addxiaoyi.starx.velocity.http.admin;

import io.github.addxiaoyi.starx.common.database.JdbcVoteRepository;
import io.github.addxiaoyi.starx.velocity.http.JsonHttpExchange;
import io.github.addxiaoyi.starx.velocity.http.RouteRegistrar;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class VoteHandler implements AdminHandler {

  private final JdbcVoteRepository repo;

  public VoteHandler(JdbcVoteRepository repo) {
    this.repo = Objects.requireNonNull(repo, "repo");
  }

  @Override
  public void register(RouteRegistrar routes) {
    routes.get("/v1/admin/votes", this::handleList);
    routes.get("/v1/admin/votes/active", this::handleActive);
    routes.post("/v1/admin/votes/cast", this::handleCast);
  }

  private void handleList(JsonHttpExchange ctx) throws IOException {
    ctx.status(200).json(repo.findAllActive());
  }

  private void handleActive(JsonHttpExchange ctx) throws IOException {
    var active = repo.findActive();
    if (active.isPresent()) {
      ctx.status(200).json(active.get());
    } else {
      ctx.status(404).json(Map.of("error", "No active vote"));
    }
  }

  private void handleCast(JsonHttpExchange ctx) throws IOException {
    CastRequest req = ctx.bodyAsClass(CastRequest.class);
    if (req.voteId == null || req.voterUuid == null || req.vote == null) {
      ctx.status(400).json(Map.of("error", "voteId, voterUuid, and vote are required"));
      return;
    }
    if (!"YES".equalsIgnoreCase(req.vote) && !"NO".equalsIgnoreCase(req.vote)) {
      ctx.status(400).json(Map.of("error", "vote must be YES or NO"));
      return;
    }
    if (repo.hasVoted(req.voteId, req.voterUuid)) {
      ctx.status(409).json(Map.of("error", "Already voted"));
      return;
    }
    repo.castVote(req.voteId, req.voterUuid, "YES".equalsIgnoreCase(req.vote));
    ctx.status(200).json(Map.of("success", true));
  }

  static final class CastRequest {
    public String voteId;
    public UUID voterUuid;
    public String vote;
  }
}
