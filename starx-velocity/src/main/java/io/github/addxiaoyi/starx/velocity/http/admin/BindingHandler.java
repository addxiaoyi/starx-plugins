package io.github.addxiaoyi.starx.velocity.http.admin;

import io.github.addxiaoyi.starx.common.auth.BindingVerificationService;
import io.github.addxiaoyi.starx.common.database.JdbcBindingRepository;
import io.github.addxiaoyi.starx.common.database.JdbcUserRepository;
import io.github.addxiaoyi.starx.common.model.PlayerBinding;
import io.github.addxiaoyi.starx.velocity.http.JsonHttpExchange;
import io.github.addxiaoyi.starx.velocity.http.RouteRegistrar;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class BindingHandler implements AdminHandler {

  private final JdbcBindingRepository repo;
  private final JdbcUserRepository userRepo;
  private final BindingVerificationService verificationService;

  public BindingHandler(JdbcBindingRepository repo, JdbcUserRepository userRepo,
      BindingVerificationService verificationService) {
    this.repo = Objects.requireNonNull(repo, "repo");
    this.userRepo = Objects.requireNonNull(userRepo, "userRepo");
    this.verificationService = Objects.requireNonNull(verificationService, "verificationService");
  }

  @Override
  public void register(RouteRegistrar routes) {
    routes.get("/v1/admin/bindings", this::handleQuery);
    routes.post("/v1/admin/bindings", this::handleSave);
    routes.post("/v1/admin/bindings/verify-code", this::handleGenerateCode);
    routes.post("/v1/admin/bindings/verify", this::handleVerify);
  }

  private void handleQuery(JsonHttpExchange ctx) throws IOException {
    String player = ctx.queryParam("player");
    String qq = ctx.queryParam("qq");
    String discord = ctx.queryParam("discord");

    Optional<PlayerBinding> result;
    if (player != null && !player.isBlank()) {
      result = repo.findByPlayer(UUID.fromString(player));
    } else if (qq != null && !qq.isBlank()) {
      result = repo.findByQq(qq);
    } else if (discord != null && !discord.isBlank()) {
      result = repo.findByDiscord(discord);
    } else {
      ctx.status(400).json(Map.of("error", "player, qq, or discord param is required"));
      return;
    }

    if (result.isPresent()) {
      ctx.status(200).json(result.get());
    } else {
      ctx.status(404).json(Map.of("error", "Binding not found"));
    }
  }

  private void handleSave(JsonHttpExchange ctx) throws IOException {
    SaveRequest req = ctx.bodyAsClass(SaveRequest.class);
    if (req.playerUuid == null) {
      ctx.status(400).json(Map.of("error", "player_uuid is required"));
      return;
    }
    PlayerBinding binding = new PlayerBinding(
        req.playerUuid, req.qqId, req.discordId, System.currentTimeMillis());
    repo.save(binding);
    ctx.status(200).json(Map.of("success", true));
  }

  private void handleGenerateCode(JsonHttpExchange ctx) throws IOException {
    CodeRequest req = ctx.bodyAsClass(CodeRequest.class);
    if (req.playerUuid == null) {
      ctx.status(400).json(Map.of("error", "player_uuid is required"));
      return;
    }
    if (!userRepo.existsByUuid(req.playerUuid)) {
      ctx.status(404).json(Map.of("error", "Player not found"));
      return;
    }

    String code = verificationService.generateCode(req.playerUuid);
    ctx.status(200).json(Map.of("code", code, "message", "Send this code to the QQ bot"));
  }

  private void handleVerify(JsonHttpExchange ctx) throws IOException {
    VerifyRequest req = ctx.bodyAsClass(VerifyRequest.class);
    if (req.code == null || req.code.isBlank()) {
      ctx.status(400).json(Map.of("error", "code is required"));
      return;
    }

    UUID playerUuid = verificationService.verifyCode(req.code);
    if (playerUuid == null) {
      ctx.status(404).json(Map.of("error", "Invalid or expired code"));
      return;
    }

    PlayerBinding binding = new PlayerBinding(
        playerUuid, req.qqId, null, System.currentTimeMillis());
    repo.save(binding);

    ctx.status(200).json(Map.of("success", true, "player_uuid", playerUuid.toString()));
  }

  static final class SaveRequest {
    public UUID playerUuid;
    public String qqId;
    public String discordId;
  }

  static final class CodeRequest {
    public UUID playerUuid;
  }

  static final class VerifyRequest {
    public String code;
    public String qqId;
  }
}
