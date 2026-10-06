package com.secureops.service;

import com.secureops.dto.Dtos.AnalyticsPoint;
import com.secureops.dto.Dtos.AuthAnalytics;
import com.secureops.dto.Dtos.LoginBucket;
import com.secureops.dto.Dtos.SecurityAnalytics;
import com.secureops.dto.Dtos.SecurityBucket;
import com.secureops.dto.Dtos.SeverityCount;
import com.secureops.dto.Dtos.ServiceAnalytics;
import com.secureops.dto.Dtos.ServiceAvailability;
import com.secureops.dto.Dtos.ServiceHistoryEntry;
import com.secureops.dto.Dtos.SourceCount;
import com.secureops.model.Models.AuthEvent;
import com.secureops.model.Models.Machine;
import com.secureops.model.Models.NetworkMetric;
import com.secureops.model.Models.SecurityEvent;
import com.secureops.model.Models.ServiceStatus;
import com.secureops.model.Models.SystemMetric;
import com.secureops.repository.AuthRepository;
import com.secureops.repository.MachineRepository;
import com.secureops.repository.MetricsRepository;
import com.secureops.repository.NetworkRepository;
import com.secureops.repository.SecurityRepository;
import com.secureops.repository.ServiceRepository;
import com.secureops.util.TimeRange;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import org.springframework.stereotype.Service;

/** Chart data. Reads historical PostgreSQL data only; never touches SSH. */
@Service
public class AnalyticsService {
    private static final int MAX_ROWS = 50_000;

    private final MetricsRepository metrics;
    private final NetworkRepository network;
    private final ServiceRepository services;
    private final AuthRepository auth;
    private final SecurityRepository security;
    private final MachineRepository machines;
    private final TargetService targets;

    public AnalyticsService(MetricsRepository metrics, NetworkRepository network, ServiceRepository services,
                            AuthRepository auth, SecurityRepository security, MachineRepository machines,
                            TargetService targets) {
        this.metrics = metrics;
        this.network = network;
        this.services = services;
        this.auth = auth;
        this.security = security;
        this.machines = machines;
        this.targets = targets;
    }

    public List<AnalyticsPoint> cpu(String target, String range) { return resource(target, range, SystemMetric::cpuUsage); }
    public List<AnalyticsPoint> memory(String target, String range) { return resource(target, range, SystemMetric::memoryUsage); }
    public List<AnalyticsPoint> disk(String target, String range) { return resource(target, range, SystemMetric::diskUsage); }

    private List<AnalyticsPoint> resource(String target, String range, Function<SystemMetric, Double> field) {
        List<Integer> ids = targets.resolveIds(target);
        Map<Integer, String> names = names();
        return metrics.series(ids, since(range)).stream()
                .map(m -> new AnalyticsPoint(m.timestamp(), m.machineId(), names.get(m.machineId()), field.apply(m), null)).toList();
    }

    /** Average RX/TX rate (bytes per second) between consecutive collections, summed over interfaces. */
    public List<AnalyticsPoint> network(String target, String range) {
        List<Integer> ids = targets.resolveIds(target);
        Map<Integer, String> names = names();
        List<NetworkMetric> rows = network.series(ids, since(range));   // ordered by machine, interface, time
        Map<Integer, TreeMap<LocalDateTime, double[]>> byMachine = new LinkedHashMap<>();
        NetworkMetric prev = null;
        for (NetworkMetric cur : rows) {
            if (prev != null && prev.machineId() == cur.machineId() && prev.interfaceName().equals(cur.interfaceName())) {
                long seconds = Duration.between(prev.timestamp(), cur.timestamp()).getSeconds();
                long dRx = cur.rxBytes() - prev.rxBytes();
                long dTx = cur.txBytes() - prev.txBytes();
                if (seconds > 0 && dRx >= 0 && dTx >= 0) {      // negative delta = counter reset after reboot: skip
                    double[] acc = byMachine.computeIfAbsent(cur.machineId(), k -> new TreeMap<>())
                            .computeIfAbsent(cur.timestamp(), k -> new double[2]);
                    acc[0] += (double) dRx / seconds;
                    acc[1] += (double) dTx / seconds;
                }
            }
            prev = cur;
        }
        List<AnalyticsPoint> out = new ArrayList<>();
        byMachine.forEach((machineId, series) -> series.forEach((ts, v) ->
                out.add(new AnalyticsPoint(ts, machineId, names.get(machineId), v[0], v[1]))));
        out.sort(Comparator.comparing(AnalyticsPoint::timestamp));
        return out;
    }

    public ServiceAnalytics services(String target, String range) {
        List<Integer> ids = targets.resolveIds(target);
        Map<Integer, String> names = names();
        List<ServiceStatus> rows = services.series(ids, since(range));
        record Key(int machineId, String service) {}
        Map<Key, int[]> counts = new LinkedHashMap<>();   // {running, total}
        List<ServiceHistoryEntry> history = new ArrayList<>();
        Map<Key, String> last = new HashMap<>();
        for (ServiceStatus s : rows) {
            Key k = new Key(s.machineId(), s.serviceName());
            int[] c = counts.computeIfAbsent(k, x -> new int[2]);
            c[1]++;
            if (s.status().equals("RUNNING")) c[0]++;
            if (!s.status().equals(last.put(k, s.status()))) {   // keep state changes only (plus first sample)
                history.add(new ServiceHistoryEntry(s.timestamp(), s.machineId(), names.get(s.machineId()), s.serviceName(), s.status()));
            }
        }
        List<ServiceAvailability> availability = new ArrayList<>();
        counts.forEach((k, c) -> availability.add(new ServiceAvailability(k.machineId(), names.get(k.machineId()), k.service(),
                Math.round(c[0] * 10000.0 / c[1]) / 100.0, c[1])));
        history.sort(Comparator.comparing(ServiceHistoryEntry::timestamp).reversed());
        return new ServiceAnalytics(availability, history);
    }

    public AuthAnalytics authentication(String target, String range) {
        List<Integer> ids = targets.resolveIds(target);
        LocalDateTime since = since(range);
        List<AuthEvent> events = auth.list(ids, since, null, MAX_ROWS);
        boolean hourly = TimeRange.parse(range).compareTo(Duration.ofHours(24)) <= 0;
        ChronoUnit unit = hourly ? ChronoUnit.HOURS : ChronoUnit.DAYS;

        TreeMap<LocalDateTime, int[]> buckets = emptyBuckets(since, unit, 2);
        Map<String, Integer> sources = new HashMap<>();
        Map<String, Integer> users = new HashMap<>();
        int success = 0;
        int failed = 0;
        for (AuthEvent e : events) {
            int[] b = buckets.get(e.timestamp().truncatedTo(unit));
            if (e.eventType().equals("SSH_LOGIN_SUCCESS")) {
                success++;
                if (b != null) b[0]++;
            } else {
                failed++;
                if (b != null) b[1]++;
                sources.merge(e.sourceIp(), 1, Integer::sum);
                users.merge(e.username(), 1, Integer::sum);
            }
        }
        List<LoginBucket> timeline = new ArrayList<>();
        buckets.forEach((t, c) -> timeline.add(new LoginBucket(t, c[0], c[1])));
        return new AuthAnalytics(timeline, top(sources), top(users), success, failed);
    }

    public SecurityAnalytics security(String target, String range) {
        List<Integer> ids = targets.resolveIds(target);
        LocalDateTime since = since(range);
        List<SecurityEvent> events = security.events(ids, since, MAX_ROWS);
        boolean hourly = TimeRange.parse(range).compareTo(Duration.ofHours(24)) <= 0;
        ChronoUnit unit = hourly ? ChronoUnit.HOURS : ChronoUnit.DAYS;

        TreeMap<LocalDateTime, int[]> buckets = emptyBuckets(since, unit, 5);
        int[] severityTotals = new int[5];
        Map<String, Integer> types = new HashMap<>();
        for (SecurityEvent e : events) {
            int sev = com.secureops.model.Severity.valueOf(e.severity()).rank();
            severityTotals[sev]++;
            int[] b = buckets.get(e.timestamp().truncatedTo(unit));
            if (b != null) b[sev]++;
            types.merge(e.eventType(), 1, Integer::sum);
        }
        List<SeverityCount> bySeverity = new ArrayList<>();
        for (com.secureops.model.Severity s : com.secureops.model.Severity.values())
            bySeverity.add(new SeverityCount(s.name(), severityTotals[s.rank()]));
        List<SecurityBucket> timeline = new ArrayList<>();
        buckets.forEach((t, c) -> timeline.add(new SecurityBucket(t, c[0], c[1], c[2], c[3], c[4])));
        return new SecurityAnalytics(bySeverity, timeline, top(types));
    }

    // ---- helpers ----
    private static LocalDateTime since(String range) {
        return LocalDateTime.now(ZoneOffset.UTC).minus(TimeRange.parse(range));
    }

    private static TreeMap<LocalDateTime, int[]> emptyBuckets(LocalDateTime since, ChronoUnit unit, int width) {
        TreeMap<LocalDateTime, int[]> m = new TreeMap<>();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        for (LocalDateTime t = since.truncatedTo(unit); !t.isAfter(now); t = t.plus(1, unit)) m.put(t, new int[width]);
        return m;
    }

    private static List<SourceCount> top(Map<String, Integer> counts) {
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5).map(e -> new SourceCount(e.getKey(), e.getValue())).toList();
    }

    private Map<Integer, String> names() {
        Map<Integer, String> names = new HashMap<>();
        for (Machine m : machines.findAll()) names.put(m.machineId(), m.machineName());
        return names;
    }
}
