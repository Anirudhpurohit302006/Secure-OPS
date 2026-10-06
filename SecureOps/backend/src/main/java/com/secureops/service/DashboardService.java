package com.secureops.service;

import com.secureops.dto.Dtos.DashboardSummary;
import com.secureops.dto.Dtos.EventItem;
import com.secureops.dto.Dtos.SystemOverview;
import com.secureops.model.Models.Machine;
import com.secureops.repository.AuthRepository;
import com.secureops.repository.EventRepository;
import com.secureops.repository.MachineRepository;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {
    private final SystemService systems;
    private final AuthRepository auth;
    private final EventRepository events;
    private final MachineRepository machines;
    private final TargetService targets;

    public DashboardService(SystemService systems, AuthRepository auth, EventRepository events,
                            MachineRepository machines, TargetService targets) {
        this.systems = systems;
        this.auth = auth;
        this.events = events;
        this.machines = machines;
        this.targets = targets;
    }

    public DashboardSummary summary(String target) {
        List<Integer> ids = targets.resolveIds(target);
        List<SystemOverview> list = systems.overviews(ids);
        int online = (int) list.stream().filter(s -> s.status().equals("ONLINE")).count();
        LocalDateTime since = LocalDateTime.now(ZoneOffset.UTC).minusHours(24);
        int findings = list.stream().mapToInt(SystemOverview::findingsCount).sum();
        return new DashboardSummary(list.size(), online, list.size() - online, overall(list),
                auth.count(ids, "SSH_LOGIN_FAILED", since), auth.count(ids, "SSH_LOGIN_SUCCESS", since), findings, list);
    }

    public List<EventItem> events(String target, int limit) {
        List<Integer> ids = targets.resolveIds(target);
        Map<Integer, String> names = new HashMap<>();
        for (Machine m : machines.findAll()) names.put(m.machineId(), m.machineName());
        return events.feed(ids, Math.max(1, Math.min(limit, 200))).stream()
                .map(e -> new EventItem(e.id(), e.source(), e.machineId(), names.get(e.machineId()), e.type(),
                        e.severity(), e.description(), e.timestamp())).toList();
    }

    private static String overall(List<SystemOverview> list) {
        if (list.stream().allMatch(s -> s.securityStatus().equals("UNKNOWN"))) return "UNKNOWN";
        if (list.stream().anyMatch(s -> s.securityStatus().equals("AT_RISK"))) return "AT_RISK";
        if (list.stream().anyMatch(s -> s.securityStatus().equals("WARNING"))) return "WARNING";
        return "SECURE";
    }
}
