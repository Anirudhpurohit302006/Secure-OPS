package com.secureops.collector;

import com.secureops.config.AppProperties;
import com.secureops.ssh.ApprovedCommand;
import com.secureops.ssh.CommandResult;
import com.secureops.ssh.RemoteSession;
import com.secureops.ssh.SshException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ServiceCollector {

    public record Snapshot(Map<String, String> states, List<String> failedUnits) {}

    private final List<String> services;

    public ServiceCollector(AppProperties props) {
        Set<String> all = new LinkedHashSet<>(List.of("sshd", "firewalld", "httpd"));
        if (props.monitoredServices() != null) {
            for (String s : props.monitoredServices()) if (s != null && !s.isBlank()) all.add(s.trim());
        }
        all.forEach(ApprovedCommand::serviceActive);   // fail fast on invalid configured names
        this.services = List.copyOf(all);
    }

    public Snapshot collect(RemoteSession s) throws SshException {
        Map<String, String> states = new LinkedHashMap<>();
        for (String name : services) {
            CommandResult r = s.run(ApprovedCommand.serviceActive(name));
            states.put(name, mapState(r.trimmed()));
        }
        List<String> failed = parseFailedUnits(s.run(ApprovedCommand.FAILED_UNITS).stdout());
        return new Snapshot(states, failed);
    }

    /** `systemctl is-active` output -> RUNNING / STOPPED / FAILED / UNKNOWN. */
    public static String mapState(String out) {
        String first = out.lines().findFirst().orElse("").trim();
        return switch (first) {
            case "active", "activating", "reloading" -> "RUNNING";
            case "inactive", "deactivating" -> "STOPPED";
            case "failed" -> "FAILED";
            default -> "UNKNOWN";
        };
    }

    /** `systemctl --failed --no-legend --plain`: first column is the unit name. */
    public static List<String> parseFailedUnits(String text) {
        List<String> units = new ArrayList<>();
        for (String line : text.split("\\R")) {
            String t = line.trim();
            if (t.isEmpty()) continue;
            String unit = t.split("\\s+")[0];
            if (unit.contains(".")) units.add(unit);
        }
        return units;
    }
}
