package com.secureops.repository;

import static com.secureops.util.Sql.ts;

import com.secureops.model.Models.ServiceStatus;
import com.secureops.util.Sql;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** Append-only service_status. */
@Repository
public class ServiceRepository {
    private static final RowMapper<ServiceStatus> MAPPER = (rs, i) -> new ServiceStatus(
            rs.getLong("service_id"), rs.getInt("machine_id"), rs.getString("service_name"),
            rs.getString("status"), ts(rs, "timestamp"));

    private final JdbcTemplate jdbc;

    public ServiceRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void insertAll(List<ServiceStatus> list) {
        jdbc.batchUpdate("INSERT INTO service_status (machine_id, service_name, status, timestamp) VALUES (?,?,?,?)",
                list, list.size(), (ps, s) -> {
                    ps.setInt(1, s.machineId());
                    ps.setString(2, s.serviceName());
                    ps.setString(3, s.status());
                    ps.setObject(4, s.timestamp());
                });
    }

    public List<ServiceStatus> latest(int machineId) {
        return jdbc.query("SELECT * FROM service_status WHERE machine_id = ? AND timestamp = "
                + "(SELECT MAX(timestamp) FROM service_status WHERE machine_id = ?) ORDER BY service_name",
                MAPPER, machineId, machineId);
    }

    public List<ServiceStatus> series(List<Integer> machineIds, LocalDateTime since) {
        if (machineIds.isEmpty()) return List.of();
        return jdbc.query("SELECT * FROM service_status WHERE machine_id IN (" + Sql.placeholders(machineIds.size())
                + ") AND timestamp >= ? ORDER BY timestamp, machine_id, service_name", MAPPER, Sql.args(machineIds, since));
    }
}
