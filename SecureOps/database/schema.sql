-- SecureOps V1 schema - PostgreSQL 16+ (works on 13+).
-- Run against the `secureops` database (scripts\setup-database.ps1 creates it first). Idempotent.
-- History tables are append-only. All timestamps are UTC, stored as TIMESTAMP (without time zone).

CREATE TABLE IF NOT EXISTS machines (
  machine_id      INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  machine_name    VARCHAR(64)  NOT NULL UNIQUE,
  hostname        VARCHAR(255),
  ip_address      VARCHAR(45)  NOT NULL,
  ssh_username    VARCHAR(64)  NOT NULL,
  ssh_port        INTEGER      NOT NULL DEFAULT 22 CHECK (ssh_port BETWEEN 1 AND 65535),
  enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
  status          VARCHAR(8)   NOT NULL DEFAULT 'UNKNOWN' CHECK (status IN ('ONLINE','OFFLINE','UNKNOWN')),
  last_seen       TIMESTAMP,
  os_name         VARCHAR(128),
  os_version      VARCHAR(64),
  kernel_version  VARCHAR(64),
  machine_uid     VARCHAR(64),
  created_at      TIMESTAMP    NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC')
);

CREATE TABLE IF NOT EXISTS system_metrics (
  metric_id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  machine_id    INTEGER   NOT NULL REFERENCES machines(machine_id) ON DELETE CASCADE,
  "timestamp"   TIMESTAMP NOT NULL,
  cpu_usage     NUMERIC(5,2),
  memory_usage  NUMERIC(5,2),
  disk_usage    NUMERIC(5,2),
  uptime        BIGINT
);
CREATE INDEX IF NOT EXISTS idx_sm_machine_time ON system_metrics (machine_id, "timestamp");

CREATE TABLE IF NOT EXISTS network_metrics (
  network_id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  machine_id   INTEGER      NOT NULL REFERENCES machines(machine_id) ON DELETE CASCADE,
  interface    VARCHAR(32)  NOT NULL,
  rx_bytes     BIGINT       NOT NULL CHECK (rx_bytes >= 0),   -- BIGINT (signed) holds up to ~9.2 EB; PostgreSQL has no UNSIGNED
  tx_bytes     BIGINT       NOT NULL CHECK (tx_bytes >= 0),
  state        VARCHAR(16),
  ip_address   VARCHAR(255),
  "timestamp"  TIMESTAMP    NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_nm_machine_time ON network_metrics (machine_id, "timestamp");

CREATE TABLE IF NOT EXISTS service_status (
  service_id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  machine_id    INTEGER     NOT NULL REFERENCES machines(machine_id) ON DELETE CASCADE,
  service_name  VARCHAR(64) NOT NULL,
  status        VARCHAR(8)  NOT NULL CHECK (status IN ('RUNNING','STOPPED','FAILED','UNKNOWN')),
  "timestamp"   TIMESTAMP   NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_ss_machine_time ON service_status (machine_id, "timestamp");

CREATE TABLE IF NOT EXISTS auth_events (
  event_id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  machine_id  INTEGER     NOT NULL REFERENCES machines(machine_id) ON DELETE CASCADE,
  username    VARCHAR(64) NOT NULL,
  source_ip   VARCHAR(45) NOT NULL,
  event_type  VARCHAR(20) NOT NULL CHECK (event_type IN ('SSH_LOGIN_SUCCESS','SSH_LOGIN_FAILED')),
  "timestamp" TIMESTAMP   NOT NULL,
  -- makes re-reading the same log lines idempotent (INSERT ... ON CONFLICT DO NOTHING)
  CONSTRAINT uq_ae_dedupe UNIQUE (machine_id, event_type, username, source_ip, "timestamp")
);
CREATE INDEX IF NOT EXISTS idx_ae_machine_time ON auth_events (machine_id, "timestamp");

CREATE TABLE IF NOT EXISTS security_events (
  event_id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  machine_id   INTEGER      NOT NULL REFERENCES machines(machine_id) ON DELETE CASCADE,
  event_type   VARCHAR(64)  NOT NULL,
  severity     VARCHAR(8)   NOT NULL CHECK (severity IN ('INFO','LOW','MEDIUM','HIGH','CRITICAL')),
  description  VARCHAR(512) NOT NULL,
  "timestamp"  TIMESTAMP    NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_se_machine_time ON security_events (machine_id, "timestamp");

CREATE TABLE IF NOT EXISTS listening_ports (
  port_id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  machine_id   INTEGER     NOT NULL REFERENCES machines(machine_id) ON DELETE CASCADE,
  protocol     VARCHAR(8)  NOT NULL,
  port         INTEGER     NOT NULL CHECK (port BETWEEN 0 AND 65535),
  bind_address VARCHAR(64),
  state        VARCHAR(16) NOT NULL,
  "timestamp"  TIMESTAMP   NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_lp_machine_time ON listening_ports (machine_id, "timestamp");

CREATE TABLE IF NOT EXISTS security_state (
  state_id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  machine_id       INTEGER    NOT NULL REFERENCES machines(machine_id) ON DELETE CASCADE,
  firewall_active  BOOLEAN,
  selinux_mode     VARCHAR(12) NOT NULL DEFAULT 'UNKNOWN' CHECK (selinux_mode IN ('ENFORCING','PERMISSIVE','DISABLED','UNKNOWN')),
  ssh_active       BOOLEAN,
  ssh_port         INTEGER,
  ssh_config       JSONB,                 -- approved sshd_config settings; NULL = could not be read
  "timestamp"      TIMESTAMP  NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_st_machine_time ON security_state (machine_id, "timestamp");

CREATE TABLE IF NOT EXISTS system_events (
  event_id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  machine_id   INTEGER      NOT NULL REFERENCES machines(machine_id) ON DELETE CASCADE,
  event_type   VARCHAR(20)  NOT NULL CHECK (event_type IN ('STARTUP','SHUTDOWN','REBOOT','SERVICE_FAILURE','SERVICE_START','SERVICE_STOP')),
  description  VARCHAR(512),
  severity     VARCHAR(8)   NOT NULL DEFAULT 'INFO' CHECK (severity IN ('INFO','LOW','MEDIUM','HIGH','CRITICAL')),
  "timestamp"  TIMESTAMP    NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_sye_machine_time ON system_events (machine_id, "timestamp");
