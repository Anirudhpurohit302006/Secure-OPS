package com.secureops.service;

import com.secureops.dto.Dtos.MetricPoint;
import com.secureops.dto.Dtos.SecurityFinding;
import com.secureops.dto.Dtos.ServiceState;
import com.secureops.dto.Dtos.SystemOverview;
import com.secureops.model.Models.Machine;
import com.secureops.model.Models.SecurityState;
import com.secureops.model.Models.SystemMetric;
import com.secureops.repository.MachineRepository;
import com.secureops.repository.MetricsRepository;
import com.secureops.repository.NetworkRepository;
import com.secureops.repository.SecurityRepository;
import com.secureops.repository.ServiceRepository;
import com.secureops.util.TimeRange;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class SystemService {
    private final MachineRepository machines;
    private final MetricsRepository metrics;
    private final ServiceRepository services;
    private final SecurityRepository security;
    private final NetworkRepository network;
    private final SecurityAssessor assessor;
    private final TargetService targets;

    public SystemService(MachineRepository machines, MetricsRepository metrics, ServiceRepository services,
                         SecurityRepository security, NetworkRepository network, SecurityAssessor assessor,
                         TargetService targets) {
        this.machines = machines;
        this.metrics = metrics;
        this.services = services;
        this.security = security;
        this.network = network;
        this.assessor = assessor;
        this.targets = targets;
    }

    public List<SystemOverview> overviews(List<Integer> ids) {
        return machines.findAll().stream().filter(m -> ids.contains(m.machineId())).map(this::overview).toList();
    }

    public SystemOverview overview(int id) { return overview(targets.require(id)); }

    public SystemOverview overview(Machine m) {
        Optional<SystemMetric> metric = metrics.latest(m.machineId());
        Optional<SecurityState> state = security.latestState(m.machineId());
        List<SecurityFinding> findings = state
                .map(s -> assessor.assess(m.machineId(), m.machineName(), s, network.latestPorts(m.machineId()), s.timestamp()))
                .orElse(List.of());
        List<ServiceState> svc = services.latest(m.machineId()).stream()
                .map(s -> new ServiceState(s.serviceName(), s.status())).toList();
        return new SystemOverview(m.machineId(), m.machineName(), m.hostname(), m.ipAddress(), m.enabled(), m.status(),
                m.lastSeen(), m.osName(), m.osVersion(), m.kernelVersion(), m.machineUid(),
                metric.map(SystemMetric::cpuUsage).orElse(null), metric.map(SystemMetric::memoryUsage).orElse(null),
                metric.map(SystemMetric::diskUsage).orElse(null), metric.map(SystemMetric::uptime).orElse(null),
                metric.map(SystemMetric::timestamp).orElse(null),
                SecurityAssessor.overallStatus(state.isPresent(), findings),
                state.map(SecurityState::firewallActive).orElse(null), state.map(SecurityState::selinuxMode).orElse(null),
                state.map(SecurityState::sshActive).orElse(null),
                (int) findings.stream().filter(f -> !f.severity().equals("INFO")).count(), svc);
    }

    public List<MetricPoint> metrics(int id, String range) {
        targets.require(id);
        LocalDateTime since = LocalDateTime.now(ZoneOffset.UTC).minus(TimeRange.parse(range));
        return metrics.series(List.of(id), since).stream()
                .map(m -> new MetricPoint(m.timestamp(), m.cpuUsage(), m.memoryUsage(), m.diskUsage(), m.uptime())).toList();
    }
}
