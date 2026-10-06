package com.secureops.repository;

import static com.secureops.util.Sql.ts;

import com.secureops.model.Models.ListeningPort;
import com.secureops.model.Models.NetworkMetric;
import com.secureops.util.Sql;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** Append-only network_metrics and listening_ports. */
@Repository
public class NetworkRepository {
    private static final RowMapper<NetworkMetric> METRIC = (rs, i) -> new NetworkMetric(
            rs.getLong("network_id"), rs.getInt("machine_id"), rs.getString("interface"), rs.getLong("rx_bytes"),
            rs.getLong("tx_bytes"), rs.getString("state"), rs.getString("ip_address"), ts(rs, "timestamp"));
    private static final RowMapper<ListeningPort> PORT = (rs, i) -> new ListeningPort(
            rs.getInt("machine_id"), rs.getString("protocol"), rs.getInt("port"), rs.getString("bind_address"),
            rs.getString("state"), ts(rs, "timestamp"));

    private final JdbcTemplate jdbc;

    public NetworkRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void insertMetrics(List<NetworkMetric> list) {
        jdbc.batchUpdate("INSERT INTO network_metrics (machine_id, interface, rx_bytes, tx_bytes, state, ip_address, timestamp) VALUES (?,?,?,?,?,?,?)",
                list, list.size(), (ps, m) -> {
                    ps.setInt(1, m.machineId());
                    ps.setString(2, m.interfaceName());
                    ps.setLong(3, m.rxBytes());
                    ps.setLong(4, m.txBytes());
                    ps.setString(5, m.state());
                    ps.setString(6, m.ipAddress());
                    ps.setObject(7, m.timestamp());
                });
    }

    public void insertPorts(List<ListeningPort> list) {
        jdbc.batchUpdate("INSERT INTO listening_ports (machine_id, protocol, port, bind_address, state, timestamp) VALUES (?,?,?,?,?,?)",
                list, list.size(), (ps, p) -> {
                    ps.setInt(1, p.machineId());
                    ps.setString(2, p.protocol());
                    ps.setInt(3, p.port());
                    ps.setString(4, p.bindAddress());
                    ps.setString(5, p.state());
                    ps.setObject(6, p.timestamp());
                });
    }

    public List<NetworkMetric> latestMetrics(int machineId) {
        return jdbc.query("SELECT * FROM network_metrics WHERE machine_id = ? AND timestamp = "
                + "(SELECT MAX(timestamp) FROM network_metrics WHERE machine_id = ?) ORDER BY interface",
                METRIC, machineId, machineId);
    }

    public List<ListeningPort> latestPorts(int machineId) {
        return jdbc.query("SELECT * FROM listening_ports WHERE machine_id = ? AND timestamp = "
                + "(SELECT MAX(timestamp) FROM listening_ports WHERE machine_id = ?) ORDER BY port, protocol",
                PORT, machineId, machineId);
    }

    public List<NetworkMetric> series(List<Integer> machineIds, LocalDateTime since) {
        if (machineIds.isEmpty()) return List.of();
        return jdbc.query("SELECT * FROM network_metrics WHERE machine_id IN (" + Sql.placeholders(machineIds.size())
                + ") AND timestamp >= ? ORDER BY machine_id, interface, timestamp", METRIC, Sql.args(machineIds, since));
    }
}
