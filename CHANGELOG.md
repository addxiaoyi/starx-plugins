# Changelog

## 0.2.0 (2026-07-07)

### New Modules

- **AdminCommandsModule** (`starx.admin`) — Staff management commands: `/report`, `/history`, `/note`, `/notes`, `/announce`, `/bind qq`
- **NapCatModule** (`starx.integrations.napcat`) — QQ Bot integration via NapCat (OneBot v11 WebSocket + HTTP)
- **VoteModule** (`starx.vote`) — Staff voting system with `/votestart`, `/vote`, `/voteinfo`
- **WelcomeModule** (`starx.welcome`) — Customizable join/leave messages
- **SmartQueueModule** (`starx.proxytools.smart-queue`) — Priority-based smart queue system
- **SmartRateLimitModule** (`starx.security.smart-ratelimit`) — Adaptive rate limiting
- **SmartAlertModule** (`starx.security.smart-alert`) — AI-assisted security alerting
- **BlossomGuardModule** (`starx.security.blossom`) — Netty-level IP blacklist via Blossom API

### Database

- 5 new tables: `starx_punishments`, `starx_staff_notes`, `starx_reports`, `starx_announcements` + `starx_announcement_reads`, `starx_player_bindings`
- 5 new JDBC repositories: `JdbcPunishmentRepository`, `JdbcStaffNoteRepository`, `JdbcReportRepository`, `JdbcAnnouncementRepository`, `JdbcBindingRepository`
- New model classes: `Punishment`, `StaffNote`, `Report`, `Announcement`, `PlayerBinding`, `StaffVote`
- Migration from Jdbi/Flyway to plain JDBC with `DatabaseManager.ensureTables()` auto DDL

### HTTP API (Admin)

- `POST /v1/admin/punishments` — record punishment
- `GET /v1/admin/punishments?player=<uuid>` — query punishment history
- `POST /v1/admin/notes` — add staff note
- `GET /v1/admin/notes?player=<uuid>` — query notes
- `POST /v1/admin/reports` — submit report
- `GET /v1/admin/reports?status=PENDING` — list reports
- `PUT /v1/admin/reports/:id/resolve` — resolve/dismiss report
- `POST /v1/admin/announcements` — publish announcement
- `GET /v1/admin/announcements?player=<uuid>` — get unread announcements
- `PUT /v1/admin/announcements/:id/read` — mark as read
- `POST /v1/admin/bindings/verify` — verify binding code

### Plan Integration

- Paper `PlanModule` rewritten: collects TPS, memory, loaded chunks, entities per player; sends via plugin messaging
- Velocity `PlanIntegrationModule` rewritten: subscribes to `PLAN_STATS_REPORT`, stores per-server stats, merges into Plan data points
- `VelocityMessageBridge` routes `CMD_PLAN_STATS` to `PLAN_STATS_REPORT` event

### Configuration

- All module names now use `starx.` prefix (e.g. `starx.auth`, `starx.maintenance`, `starx.integrations.plan`)
- Default config YAML updated with all new modules
- New config sections: `napcat:` (ws-url, http-url, qq-group-id, forward-format)

### LuckPerms Context

- `BindingContextCalculator` — registers `qq-bound` / `discord-bound` contexts via reflection

### Module Renaming

All 36+ Velocity modules renamed with `starx.` prefix for consistency:
| Old | New |
|-----|-----|
| `auth` | `starx.auth` |
| `skin-bridge` | `starx.skin-bridge` |
| `maintenance` | `starx.maintenance` |
| `chat` | `starx.chat` |
| `plan` (Paper) | `starx.integrations.plan` |
| ... | `starx.*` for all |

### Build & Tech

- Removed Jdbi/Flyway dependencies — all database via HikariCP + raw JDBC
- ShadowJar minimized (45% smaller Paper jar, 13% smaller Velocity jar)
- All 200+ Velocity tests passing + all Paper tests passing
