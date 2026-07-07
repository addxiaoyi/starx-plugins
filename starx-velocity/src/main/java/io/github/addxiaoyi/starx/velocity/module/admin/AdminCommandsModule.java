package io.github.addxiaoyi.starx.velocity.module.admin;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.addxiaoyi.starx.common.auth.BindingVerificationService;
import io.github.addxiaoyi.starx.common.database.JdbcAnnouncementRepository;
import io.github.addxiaoyi.starx.common.database.JdbcBindingRepository;
import io.github.addxiaoyi.starx.common.database.JdbcPunishmentRepository;
import io.github.addxiaoyi.starx.common.database.JdbcReportRepository;
import io.github.addxiaoyi.starx.common.database.JdbcStaffNoteRepository;
import io.github.addxiaoyi.starx.common.database.JdbcUserRepository;
import io.github.addxiaoyi.starx.common.model.Announcement;
import io.github.addxiaoyi.starx.common.model.PlayerBinding;
import io.github.addxiaoyi.starx.common.model.Punishment;
import io.github.addxiaoyi.starx.common.model.Report;
import io.github.addxiaoyi.starx.common.model.StaffNote;
import io.github.addxiaoyi.starx.velocity.StarxVelocityPlugin;
import io.github.addxiaoyi.starx.velocity.module.VelocityModule;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class AdminCommandsModule implements VelocityModule {

  private static final List<String> REPORT_CATEGORIES =
      List.of("CHEATING", "CHAT_ABUSE", "SPAM", "NAME", "OTHER");
  private static final List<String> NOTE_SEVERITIES = List.of("INFO", "WARNING", "CRITICAL");

  private final StarxVelocityPlugin plugin;
  private final JdbcUserRepository userRepo;
  private final JdbcPunishmentRepository punishmentRepo;
  private final JdbcStaffNoteRepository staffNoteRepo;
  private final JdbcReportRepository reportRepo;
  private final JdbcAnnouncementRepository announcementRepo;
  private final JdbcBindingRepository bindingRepo;
  private final BindingVerificationService bindingVerification;

  public AdminCommandsModule(
      StarxVelocityPlugin plugin,
      JdbcUserRepository userRepo,
      JdbcPunishmentRepository punishmentRepo,
      JdbcStaffNoteRepository staffNoteRepo,
      JdbcReportRepository reportRepo,
      JdbcAnnouncementRepository announcementRepo,
      JdbcBindingRepository bindingRepo,
      BindingVerificationService bindingVerification) {
    this.plugin = plugin;
    this.userRepo = userRepo;
    this.punishmentRepo = punishmentRepo;
    this.staffNoteRepo = staffNoteRepo;
    this.reportRepo = reportRepo;
    this.announcementRepo = announcementRepo;
    this.bindingRepo = bindingRepo;
    this.bindingVerification = bindingVerification;
  }

  @Override
  public String name() {
    return "starx.admin";
  }

  @Override
  public void onEnable() {
    ProxyServer proxy = plugin.proxy();
    var reg = proxy.getCommandManager();
    reg.register(reg.metaBuilder("report").build(), new ReportCommand());
    reg.register(reg.metaBuilder("history").build(), new HistoryCommand());
    reg.register(reg.metaBuilder("note").build(), new NoteCommand());
    reg.register(reg.metaBuilder("notes").build(), new NotesCommand());
    reg.register(reg.metaBuilder("announce").build(), new AnnounceCommand());
    reg.register(reg.metaBuilder("bind").build(), new BindCommand());
  }

  @Override
  public void onDisable() {}

  // ---- /report <player> <category> ----

  private final class ReportCommand implements SimpleCommand {
    @Override
    public void execute(Invocation inv) {
      if (!inv.source().hasPermission("starx.report")) {
        inv.source().sendMessage(Component.text("No permission.", NamedTextColor.RED));
        return;
      }
      if (!(inv.source() instanceof Player reporter)) {
        inv.source().sendMessage(Component.text("Players only.", NamedTextColor.RED));
        return;
      }
      String[] args = inv.arguments();
      if (args.length < 2) {
        inv.source()
            .sendMessage(
                Component.text(
                    "Usage: /report <player> <category> ["
                        + String.join("/", REPORT_CATEGORIES)
                        + "]",
                    NamedTextColor.YELLOW));
        return;
      }
      String targetName = args[0];
      String category = args[1].toUpperCase();
      if (!REPORT_CATEGORIES.contains(category)) {
        inv.source()
            .sendMessage(
                Component.text(
                    "Invalid category. Valid: " + String.join(", ", REPORT_CATEGORIES),
                    NamedTextColor.RED));
        return;
      }
      String details =
          args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "";

      Optional<Player> target = plugin.proxy().getPlayer(targetName);
      if (target.isEmpty()) {
        inv.source().sendMessage(Component.text("Player not found.", NamedTextColor.RED));
        return;
      }

      Report r =
          new Report(
              UUID.randomUUID().toString(),
              reporter.getUniqueId(),
              target.get().getUniqueId(),
              category,
              details,
              "PENDING",
              null,
              null);
      reportRepo.create(r);
      inv.source().sendMessage(Component.text("Report submitted.", NamedTextColor.GREEN));
    }

    @Override
    public List<String> suggest(Invocation inv) {
      String[] args = inv.arguments();
      if (args.length <= 1) {
        String prefix = args.length == 0 ? "" : args[0].toLowerCase();
        return plugin.proxy().getAllPlayers().stream()
            .map(Player::getUsername)
            .filter(n -> n.toLowerCase().startsWith(prefix))
            .collect(Collectors.toList());
      }
      if (args.length == 2) {
        String prefix = args[1].toLowerCase();
        return REPORT_CATEGORIES.stream()
            .filter(c -> c.toLowerCase().startsWith(prefix))
            .collect(Collectors.toList());
      }
      return List.of();
    }
  }

  // ---- /history <player> ----

  private final class HistoryCommand implements SimpleCommand {
    @Override
    public void execute(Invocation inv) {
      if (!inv.source().hasPermission("starx.history")) {
        inv.source().sendMessage(Component.text("No permission.", NamedTextColor.RED));
        return;
      }
      String[] args = inv.arguments();
      if (args.length == 0) {
        inv.source().sendMessage(Component.text("Usage: /history <player>", NamedTextColor.YELLOW));
        return;
      }
      String targetName = args[0];
      Optional<Player> target = plugin.proxy().getPlayer(targetName);
      if (target.isEmpty()) {
        inv.source().sendMessage(Component.text("Player not found.", NamedTextColor.RED));
        return;
      }
      UUID uuid = target.get().getUniqueId();

      inv.source()
          .sendMessage(
              Component.text("==== History: " + targetName + " ====", NamedTextColor.GOLD));

      List<Punishment> punishments = punishmentRepo.findByPlayer(uuid);
      inv.source()
          .sendMessage(
              Component.text("Punishments (" + punishments.size() + "):", NamedTextColor.AQUA));
      for (Punishment p : punishments) {
        inv.source()
            .sendMessage(
                Component.text(
                    "  [" + p.type() + "] " + p.reason() + " - by " + p.staffName(),
                    NamedTextColor.GRAY));
      }

      List<StaffNote> notes = staffNoteRepo.findByPlayer(uuid);
      if (!notes.isEmpty()) {
        inv.source().sendMessage(Component.text("Staff Notes:", NamedTextColor.AQUA));
        for (StaffNote n : notes) {
          inv.source()
              .sendMessage(
                  Component.text("  [" + n.severity() + "] " + n.note(), NamedTextColor.GRAY));
        }
      }

      List<Report> reports = reportRepo.findByTarget(uuid);
      if (!reports.isEmpty()) {
        inv.source()
            .sendMessage(Component.text("Reports (" + reports.size() + "):", NamedTextColor.AQUA));
        for (Report r : reports) {
          inv.source()
              .sendMessage(
                  Component.text(
                      "  [" + r.status() + "] " + r.category() + " - " + r.details(),
                      NamedTextColor.GRAY));
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

  // ---- /note <player> <content> [-s <severity>] ----

  private final class NoteCommand implements SimpleCommand {
    @Override
    public void execute(Invocation inv) {
      if (!inv.source().hasPermission("starx.note")) {
        inv.source().sendMessage(Component.text("No permission.", NamedTextColor.RED));
        return;
      }
      if (!(inv.source() instanceof Player staff)) {
        inv.source().sendMessage(Component.text("Players only.", NamedTextColor.RED));
        return;
      }
      String[] args = inv.arguments();
      if (args.length < 2) {
        inv.source()
            .sendMessage(
                Component.text(
                    "Usage: /note <player> <content> [-s INFO|WARNING|CRITICAL]",
                    NamedTextColor.YELLOW));
        return;
      }
      String targetName = args[0];
      String severity = "INFO";
      int contentEnd = args.length;

      if (args.length >= 4 && "-s".equalsIgnoreCase(args[args.length - 2])) {
        severity = args[args.length - 1].toUpperCase();
        if (!NOTE_SEVERITIES.contains(severity)) {
          inv.source()
              .sendMessage(Component.text("Invalid severity: " + severity, NamedTextColor.RED));
          return;
        }
        contentEnd = args.length - 2;
      }
      String content = String.join(" ", Arrays.copyOfRange(args, 1, contentEnd));

      Optional<Player> target = plugin.proxy().getPlayer(targetName);
      if (target.isEmpty()) {
        inv.source().sendMessage(Component.text("Player not found.", NamedTextColor.RED));
        return;
      }

      StaffNote note =
          new StaffNote(
              UUID.randomUUID().toString(),
              target.get().getUniqueId(),
              content,
              severity,
              staff.getUniqueId(),
              System.currentTimeMillis());
      staffNoteRepo.addNote(note);
      inv.source().sendMessage(Component.text("Note added.", NamedTextColor.GREEN));
    }

    @Override
    public List<String> suggest(Invocation inv) {
      String[] args = inv.arguments();
      if (args.length <= 1) {
        String prefix = args.length == 0 ? "" : args[0].toLowerCase();
        return plugin.proxy().getAllPlayers().stream()
            .map(Player::getUsername)
            .filter(n -> n.toLowerCase().startsWith(prefix))
            .collect(Collectors.toList());
      }
      if (args.length >= 2 && "-s".equalsIgnoreCase(args[args.length - 1])) {
        return NOTE_SEVERITIES;
      }
      return List.of();
    }
  }

  // ---- /notes <player> ----

  private final class NotesCommand implements SimpleCommand {
    @Override
    public void execute(Invocation inv) {
      if (!inv.source().hasPermission("starx.note.list")) {
        inv.source().sendMessage(Component.text("No permission.", NamedTextColor.RED));
        return;
      }
      String[] args = inv.arguments();
      if (args.length == 0) {
        inv.source().sendMessage(Component.text("Usage: /notes <player>", NamedTextColor.YELLOW));
        return;
      }
      String targetName = args[0];
      Optional<Player> target = plugin.proxy().getPlayer(targetName);
      if (target.isEmpty()) {
        inv.source().sendMessage(Component.text("Player not found.", NamedTextColor.RED));
        return;
      }
      List<StaffNote> notes = staffNoteRepo.findByPlayer(target.get().getUniqueId());
      if (notes.isEmpty()) {
        inv.source()
            .sendMessage(Component.text("No notes for " + targetName + ".", NamedTextColor.GRAY));
        return;
      }
      inv.source()
          .sendMessage(Component.text("Notes for " + targetName + ":", NamedTextColor.GOLD));
      for (StaffNote n : notes) {
        inv.source()
            .sendMessage(
                Component.text("  [" + n.severity() + "] " + n.note(), NamedTextColor.GRAY));
      }
    }

    @Override
    public List<String> suggest(Invocation inv) {
      String prefix = inv.arguments().length == 0 ? "" : inv.arguments()[0].toLowerCase();
      return plugin.proxy().getAllPlayers().stream()
          .map(Player::getUsername)
          .filter(n -> n.toLowerCase().startsWith(prefix))
          .collect(Collectors.toList());
    }
  }

  // ---- /announce <title> <content> ----

  private final class AnnounceCommand implements SimpleCommand {
    @Override
    public void execute(Invocation inv) {
      if (!inv.source().hasPermission("starx.announce")) {
        inv.source().sendMessage(Component.text("No permission.", NamedTextColor.RED));
        return;
      }
      String[] args = inv.arguments();
      if (args.length < 2) {
        inv.source()
            .sendMessage(
                Component.text("Usage: /announce <title> <content>", NamedTextColor.YELLOW));
        return;
      }
      String title = args[0];
      String content = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

      Announcement a =
          new Announcement(
              UUID.randomUUID().toString(),
              title,
              content,
              inv.source() instanceof Player p ? p.getUniqueId().toString() : "console",
              System.currentTimeMillis(),
              null);
      announcementRepo.create(a);

      Component msg =
          Component.text("[" + title + "] ", NamedTextColor.GOLD)
              .append(Component.text(content, NamedTextColor.WHITE));
      for (Player player : plugin.proxy().getAllPlayers()) {
        player.sendMessage(msg);
      }
      inv.source().sendMessage(Component.text("Announcement sent.", NamedTextColor.GREEN));
    }
  }

  // ---- /bind qq ----

  private final class BindCommand implements SimpleCommand {
    @Override
    public void execute(Invocation inv) {
      if (!(inv.source() instanceof Player player)) {
        inv.source().sendMessage(Component.text("Players only.", NamedTextColor.RED));
        return;
      }
      String[] args = inv.arguments();
      if (args.length < 1 || !"qq".equalsIgnoreCase(args[0])) {
        inv.source().sendMessage(Component.text("Usage: /bind qq", NamedTextColor.YELLOW));
        return;
      }

      UUID uuid = player.getUniqueId();
      Optional<PlayerBinding> existing = bindingRepo.findByPlayer(uuid);
      if (existing.isPresent() && existing.get().qqId() != null) {
        inv.source()
            .sendMessage(
                Component.text(
                    "Your account is already bound to a QQ account.", NamedTextColor.RED));
        return;
      }

      String code = bindingVerification.generateCode(uuid);

      inv.source()
          .sendMessage(
              Component.text("")
                  .append(
                      Component.text(
                          "\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500",
                          NamedTextColor.DARK_GRAY))
                  .append(Component.text("\n\u2605 Your verification code: ", NamedTextColor.GREEN))
                  .append(Component.text(code, NamedTextColor.AQUA))
                  .append(Component.text(" \u2605", NamedTextColor.GREEN))
                  .append(
                      Component.text(
                          "\nSend this code to the QQ bot via private message",
                          NamedTextColor.GRAY))
                  .append(
                      Component.text(
                          "\nto bind your QQ account to " + player.getUsername() + ".",
                          NamedTextColor.GRAY))
                  .append(Component.text("\nCode expires in 5 minutes.", NamedTextColor.RED))
                  .append(
                      Component.text(
                          "\n\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500",
                          NamedTextColor.DARK_GRAY)));
    }

    @Override
    public List<String> suggest(Invocation inv) {
      if (inv.arguments().length <= 1) {
        String prefix = inv.arguments().length == 0 ? "" : inv.arguments()[0].toLowerCase();
        return List.of("qq").stream()
            .filter(s -> s.startsWith(prefix))
            .collect(Collectors.toList());
      }
      return List.of();
    }
  }
}
