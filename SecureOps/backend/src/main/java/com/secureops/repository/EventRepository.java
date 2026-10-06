package com.secureops.repository;

import static com.secureops.util.Sql.ts;

import com.secureops.dto.Dtos.EventItem;
import com.secureops.model.Models.SystemEvent;
import com.secureops.util.Sql;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** system_events (startup/shutdown/reboot/service events) plus the unified event feed. */
@Repository
public class EventRepository {
    private final JdbcTemplate jdbc;

    public EventRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void insert(SystemEvent e) {
        jdbc.update("INSERT INTO system_events (machine_id, event_type, description, severity, timestamp) VALUES (?,?,?,?,?)",
                e.machineId(), e.eventType(), e.description(), e.severity(), e.timestamp());
    }

    public boolean existsRecent(int machineId, String type, String description, LocalDateTime since) {
        Integer c = jdbc.queryForObject("SELECT COUNT(*) FROM system_events WHERE machine_id = ? AND event_type = ? "
                + "AND description = ? AND timestamp >= ?", Integer.class, machineId, type, description, since);
        return c != null && c > 0;
    }

    /** Auth + security + system events merged, newest first. machineName is filled in by the service layer. */
    public List<EventItem> feed(List<Integer> machineIds, int limit) {
        if (machineIds.isEmpty()) return List.of();
        String in = Sql.placeholders(machineIds.size());
        String sql = "SELECT * FROM ("
                + " SELECT CONCAT('auth-', event_id) AS id, 'AUTH' AS source, machine_id, event_type AS type,"
                + "  CASE WHEN event_type = 'SSH_LOGIN_FAILED' THEN 'LOW' ELSE 'INFO' END AS severity,"
                + "  CONCAT(CASE WHEN event_type = 'SSH_LOGIN_FAILED' THEN 'Failed SSH login for ' ELSE 'Successful SSH login for ' END, username, ' from ', source_ip) AS description,"
                + "  timestamp AS ts FROM auth_events WHERE machine_id IN (" + in + ")"
                + " UNION ALL"
                + " SELECT CONCAT('sec-', event_id), 'SECURITY', machine_id, event_type, severity, description, timestamp"
                + "  FROM security_events WHERE machine_id IN (" + in + ")"
                + " UNION ALL"
                + " SELECT CONCAT('sys-', event_id), 'SYSTEM', machine_id, event_type, severity, COALESCE(description, event_type), timestamp"
                + "  FROM system_events WHERE machine_id IN (" + in + ")"
                + ") e ORDER BY ts DESC LIMIT ?";
        List<Object> args = new ArrayList<>();
        for (int k = 0; k < 3; k++) args.addAll(machineIds);
        args.add(limit);
        return jdbc.query(sql, (rs, i) -> new EventItem(rs.getString("id"), rs.getString("source"), rs.getInt("machine_id"),
                null, rs.getString("type"), rs.getString("severity"), rs.getString("description"), ts(rs, "ts")), args.toArray());
    }
}
