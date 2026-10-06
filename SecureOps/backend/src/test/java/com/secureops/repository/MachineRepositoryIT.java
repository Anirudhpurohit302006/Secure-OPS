package com.secureops.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secureops.model.Models.AuthEvent;
import com.secureops.model.Models.SecurityState;
import com.secureops.model.Models.SystemMetric;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * Integration test against a REAL PostgreSQL with database/schema.sql already applied.
 * Skipped unless configured (see tests/README.md):
 *   SECUREOPS_IT_DB_URL=jdbc:postgresql://localhost:5432/secureops_test
 *   SECUREOPS_IT_DB_USER=...   SECUREOPS_IT_DB_PASSWORD=...
 * Every test creates its own machine and deletes it afterwards (cascade removes the history).
 */
@EnabledIfEnvironmentVariable(named = "SECUREOPS_IT_DB_URL", matches = ".+")
class MachineRepositoryIT {
    private static final LocalDateTime T0 = LocalDateTime.of(2026, 10, 4, 12, 0);

    private JdbcTemplate jdbc;
    private MachineRepository machines;
    private MetricsRepository metrics;
    private AuthRepository auth;
    private SecurityRepository security;
    private int machineId;

    @BeforeEach
    void setUp() {
        var ds = new DriverManagerDataSource(System.getenv("SECUREOPS_IT_DB_URL"),
                System.getenv("SECUREOPS_IT_DB_USER"), System.getenv("SECUREOPS_IT_DB_PASSWORD"));
        jdbc = new JdbcTemplate(ds);
        machines = new MachineRepository(jdbc);
        metrics = new MetricsRepository(jdbc);
        auth = new AuthRepository(jdbc);
        security = new SecurityRepository(jdbc, new ObjectMapper());
        machineId = machines.insert("IT-" + System.nanoTime(), "10.9.9.9", "ani", 22, true);
    }

    @AfterEach
    void tearDown() { machines.delete(machineId); }

    @Test
    void insertReturnsTheGeneratedId() {
        assertThat(machineId).isPositive();
        assertThat(machines.findById(machineId)).isPresent();
    }

    @Test
    void lastSeenIsPreservedWhenMachineGoesOffline() {
        machines.updateStatus(machineId, "ONLINE", T0);
        machines.updateStatus(machineId, "OFFLINE", null);
        var m = machines.findById(machineId).orElseThrow();
        assertThat(m.status()).isEqualTo("OFFLINE");
        assertThat(m.lastSeen()).isEqualTo(T0);
    }

    @Test
    void metricsAreAppendOnlyAndLatestIsReturned() {
        metrics.insert(new SystemMetric(0, machineId, T0, 10.0, 20.0, 30.0, 100L));
        metrics.insert(new SystemMetric(0, machineId, T0.plusMinutes(30), 15.0, 25.0, 30.0, 1900L));
        assertThat(metrics.series(List.of(machineId), T0.minusHours(1))).hasSize(2);
        assertThat(metrics.latest(machineId).orElseThrow().cpuUsage()).isEqualTo(15.0);
    }

    @Test
    void authEventsAreDeduplicated() {
        var e = new AuthEvent(0, machineId, "bob", "10.0.0.5", "SSH_LOGIN_FAILED", T0);
        assertThat(auth.insertIgnore(e)).isTrue();
        assertThat(auth.insertIgnore(e)).isFalse();      // same log line read again
        assertThat(auth.list(List.of(machineId), T0.minusDays(1), null, 10)).hasSize(1);
    }

    @Test
    void securityStateRoundTripsJsonbAndUnreadableConfig() {
        security.insertState(new SecurityState(machineId, true, "ENFORCING", true, 22, Map.of("PermitRootLogin", "no"), T0));
        assertThat(security.latestState(machineId).orElseThrow().sshConfig()).containsEntry("PermitRootLogin", "no");
        security.insertState(new SecurityState(machineId, false, "UNKNOWN", true, 22, null, T0.plusMinutes(30)));
        assertThat(security.latestState(machineId).orElseThrow().sshConfig()).isNull();   // NULL = could not be read
    }

    @Test
    void deletingATargetCascadesToItsHistory() {
        metrics.insert(new SystemMetric(0, machineId, T0, 1.0, 1.0, 1.0, 1L));
        machines.delete(machineId);
        assertThat(metrics.latest(machineId)).isEmpty();
        machineId = machines.insert("IT-" + System.nanoTime(), "10.9.9.9", "ani", 22, true);   // keep tearDown valid
    }
}
