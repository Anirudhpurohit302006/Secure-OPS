package com.secureops.repository;

import static com.secureops.util.Sql.bool;
import static com.secureops.util.Sql.integer;
import static com.secureops.util.Sql.ts;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.secureops.model.Models.SecurityEvent;
import com.secureops.model.Models.SecurityState;
import com.secureops.util.Sql;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** Append-only security_events and security_state. */
@Repository
public class SecurityRepository {
    private static final RowMapper<SecurityEvent> EVENT = (rs, i) -> new SecurityEvent(
            rs.getLong("event_id"), rs.getInt("machine_id"), rs.getString("event_type"), rs.getString("severity"),
            rs.getString("description"), ts(rs, "timestamp"));

    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public SecurityRepository(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public void insertEvent(int machineId, String type, String severity, String description, LocalDateTime ts) {
        jdbc.update("INSERT INTO security_events (machine_id, event_type, severity, description, timestamp) VALUES (?,?,?,?,?)",
                machineId, type, severity, trim(description), ts);
    }

    /** Inserts unless an identical (type, description) event already exists since the given time. */
    public boolean insertEventIfNew(int machineId, String type, String severity, String description,
                                    LocalDateTime ts, LocalDateTime since) {
        Integer c = jdbc.queryForObject("SELECT COUNT(*) FROM security_events WHERE machine_id = ? AND event_type = ? "
                + "AND description = ? AND timestamp >= ?", Integer.class, machineId, type, trim(description), since);
        if (c != null && c > 0) return false;
        insertEvent(machineId, type, severity, description, ts);
        return true;
    }

    public List<SecurityEvent> events(List<Integer> machineIds, LocalDateTime since, int limit) {
        if (machineIds.isEmpty()) return List.of();
        return jdbc.query("SELECT * FROM security_events WHERE machine_id IN (" + Sql.placeholders(machineIds.size())
                + ") AND timestamp >= ? ORDER BY timestamp DESC, event_id DESC LIMIT ?", EVENT, Sql.args(machineIds, since, limit));
    }

    public void insertState(SecurityState s) {
        String cfg = null;
        try {
            if (s.sshConfig() != null) cfg = json.writeValueAsString(s.sshConfig());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
        jdbc.update("INSERT INTO security_state (machine_id, firewall_active, selinux_mode, ssh_active, ssh_port, ssh_config, timestamp) VALUES (?,?,?,?,?,CAST(? AS JSONB),?)",
                s.machineId(), s.firewallActive(), s.selinuxMode(), s.sshActive(), s.sshPort(), cfg, s.timestamp());
    }

    public Optional<SecurityState> latestState(int machineId) {
        return jdbc.query("SELECT * FROM security_state WHERE machine_id = ? ORDER BY timestamp DESC, state_id DESC LIMIT 1",
                (rs, i) -> {
                    Map<String, String> cfg = null;
                    String raw = rs.getString("ssh_config");
                    if (raw != null) {
                        try {
                            cfg = json.readValue(raw, new TypeReference<Map<String, String>>() {});
                        } catch (JsonProcessingException e) {
                            cfg = null;
                        }
                    }
                    return new SecurityState(rs.getInt("machine_id"), bool(rs, "firewall_active"),
                            rs.getString("selinux_mode"), bool(rs, "ssh_active"), integer(rs, "ssh_port"), cfg,
                            ts(rs, "timestamp"));
                }, machineId).stream().findFirst();
    }

    private static String trim(String s) { return s.length() > 500 ? s.substring(0, 500) : s; }
}
