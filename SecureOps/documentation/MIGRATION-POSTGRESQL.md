# Migration history: MySQL -> PostgreSQL (migration history; MySQL is no longer used)

SecureOps V1 originally targeted MySQL 8. MySQL cannot run on the development machine, so the project now uses **PostgreSQL 16+ only**.
No dual-database support was kept. The repository layer (`JdbcTemplate`) was not rewritten; only SQL and configuration changed.

| Area | Before | After |
|---|---|---|
| JDBC driver | `com.mysql:mysql-connector-j` | `org.postgresql:postgresql` (version managed by Spring Boot) |
| URL | `jdbc:mysql://host:3306/db?...` | `jdbc:postgresql://host:5432/db` (from `DB_HOST`, `DB_PORT`, `DB_NAME`) |
| Default DB user | `secureops_app` | `secureops` |
| IDs | `AUTO_INCREMENT` | `GENERATED ALWAYS AS IDENTITY` |
| Generated key on insert | `getGeneratedKeys()` (PostgreSQL returns all columns, so `getKey()` would fail) | `INSERT ... RETURNING machine_id` |
| Types | `DATETIME`, `ENUM`, `DECIMAL(5,2)`, `BIGINT UNSIGNED`, `TEXT` JSON | `TIMESTAMP` (UTC), `VARCHAR` + `CHECK`, `NUMERIC(5,2)`, `BIGINT` + `CHECK >= 0`, `JSONB` |
| Engine / quoting | `ENGINE=InnoDB`, backticks | removed; `"timestamp"` quoted in DDL, unquoted in queries |
| Idempotent import of auth events | `INSERT IGNORE` | `INSERT ... ON CONFLICT ON CONSTRAINT uq_ae_dedupe DO NOTHING` |
| Conditional in event feed | `IF(cond, a, b)` | `CASE WHEN ... END` |
| last_seen preservation | `COALESCE(?, last_seen)` | `COALESCE(CAST(? AS TIMESTAMP), last_seen)` (typed NULL) |
| Schema file | contained `CREATE DATABASE` | database created by the setup script (connects to `postgres` first); `schema.sql` is idempotent |
| Setup | `mysql` client | `scripts\setup-database.ps1` using `psql` |
| Integration test | MySQL URL | PostgreSQL URL, plus tests for dedupe, JSONB and ids |

Unchanged: tables, columns, relationships, append-only history, REST API, React UI, collectors, SSH architecture, scheduler, RHEL commands and `scripts/rhel-setup.sh`.
Port 3306 still appears in `SecurityAssessor` only as a *risky listening port on a monitored RHEL host* (a MySQL server exposed there is a finding); it is not a dependency.
