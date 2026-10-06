package com.secureops.repository;

import static com.secureops.util.Sql.dbl;
import static com.secureops.util.Sql.lng;
import static com.secureops.util.Sql.ts;

import com.secureops.model.Models.SystemMetric;
import com.secureops.util.Sql;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** Append-only system_metrics. */
@Repository
public class MetricsRepository {
    private static final RowMapper<SystemMetric> MAPPER = (rs, i) -> new SystemMetric(
            rs.getLong("metric_id"), rs.getInt("machine_id"), ts(rs, "timestamp"),
            dbl(rs, "cpu_usage"), dbl(rs, "memory_usage"), dbl(rs, "disk_usage"), lng(rs, "uptime"));

    private final JdbcTemplate jdbc;

    public MetricsRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void insert(SystemMetric m) {
        jdbc.update("INSERT INTO system_metrics (machine_id, timestamp, cpu_usage, memory_usage, disk_usage, uptime) VALUES (?,?,?,?,?,?)",
                m.machineId(), m.timestamp(), m.cpuUsage(), m.memoryUsage(), m.diskUsage(), m.uptime());
    }

    public Optional<SystemMetric> latest(int machineId) {
        return jdbc.query("SELECT * FROM system_metrics WHERE machine_id = ? ORDER BY timestamp DESC, metric_id DESC LIMIT 1",
                MAPPER, machineId).stream().findFirst();
    }

    public List<SystemMetric> series(List<Integer> machineIds, LocalDateTime since) {
        if (machineIds.isEmpty()) return List.of();
        return jdbc.query("SELECT * FROM system_metrics WHERE machine_id IN (" + Sql.placeholders(machineIds.size())
                + ") AND timestamp >= ? ORDER BY timestamp, machine_id", MAPPER, Sql.args(machineIds, since));
    }
}
