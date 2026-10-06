package com.secureops.repository;

import static com.secureops.util.Sql.ts;

import com.secureops.model.Models.Machine;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class MachineRepository {
    private static final RowMapper<Machine> MAPPER = (rs, i) -> new Machine(
            rs.getInt("machine_id"), rs.getString("machine_name"), rs.getString("hostname"),
            rs.getString("ip_address"), rs.getString("ssh_username"), rs.getInt("ssh_port"),
            rs.getBoolean("enabled"), rs.getString("status"), ts(rs, "last_seen"),
            rs.getString("os_name"), rs.getString("os_version"), rs.getString("kernel_version"),
            rs.getString("machine_uid"));

    private final JdbcTemplate jdbc;

    public MachineRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Machine> findAll() {
        return jdbc.query("SELECT * FROM machines ORDER BY machine_id", MAPPER);
    }

    public List<Machine> findEnabled() {
        return jdbc.query("SELECT * FROM machines WHERE enabled = TRUE ORDER BY machine_id", MAPPER);
    }

    public Optional<Machine> findById(int id) {
        return jdbc.query("SELECT * FROM machines WHERE machine_id = ?", MAPPER, id).stream().findFirst();
    }

    public boolean nameExists(String name, int excludeId) {
        Integer c = jdbc.queryForObject("SELECT COUNT(*) FROM machines WHERE machine_name = ? AND machine_id <> ?",
                Integer.class, name, excludeId);
        return c != null && c > 0;
    }

    public int insert(String name, String ip, String user, int port, boolean enabled) {
        // RETURNING gives exactly one value; getGeneratedKeys() would return every column on PostgreSQL.
        Integer id = jdbc.queryForObject(
                "INSERT INTO machines (machine_name, ip_address, ssh_username, ssh_port, enabled) VALUES (?,?,?,?,?) RETURNING machine_id",
                Integer.class, name, ip, user, port, enabled);
        return id;
    }

    public void update(int id, String name, String ip, String user, int port, boolean enabled) {
        jdbc.update("UPDATE machines SET machine_name=?, ip_address=?, ssh_username=?, ssh_port=?, enabled=? WHERE machine_id=?",
                name, ip, user, port, enabled, id);
    }

    public void delete(int id) {
        jdbc.update("DELETE FROM machines WHERE machine_id = ?", id);
    }

    /** last_seen is only changed when a value is supplied, so it is preserved while a target is offline. */
    public void updateStatus(int id, String status, LocalDateTime lastSeenOrNull) {
        jdbc.update("UPDATE machines SET status = ?, last_seen = COALESCE(CAST(? AS TIMESTAMP), last_seen) WHERE machine_id = ?",
                status, lastSeenOrNull, id);
    }

    public void updateInfo(int id, String hostname, String osName, String osVersion, String kernel, String machineUid) {
        jdbc.update("UPDATE machines SET hostname=?, os_name=?, os_version=?, kernel_version=?, machine_uid=? WHERE machine_id=?",
                hostname, osName, osVersion, kernel, machineUid, id);
    }
}
