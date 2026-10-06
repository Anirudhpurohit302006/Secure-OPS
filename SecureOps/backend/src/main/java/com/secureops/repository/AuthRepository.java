package com.secureops.repository;

import static com.secureops.util.Sql.ts;

import com.secureops.model.Models.AuthEvent;
import com.secureops.util.Sql;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** Append-only auth_events. A unique key makes re-reading the same log lines idempotent. */
@Repository
public class AuthRepository {
    private static final RowMapper<AuthEvent> MAPPER = (rs, i) -> new AuthEvent(
            rs.getLong("event_id"), rs.getInt("machine_id"), rs.getString("username"), rs.getString("source_ip"),
            rs.getString("event_type"), ts(rs, "timestamp"));

    private final JdbcTemplate jdbc;

    public AuthRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    /** @return true when the event was new. */
    public boolean insertIgnore(AuthEvent e) {
        return jdbc.update("INSERT INTO auth_events (machine_id, username, source_ip, event_type, timestamp) VALUES (?,?,?,?,?) "
                + "ON CONFLICT ON CONSTRAINT uq_ae_dedupe DO NOTHING",
                e.machineId(), e.username(), e.sourceIp(), e.eventType(), e.timestamp()) > 0;
    }

    public List<AuthEvent> list(List<Integer> machineIds, LocalDateTime since, String typeOrNull, int limit) {
        if (machineIds.isEmpty()) return List.of();
        String type = typeOrNull == null ? "" : " AND event_type = ?";
        Object[] args = typeOrNull == null ? Sql.args(machineIds, since, limit) : Sql.args(machineIds, since, typeOrNull, limit);
        return jdbc.query("SELECT * FROM auth_events WHERE machine_id IN (" + Sql.placeholders(machineIds.size())
                + ") AND timestamp >= ?" + type + " ORDER BY timestamp DESC LIMIT ?", MAPPER, args);
    }

    public int count(List<Integer> machineIds, String type, LocalDateTime since) {
        if (machineIds.isEmpty()) return 0;
        Integer c = jdbc.queryForObject("SELECT COUNT(*) FROM auth_events WHERE machine_id IN ("
                + Sql.placeholders(machineIds.size()) + ") AND event_type = ? AND timestamp >= ?",
                Integer.class, Sql.args(machineIds, type, since));
        return c == null ? 0 : c;
    }
}
