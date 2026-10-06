package com.secureops.service;

import com.secureops.collector.AuthenticationCollector;
import com.secureops.collector.NetworkCollector;
import com.secureops.collector.SecurityCollector;
import com.secureops.collector.ServiceCollector;
import com.secureops.collector.SystemCollector;
import com.secureops.dto.Dtos.ActionResult;
import com.secureops.dto.Dtos.SecurityFinding;
import com.secureops.model.Models.AuthEvent;
import com.secureops.model.Models.Machine;
import com.secureops.model.Models.SecurityState;
import com.secureops.model.Models.ServiceStatus;
import com.secureops.model.Models.SystemEvent;
import com.secureops.model.Models.SystemMetric;
import com.secureops.repository.AuthRepository;
import com.secureops.repository.EventRepository;
import com.secureops.repository.MachineRepository;
import com.secureops.repository.MetricsRepository;
import com.secureops.repository.NetworkRepository;
import com.secureops.repository.SecurityRepository;
import com.secureops.repository.ServiceRepository;
import com.secureops.ssh.RemoteSession;
import com.secureops.ssh.SshException;
import com.secureops.ssh.SshManager;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * One collection run for one target: open SSH, collect approved data, close SSH, persist (append-only).
 * Offline targets keep their history and last_seen.
 */
@Service
public class CollectionService {
    private static final Logger log = LoggerFactory.getLogger(CollectionService.class);

    private record Collected(SystemCollector.Snapshot system, NetworkCollector.Snapshot network,
                             ServiceCollector.Snapshot services, AuthenticationCollector.Result auth,
                             SecurityState security) {}

    private final SshManager ssh;
    private final SystemCollector systemCollector;
    private final NetworkCollector networkCollector;
    private final ServiceCollector serviceCollector;
    private final AuthenticationCollector authCollector;
    private final SecurityCollector securityCollector;
    private final SecurityAssessor assessor;
    private final MachineRepository machines;
    private final MetricsRepository metrics;
    private final NetworkRepository network;
    private final ServiceRepository services;
    private final AuthRepository auth;
    private final SecurityRepository security;
    private final EventRepository events;
    private final TransactionTemplate tx;

    public CollectionService(SshManager ssh, SystemCollector systemCollector, NetworkCollector networkCollector,
                             ServiceCollector serviceCollector, AuthenticationCollector authCollector,
                             SecurityCollector securityCollector, SecurityAssessor assessor,
                             MachineRepository machines, MetricsRepository metrics, NetworkRepository network,
                             ServiceRepository services, AuthRepository auth, SecurityRepository security,
                             EventRepository events, TransactionTemplate tx) {
        this.ssh = ssh;
        this.systemCollector = systemCollector;
        this.networkCollector = networkCollector;
        this.serviceCollector = serviceCollector;
        this.authCollector = authCollector;
        this.securityCollector = securityCollector;
        this.assessor = assessor;
        this.machines = machines;
        this.metrics = metrics;
        this.network = network;
        this.services = services;
        this.auth = auth;
        this.security = security;
        this.events = events;
        this.tx = tx;
    }

    /** Synchronized so a scheduled run and a manual "collect now" never overlap. */
    public synchronized ActionResult collect(Machine m) {
        long start = System.currentTimeMillis();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
        Collected c;
        try (RemoteSession s = ssh.connect(m)) {
            c = gather(s, m, now);
        } catch (SshException e) {
            log.warn("Collection failed for {}: {}", m.machineName(), e.getMessage());
            handleOffline(m, now, e.getMessage());
            return new ActionResult(false, e.getMessage(), System.currentTimeMillis() - start);
        } catch (RuntimeException e) {
            log.error("Unexpected collection error for {}", m.machineName(), e);
            handleOffline(m, now, "Unexpected error while collecting data.");
            return new ActionResult(false, "Unexpected error while collecting data.", System.currentTimeMillis() - start);
        }
        tx.executeWithoutResult(status -> persist(m, now, c));
        return new ActionResult(true, "Collection complete.", System.currentTimeMillis() - start);
    }

    private Collected gather(RemoteSession s, Machine m, LocalDateTime now) throws SshException {
        var sys = systemCollector.collect(s);
        var net = networkCollector.collect(s, m.machineId(), now);
        var svc = serviceCollector.collect(s);
        var authResult = authCollector.collect(s, m.machineId(), now);
        var sec = securityCollector.collect(s, m.machineId(), svc.states(), net.ports(), now);
        return new Collected(sys, net, svc, authResult, sec);
    }

    private void handleOffline(Machine m, LocalDateTime now, String reason) {
        if ("ONLINE".equals(m.status())) {
            events.insert(new SystemEvent(0, m.machineId(), "SHUTDOWN",
                    "Target became unreachable (possible shutdown): " + reason, "MEDIUM", now));
        }
        machines.updateStatus(m.machineId(), "OFFLINE", null);   // last_seen preserved
    }

    private void persist(Machine m, LocalDateTime now, Collected c) {
        int id = m.machineId();
        SystemCollector.Snapshot sys = c.system();
        LocalDateTime since24h = now.minusHours(24);

        // ---- system metrics + startup/reboot detection (uptime reset) ----
        var previous = metrics.latest(id);
        metrics.insert(new SystemMetric(0, id, now, sys.cpuUsage(), sys.memoryUsage(), sys.diskUsage(), sys.uptimeSeconds()));
        LocalDateTime bootTime = now.minusSeconds(sys.uptimeSeconds());
        if (previous.isEmpty()) {
            events.insert(new SystemEvent(0, id, "STARTUP", "First collection; system has been up since " + bootTime + " UTC.", "INFO", bootTime));
        } else if (previous.get().uptime() != null && sys.uptimeSeconds() < previous.get().uptime()) {
            String type = "OFFLINE".equals(m.status()) ? "STARTUP" : "REBOOT";
            events.insert(new SystemEvent(0, id, type, "System booted at " + bootTime + " UTC.", "INFO", bootTime));
        }
        machines.updateInfo(id, sys.hostname(), sys.osName(), sys.osVersion(), sys.kernel(), sys.machineUid());
        machines.updateStatus(id, "ONLINE", now);

        // ---- network ----
        if (!c.network().metrics().isEmpty()) network.insertMetrics(c.network().metrics());
        if (!c.network().ports().isEmpty()) network.insertPorts(c.network().ports());

        // ---- services + transitions ----
        Map<String, String> before = new HashMap<>();
        for (ServiceStatus s : services.latest(id)) before.put(s.serviceName(), s.status());
        List<ServiceStatus> rows = new ArrayList<>();
        for (Map.Entry<String, String> e : c.services().states().entrySet()) {
            rows.add(new ServiceStatus(0, id, e.getKey(), e.getValue(), now));
            String prev = before.get(e.getKey());
            String cur = e.getValue();
            if (prev == null || prev.equals(cur)) continue;
            if (cur.equals("FAILED")) events.insert(new SystemEvent(0, id, "SERVICE_FAILURE", e.getKey() + " entered the failed state.", "HIGH", now));
            else if (cur.equals("STOPPED") && prev.equals("RUNNING")) events.insert(new SystemEvent(0, id, "SERVICE_STOP", e.getKey() + " stopped.", "MEDIUM", now));
            else if (cur.equals("RUNNING")) events.insert(new SystemEvent(0, id, "SERVICE_START", e.getKey() + " is running again.", "INFO", now));
        }
        if (!rows.isEmpty()) services.insertAll(rows);
        for (String unit : c.services().failedUnits()) {
            String desc = "systemd unit failed: " + unit;
            if (!events.existsRecent(id, "SERVICE_FAILURE", desc, since24h)) {
                events.insert(new SystemEvent(0, id, "SERVICE_FAILURE", desc, "HIGH", now));
            }
        }

        // ---- authentication events (idempotent) + simple detections on newly seen events ----
        List<AuthEvent> fresh = new ArrayList<>();
        for (AuthEvent e : c.auth().events()) if (auth.insertIgnore(e)) fresh.add(e);
        detectAuthPatterns(id, now, since24h, fresh);
        if (!c.auth().logReadable()) {
            security.insertEventIfNew(id, "AUTH_LOG_UNREADABLE", "INFO",
                    "Authentication log (/var/log/secure) is not readable by the monitoring user.", now, since24h);
        }

        // ---- security state + findings ----
        security.insertState(c.security());
        for (SecurityFinding f : assessor.assess(id, m.machineName(), c.security(), c.network().ports(), now)) {
            security.insertEventIfNew(id, f.type(), f.severity(), f.description(), now, since24h);
        }
    }

    private void detectAuthPatterns(int id, LocalDateTime now, LocalDateTime since24h, List<AuthEvent> fresh) {
        Map<String, Integer> failedBySource = new HashMap<>();
        for (AuthEvent e : fresh) {
            if (AuthenticationCollector.FAILED.equals(e.eventType())) failedBySource.merge(e.sourceIp(), 1, Integer::sum);
            if (AuthenticationCollector.SUCCESS.equals(e.eventType()) && "root".equals(e.username())) {
                security.insertEventIfNew(id, "ROOT_LOGIN", "MEDIUM",
                        "Successful SSH login as root from " + e.sourceIp() + ".", e.timestamp(), since24h);
            }
        }
        failedBySource.forEach((ip, n) -> {
            if (n >= 5) {
                String severity = n >= 10 ? "HIGH" : "MEDIUM";
                security.insertEvent(id, "SSH_BRUTE_FORCE", severity, n + " failed SSH logins from " + ip + " since the last collection.", now);
            }
        });
    }
}
