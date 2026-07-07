package io.github.addxiaoyi.starx.velocity.module.vote;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.addxiaoyi.starx.common.database.JdbcVoteRepository;
import io.github.addxiaoyi.starx.common.model.StaffVote;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import io.github.addxiaoyi.starx.velocity.module.VelocityModule;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class VoteModule implements VelocityModule {

  private static final long VOTE_DURATION_MS = 120_000;

  private final StarxVelocityPlugin plugin;
  private final JdbcVoteRepository voteRepo;

  public VoteModule(StarxVelocityPlugin plugin, JdbcVoteRepository voteRepo) {
    this.plugin = plugin;
    this.voteRepo = voteRepo;
  }

  @Override
  public String name() {
    return "starx.vote";
  }

  @Override
  public void onEnable() {
    ProxyServer proxy = plugin.proxy();
    var reg = proxy.getCommandManager();
    reg.register(reg.metaBuilder("votestart").build(), new VoteStartCommand());
    reg.register(reg.metaBuilder("vote").build(), new VoteCastCommand());
    reg.register(reg.metaBuilder("voteinfo").build(), new VoteInfoCommand());
  }

  @Override
  public void onDisable() {}

  private final class VoteStartCommand implements SimpleCommand {
    @Override
    public void execute(Invocation inv) {
      if (!inv.source().hasPermission("starx.vote.start")) {
        inv.source().sendMessage(Component.text("No permission.", NamedTextColor.RED));
        return;
      }
      if (!(inv.source() instanceof Player staff)) {
        inv.source().sendMessage(Component.text("Players only.", NamedTextColor.RED));
        return;
      }
      String[] args = inv.arguments();
      if (args.length < 2) {
        inv.source().sendMessage(Component.text("Usage: /votestart <player> <reason>", NamedTextColor.YELLOW));
        return;
      }

      Optional<StaffVote> active = voteRepo.findActive();
      if (active.isPresent()) {
        inv.source().sendMessage(Component.text("An active vote is already in progress.", NamedTextColor.RED));
        return;
      }

      String targetName = args[0];
      String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

      Optional<Player> target = plugin.proxy().getPlayer(targetName);
      if (target.isEmpty()) {
        inv.source().sendMessage(Component.text("Player not found.", NamedTextColor.RED));
        return;
      }

      StaffVote vote = new StaffVote(
          UUID.randomUUID().toString(),
          target.get().getUniqueId(), targetName, reason, "STAFF_VOTE",
          "ACTIVE", staff.getUniqueId(), staff.getUsername(),
          0, 0, 3,
          System.currentTimeMillis() + VOTE_DURATION_MS,
          System.currentTimeMillis(), null);
      voteRepo.create(vote);

      Component msg = Component.text()
          .append(Component.text("\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500", NamedTextColor.DARK_GRAY))
          .append(Component.text("\n[VOTE] ", NamedTextColor.GOLD))
          .append(Component.text(staff.getUsername(), NamedTextColor.YELLOW))
          .append(Component.text(" started a vote on ", NamedTextColor.WHITE))
          .append(Component.text(targetName, NamedTextColor.RED))
          .append(Component.text(": ", NamedTextColor.WHITE))
          .append(Component.text(reason, NamedTextColor.GRAY))
          .append(Component.text("\nType ", NamedTextColor.WHITE))
          .append(Component.text("/vote yes", NamedTextColor.GREEN))
          .append(Component.text(" or ", NamedTextColor.WHITE))
          .append(Component.text("/vote no", NamedTextColor.RED))
          .append(Component.text(" to cast your vote.", NamedTextColor.WHITE))
          .append(Component.text("\n\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500", NamedTextColor.DARK_GRAY))
          .build();

      for (Player p : plugin.proxy().getAllPlayers()) {
        if (p.hasPermission("starx.vote.cast")) {
          p.sendMessage(msg);
        }
      }
    }

    @Override
    public List<String> suggest(Invocation inv) {
      if (inv.arguments().length <= 1) {
        String prefix = inv.arguments().length == 0 ? "" : inv.arguments()[0].toLowerCase();
        return plugin.proxy().getAllPlayers().stream()
            .map(Player::getUsername)
            .filter(n -> n.toLowerCase().startsWith(prefix))
            .collect(Collectors.toList());
      }
      return List.of();
    }
  }

  private final class VoteCastCommand implements SimpleCommand {
    @Override
    public void execute(Invocation inv) {
      if (!inv.source().hasPermission("starx.vote.cast")) {
        inv.source().sendMessage(Component.text("No permission.", NamedTextColor.RED));
        return;
      }
      if (!(inv.source() instanceof Player voter)) {
        inv.source().sendMessage(Component.text("Players only.", NamedTextColor.RED));
        return;
      }
      String[] args = inv.arguments();
      if (args.length == 0) {
        inv.source().sendMessage(Component.text("Usage: /vote <yes|no>", NamedTextColor.YELLOW));
        return;
      }

      String choice = args[0].toLowerCase();
      if (!"yes".equals(choice) && !"no".equals(choice)) {
        inv.source().sendMessage(Component.text("Vote yes or no.", NamedTextColor.RED));
        return;
      }

      Optional<StaffVote> active = voteRepo.findActive();
      if (active.isEmpty()) {
        inv.source().sendMessage(Component.text("No active vote.", NamedTextColor.RED));
        return;
      }

      StaffVote vote = active.get();
      if (voteRepo.hasVoted(vote.id(), voter.getUniqueId())) {
        inv.source().sendMessage(Component.text("You already voted.", NamedTextColor.RED));
        return;
      }

      boolean yes = "yes".equals(choice);
      voteRepo.castVote(vote.id(), voter.getUniqueId(), yes);
      inv.source().sendMessage(Component.text("Vote cast: " + choice.toUpperCase(), NamedTextColor.GREEN));

      int yesCount = voteRepo.countYes(vote.id());
      // Vote is a copy at this point; re-read from DB to get accurate counts
      Optional<StaffVote> updated = voteRepo.findById(vote.id());
      if (updated.isPresent()) {
        StaffVote current = updated.get();
        if (yesCount >= current.requiredYes()) {
          voteRepo.updateStatus(current.id(), "PASSED", System.currentTimeMillis());
          Component result = Component.text()
              .append(Component.text("[VOTE] ", NamedTextColor.GOLD))
              .append(Component.text("Vote PASSED on ", NamedTextColor.GREEN))
              .append(Component.text(current.targetName(), NamedTextColor.RED))
              .append(Component.text(". Reason: ", NamedTextColor.WHITE))
              .append(Component.text(current.reason(), NamedTextColor.GRAY))
              .build();
          for (Player p : plugin.proxy().getAllPlayers()) {
            if (p.hasPermission("starx.vote.cast")) {
              p.sendMessage(result);
            }
          }
        }
      }
    }

    @Override
    public List<String> suggest(Invocation inv) {
      if (inv.arguments().length <= 1) {
        String prefix = inv.arguments().length == 0 ? "" : inv.arguments()[0].toLowerCase();
        return List.of("yes", "no").stream()
            .filter(s -> s.startsWith(prefix))
            .collect(Collectors.toList());
      }
      return List.of();
    }
  }

  private final class VoteInfoCommand implements SimpleCommand {
    @Override
    public void execute(Invocation inv) {
      if (!inv.source().hasPermission("starx.vote.cast")) {
        inv.source().sendMessage(Component.text("No permission.", NamedTextColor.RED));
        return;
      }

      Optional<StaffVote> active = voteRepo.findActive();
      if (active.isEmpty()) {
        inv.source().sendMessage(Component.text("No active vote.", NamedTextColor.GRAY));
        return;
      }

      StaffVote vote = active.get();
      long remaining = Math.max(0, vote.expiresAt() - System.currentTimeMillis()) / 1000;
      inv.source().sendMessage(Component.text("==== Active Vote ====", NamedTextColor.GOLD));
      inv.source().sendMessage(Component.text("Target: " + vote.targetName(), NamedTextColor.WHITE));
      inv.source().sendMessage(Component.text("Reason: " + vote.reason(), NamedTextColor.GRAY));
      inv.source().sendMessage(Component.text("Yes: " + vote.yesVotes(), NamedTextColor.GREEN)
          .append(Component.text(" | No: " + vote.noVotes(), NamedTextColor.RED)));
      inv.source().sendMessage(Component.text("Required: " + vote.requiredYes(), NamedTextColor.YELLOW));
      inv.source().sendMessage(Component.text("Time left: " + remaining + "s", NamedTextColor.AQUA));
    }
  }
}
