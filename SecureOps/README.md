# SecureOps

RHEL infrastructure, security and analytics console (Version 1.0).
Monitors authorized RHEL virtual machines from a Windows host over SSH and stores history in PostgreSQL.

```
Browser -> React/Vite/TypeScript -> Java (Spring Boot) backend -> PostgreSQL
                                          |
                                          +-- SSHManager --SSH--> RHEL-01 (192.168.159.129)
                                                           \--SSH--> RHEL-02 (192.168.159.128)
```

## Features (V1)
Login/logout with server-side sessions · target management with connection test · CPU, RAM, disk, uptime, network,
listening ports · SSH/firewalld/HTTPD status · SELinux, firewall and SSH-hardening assessment with severity-rated findings ·
SSH login history (success/failed) · startup/reboot/shutdown/service-failure events · 30-minute scheduled collection ·
historical analytics with target and time filters.
Not in V1: reports, machine comparison, alert engine, RBAC (see the specification's future versions).

## Quick start (Windows)
Full instructions: **documentation/SETUP.md**. Short version:

1. Install Java 21+ (25 works), Maven, Node 20+, **PostgreSQL 16+**, Git.
2. `scripts\setup-database.ps1` - creates the PostgreSQL role and database `secureops`, applies the schema and seed.
3. `copy config\secureops.example.env config\secureops.env`, edit it, run `scripts\hash-password.ps1` for the admin hash.
4. On each RHEL VM: set up SSH key login and run `scripts/rhel-setup.sh <user>` (see SETUP.md).
5. `scripts\run-backend.ps1` and, in a second terminal, `scripts\run-frontend.ps1`.
6. Open http://localhost:5173, sign in, open **Settings**, edit RHEL-01/RHEL-02 targets, press **Test Connection** then **Collect now**.

## Documentation
| File | Content |
|------|---------|
| documentation/ARCHITECTURE.md | layers, packages, data flow, design decisions |
| documentation/SETUP.md | prerequisites, Windows, PostgreSQL, RHEL, SSH, configuration, running, testing, troubleshooting |
| documentation/COLLECTORS.md | every RHEL command SecureOps runs and how it is parsed |
| documentation/API.md | REST endpoints |
| documentation/SECURITY.md | security model and considerations |
| documentation/MIGRATION-POSTGRESQL.md | migration history (database change) |

## Project structure
```
backend/        Java 21 bytecode (runs on Java 25) - controller, service, ssh, collector, repository, scheduler, ...
frontend/       React + Vite + TS - pages, components, charts, hooks, services, styles
database/       schema.sql, seed.sql (PostgreSQL), create-user.sql.example
analytics/      integration notes for the visualization module
config/         *.example files only (real secrets stay local)
scripts/        PowerShell helpers + rhel-setup.sh
tests/          notes on running tests (tests themselves sit beside the code)
documentation/
```

## Tests
`cd backend && mvn test` · `cd frontend && npm test`
