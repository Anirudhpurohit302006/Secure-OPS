package com.secureops.collector;

import com.secureops.model.Models.ListeningPort;
import com.secureops.model.Models.SecurityState;
import com.secureops.ssh.ApprovedCommand;
import com.secureops.ssh.CommandResult;
import com.secureops.ssh.RemoteSession;
import com.secureops.ssh.SshException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class SecurityCollector {

    private static final Set<String> DIRECTIVES = Set.of("port", "permitrootlogin", "passwordauthentication",
            "permitemptypasswords", "pubkeyauthentication", "x11forwarding", "maxauthtries");

    /** Service states and ports come from the Service/Network collectors so nothing is queried twice. */
    public SecurityState collect(RemoteSession s, int machineId, Map<String, String> serviceStates,
                                 List<ListeningPort> ports, LocalDateTime now) throws SshException {
        Boolean firewall = "RUNNING".equals(serviceStates.get("firewalld"));
        Boolean ssh = "RUNNING".equals(serviceStates.get("sshd"));
        String selinux = parseSelinux(s.run(ApprovedCommand.SELINUX).trimmed());

        CommandResult cfg = s.run(ApprovedCommand.SSHD_CONFIG);
        Map<String, String> config = (cfg.exitCode() == 0 || !cfg.stdout().isBlank()) ? parseSshdConfig(cfg.stdout()) : null;
        Integer port = 22;
        if (config != null && config.containsKey("Port")) {
            try { port = Integer.parseInt(config.get("Port")); } catch (NumberFormatException ignored) { }
        }
        return new SecurityState(machineId, firewall, selinux, ssh, port, config, now);
    }

    /** getenforce -> ENFORCING / PERMISSIVE / DISABLED / UNKNOWN. */
    public static String parseSelinux(String out) {
        return switch (out.trim().toLowerCase(Locale.ROOT)) {
            case "enforcing" -> "ENFORCING";
            case "permissive" -> "PERMISSIVE";
            case "disabled" -> "DISABLED";
            default -> "UNKNOWN";
        };
    }

    /** Keeps only approved directives; first occurrence wins (sshd semantics). Port keeps its number, others are lower-cased. */
    public static Map<String, String> parseSshdConfig(String text) {
        Map<String, String> out = new LinkedHashMap<>();
        for (String raw : text.split("\\R")) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] p = line.split("\\s+", 2);
            if (p.length < 2) continue;
            String key = p[0].toLowerCase(Locale.ROOT);
            if (!DIRECTIVES.contains(key)) continue;
            String canonical = canonical(key);
            String value = p[1].trim();
            if (!key.equals("port")) value = value.toLowerCase(Locale.ROOT);
            out.putIfAbsent(canonical, value);
        }
        return out;
    }

    private static String canonical(String key) {
        return switch (key) {
            case "port" -> "Port";
            case "permitrootlogin" -> "PermitRootLogin";
            case "passwordauthentication" -> "PasswordAuthentication";
            case "permitemptypasswords" -> "PermitEmptyPasswords";
            case "pubkeyauthentication" -> "PubkeyAuthentication";
            case "x11forwarding" -> "X11Forwarding";
            default -> "MaxAuthTries";
        };
    }
}
