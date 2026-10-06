package com.secureops.dto;

import com.secureops.model.Models.Machine;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** API data-transfer objects. Timestamps are UTC (serialized without zone; the frontend treats them as UTC). */
public final class Dtos {
    private Dtos() {}

    public record ApiError(String error, String message) {}
    public record LoginRequest(String username, String password) {}
    public record SessionUser(String username) {}

    public record TargetRequest(String machineName, String ipAddress, String sshUsername, Integer sshPort, Boolean enabled) {}

    public record TargetDto(int id, String machineName, String hostname, String ipAddress, String sshUsername,
                            int sshPort, boolean enabled, String status, LocalDateTime lastSeen) {
        public static TargetDto from(Machine m) {
            return new TargetDto(m.machineId(), m.machineName(), m.hostname(), m.ipAddress(), m.sshUsername(),
                    m.sshPort(), m.enabled(), m.status(), m.lastSeen());
        }
    }

    public record ActionResult(boolean success, String message, long durationMs) {}

    public record ServiceState(String name, String status) {}

    public record SystemOverview(int id, String machineName, String hostname, String ipAddress, boolean enabled,
                                 String status, LocalDateTime lastSeen, String osName, String osVersion,
                                 String kernelVersion, String machineUid, Double cpuUsage, Double memoryUsage,
                                 Double diskUsage, Long uptimeSeconds, LocalDateTime lastCollected,
                                 String securityStatus, Boolean firewallActive, String selinuxMode,
                                 Boolean sshActive, int findingsCount, List<ServiceState> services) {}

    public record MetricPoint(LocalDateTime timestamp, Double cpuUsage, Double memoryUsage, Double diskUsage, Long uptime) {}

    public record InterfaceInfo(String name, String state, String ipAddress, long rxBytes, long txBytes) {}
    public record NetworkResponse(LocalDateTime timestamp, List<InterfaceInfo> interfaces) {}
    public record PortInfo(String protocol, int port, String bindAddress, String state) {}

    public record SecurityFinding(String severity, String type, String description, LocalDateTime timestamp,
                                  int machineId, String machineName) {}

    public record SecurityResponse(String overallStatus, Boolean firewallActive, String selinuxMode, Boolean sshActive,
                                   Integer sshPort, boolean sshPortListening, Map<String, String> sshConfig,
                                   boolean sshConfigReadable, List<PortInfo> ports, List<SecurityFinding> findings,
                                   LocalDateTime lastCollected) {}

    public record AuthEventDto(long id, int machineId, String machineName, String username, String sourceIp,
                               String eventType, LocalDateTime timestamp) {}

    public record SecurityEventDto(long id, int machineId, String machineName, String eventType, String severity,
                                   String description, LocalDateTime timestamp) {}

    public record ServiceHistoryPoint(LocalDateTime timestamp, String serviceName, String status) {}

    public record EventItem(String id, String source, int machineId, String machineName, String type, String severity,
                            String description, LocalDateTime timestamp) {}

    public record DashboardSummary(int totalSystems, int onlineSystems, int offlineSystems, String overallSecurity,
                                   int failedLogins24h, int successfulLogins24h, int securityFindings,
                                   List<SystemOverview> systems) {}

    /** value/value2 meaning depends on the chart (network: value = RX B/s, value2 = TX B/s). */
    public record AnalyticsPoint(LocalDateTime timestamp, int machineId, String machineName, Double value, Double value2) {}

    public record ServiceAvailability(int machineId, String machineName, String serviceName, double availabilityPct, int samples) {}
    public record ServiceHistoryEntry(LocalDateTime timestamp, int machineId, String machineName, String serviceName, String status) {}
    public record ServiceAnalytics(List<ServiceAvailability> availability, List<ServiceHistoryEntry> history) {}

    public record LoginBucket(LocalDateTime bucket, int success, int failed) {}
    public record SourceCount(String key, int count) {}
    public record AuthAnalytics(List<LoginBucket> timeline, List<SourceCount> topFailedSources,
                                List<SourceCount> topFailedUsers, int totalSuccess, int totalFailed) {}

    public record SeverityCount(String severity, int count) {}
    public record SecurityBucket(LocalDateTime bucket, int info, int low, int medium, int high, int critical) {}
    public record SecurityAnalytics(List<SeverityCount> bySeverity, List<SecurityBucket> timeline, List<SourceCount> byType) {}
}
