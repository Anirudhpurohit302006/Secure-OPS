# Architecture

```
React (Vite/TypeScript)
   |  HTTP/JSON
Spring Boot backend ---- SSHManager ----SSH----> RHEL-01 192.168.159.129 / RHEL-02 192.168.159.128
   |  JDBC
PostgreSQL (secureops)
```

SecureOps is a **modular monolith**: one Java process, one PostgreSQL database, one React single-page app.

## Layers (backend, package `com.secureops`)
| Package | Responsibility |
|---|---|
| `controller` | HTTP/JSON only: parse request, call a service, return a DTO |
| `service` | Business logic: `TargetService`, `SystemService`, `NetworkService`, `ServiceMonitor`, `SecurityService`, `AnalyticsService`, `DashboardService`, `CollectionService`, `SecurityAssessor` |
| `ssh` | `SshManager` (only place that uses SSH), `ApprovedCommand` (the command allow-list), `RemoteSession` |
| `collector` | `SystemCollector`, `NetworkCollector`, `ServiceCollector`, `AuthenticationCollector`, `SecurityCollector`: run approved commands, parse output |
| `repository` | JDBC / `JdbcTemplate` (prepared statements, PostgreSQL SQL): Machine, Metrics, Network, Service, Auth, Security, Event repositories |
| `model` / `dto` | persistence records / API records (entities are never exposed directly) |
| `scheduler` | `CollectionScheduler`: sequential cycle every 30 min |
| `security` | session interceptor, login throttle |
| `config`, `exception`, `util` | configuration, uniform error responses, validation |

## Collection flow
```
scheduler (30 min, fixedDelay)         or     POST /api/targets/{id}/collect
   -> for each enabled target, one at a time:
        SshManager.connect  -> collectors run ApprovedCommands -> connection closed
        -> one DB transaction appends metrics, ports, services, auth events, security state/events
   -> unreachable target: status OFFLINE, last_seen and history untouched, retried next cycle
```
The browser and Analytics only ever read PostgreSQL; nothing but the scheduler/"Collect now" talks to RHEL.

## Key decisions
- **Append-only history.** Rows are only inserted. Auth events use a unique key so re-reading the log is idempotent.
- **Timestamps are UTC** in the database and API; the browser converts for display. Remote log times are converted using the VM's UTC offset (`date +%z`).
- **CPU** uses two `/proc/stat` samples taken one second apart in a single approved command, so the value reflects "now" rather than the average since boot.
- **Findings** are computed by pure rules (`SecurityAssessor`) from the latest collected state; the current assessment is always live, and each finding is also logged to `security_events` (deduplicated per 24 h) for history.
- **Overall status**: AT_RISK if any HIGH/CRITICAL finding, WARNING if MEDIUM, otherwise SECURE; UNKNOWN before the first collection.
- **Network rate** = byte-counter delta / seconds between collections (counter resets after reboot are skipped).
- **No Spring Security**: single-user session auth with BCrypt (`spring-security-crypto`) is enough for V1.

## Deviations from the specification (and why)
1. Extra tables `listening_ports`, `security_state`, `system_events`, and extra columns on `machines`/`network_metrics`: required by the Definition of Done (ports, SELinux/firewall/SSH state, startup/reboot events, interface status/IP).
2. `POST /api/targets/{id}/collect` ("Collect now"): without it the first data would only appear after the first scheduled cycle.
3. Uptime comes from `cat /proc/uptime` (more robust to parse than `uptime`); boot time is derived from it instead of `who -b`.
4. Failed services come from `systemctl --failed` (no journalctl parsing needed).
5. `{id}` may be `all` on `auth-events` and `security-events` so the Security page can show all systems at once.
6. Java source level 21 (runs on your Java 25 JDK): avoids dependency on Spring Boot's Java 25 support.
7. Database: PostgreSQL 16+ (`TIMESTAMP` columns hold UTC, `GENERATED ALWAYS AS IDENTITY` ids, `JSONB` for sshd settings, `ON CONFLICT DO NOTHING` for idempotent log import). See MIGRATION-POSTGRESQL.md.
8. The existing visualization module was not provided; analytics is native (see `analytics/README.md`).
