package com.secureops.service;

import com.secureops.dto.Dtos.SecurityFinding;
import com.secureops.model.Models.ListeningPort;
import com.secureops.model.Models.SecurityState;
import com.secureops.model.Severity;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Pure rules: firewall, SELinux, SSH and listening-port assessment. No I/O. */
@Component
public class SecurityAssessor {

    private record Risky(String name, Severity severity) {}

    private static final Map<Integer, Risky> RISKY_PORTS = Map.ofEntries(
            Map.entry(21, new Risky("FTP", Severity.HIGH)),
            Map.entry(23, new Risky("Telnet", Severity.CRITICAL)),
            Map.entry(69, new Risky("TFTP", Severity.MEDIUM)),
            Map.entry(111, new Risky("rpcbind", Severity.LOW)),
            Map.entry(445, new Risky("SMB", Severity.MEDIUM)),
            Map.entry(512, new Risky("rexec", Severity.HIGH)),
            Map.entry(513, new Risky("rlogin", Severity.HIGH)),
            Map.entry(514, new Risky("rsh", Severity.HIGH)),
            Map.entry(3306, new Risky("MySQL", Severity.MEDIUM)),
            Map.entry(3389, new Risky("RDP", Severity.MEDIUM)),
            Map.entry(5432, new Risky("PostgreSQL", Severity.MEDIUM)),
            Map.entry(5900, new Risky("VNC", Severity.HIGH)),
            Map.entry(6379, new Risky("Redis", Severity.HIGH)),
            Map.entry(27017, new Risky("MongoDB", Severity.MEDIUM)));

    public List<SecurityFinding> assess(int machineId, String machineName, SecurityState st,
                                        List<ListeningPort> ports, LocalDateTime now) {
        List<SecurityFinding> f = new ArrayList<>();
        if (st == null) return f;

        // Firewall
        if (!Boolean.TRUE.equals(st.firewallActive())) {
            f.add(finding(Severity.HIGH, "FIREWALL_INACTIVE", "firewalld is not active; the host is not protected by the firewall.", machineId, machineName, now));
        }

        // SELinux
        switch (st.selinuxMode() == null ? "UNKNOWN" : st.selinuxMode()) {
            case "PERMISSIVE" -> f.add(finding(Severity.MEDIUM, "SELINUX_PERMISSIVE", "SELinux is in permissive mode (violations are logged but not blocked).", machineId, machineName, now));
            case "DISABLED" -> f.add(finding(Severity.HIGH, "SELINUX_DISABLED", "SELinux is disabled.", machineId, machineName, now));
            default -> { }
        }

        // SSH
        if (!Boolean.TRUE.equals(st.sshActive())) {
            f.add(finding(Severity.INFO, "SSH_INACTIVE", "sshd is not reported as active.", machineId, machineName, now));
        }
        Map<String, String> cfg = st.sshConfig();
        if (cfg == null) {
            f.add(finding(Severity.INFO, "SSH_CONFIG_UNREADABLE", "sshd_config could not be read by the monitoring user; SSH hardening was not assessed.", machineId, machineName, now));
        } else {
            if ("yes".equals(cfg.get("PermitRootLogin")))
                f.add(finding(Severity.HIGH, "SSH_ROOT_LOGIN", "SSH allows direct root login (PermitRootLogin yes).", machineId, machineName, now));
            if ("yes".equals(cfg.get("PermitEmptyPasswords")))
                f.add(finding(Severity.CRITICAL, "SSH_EMPTY_PASSWORDS", "SSH permits empty passwords (PermitEmptyPasswords yes).", machineId, machineName, now));
            if (!"no".equals(cfg.get("PasswordAuthentication")))
                f.add(finding(Severity.LOW, "SSH_PASSWORD_AUTH", "SSH password authentication is enabled; key-based authentication is safer.", machineId, machineName, now));
            if ("yes".equals(cfg.get("X11Forwarding")))
                f.add(finding(Severity.LOW, "SSH_X11_FORWARDING", "SSH X11 forwarding is enabled.", machineId, machineName, now));
            try {
                if (cfg.containsKey("MaxAuthTries") && Integer.parseInt(cfg.get("MaxAuthTries")) > 6)
                    f.add(finding(Severity.LOW, "SSH_MAX_AUTH_TRIES", "MaxAuthTries is higher than 6.", machineId, machineName, now));
            } catch (NumberFormatException ignored) { }
        }
        if (st.sshPort() != null && st.sshPort() == 22) {
            f.add(finding(Severity.INFO, "SSH_DEFAULT_PORT", "SSH listens on the default port 22.", machineId, machineName, now));
        }

        // Listening ports (non-loopback only)
        if (ports != null) {
            for (ListeningPort p : ports) {
                Risky r = RISKY_PORTS.get(p.port());
                if (r == null || isLoopback(p.bindAddress())) continue;
                f.add(finding(r.severity(), "RISKY_PORT",
                        "Port " + p.port() + "/" + p.protocol() + " (" + r.name() + ") is listening on " + p.bindAddress() + ".",
                        machineId, machineName, now));
            }
        }
        return f;
    }

    /** SECURE / WARNING / AT_RISK, or UNKNOWN when nothing has been collected yet. */
    public static String overallStatus(boolean hasData, List<SecurityFinding> findings) {
        if (!hasData) return "UNKNOWN";
        int worst = -1;
        for (SecurityFinding x : findings) worst = Math.max(worst, Severity.valueOf(x.severity()).rank());
        if (worst >= Severity.HIGH.rank()) return "AT_RISK";
        if (worst >= Severity.MEDIUM.rank()) return "WARNING";
        return "SECURE";
    }

    private static boolean isLoopback(String addr) {
        return addr != null && (addr.startsWith("127.") || addr.equals("::1"));
    }

    private static SecurityFinding finding(Severity s, String type, String desc, int id, String name, LocalDateTime now) {
        return new SecurityFinding(s.name(), type, desc, now, id, name);
    }
}
