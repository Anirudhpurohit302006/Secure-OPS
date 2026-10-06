package com.secureops.model;

import java.time.LocalDateTime;
import java.util.Map;

/** Persistence models. All timestamps are UTC. */
public final class Models {
    private Models() {}

    public record Machine(int machineId, String machineName, String hostname, String ipAddress, String sshUsername,
                          int sshPort, boolean enabled, String status, LocalDateTime lastSeen, String osName,
                          String osVersion, String kernelVersion, String machineUid) {}

    public record SystemMetric(long metricId, int machineId, LocalDateTime timestamp, Double cpuUsage,
                               Double memoryUsage, Double diskUsage, Long uptime) {}

    public record NetworkMetric(long networkId, int machineId, String interfaceName, long rxBytes, long txBytes,
                                String state, String ipAddress, LocalDateTime timestamp) {}

    public record ServiceStatus(long serviceId, int machineId, String serviceName, String status,
                                LocalDateTime timestamp) {}

    public record AuthEvent(long eventId, int machineId, String username, String sourceIp, String eventType,
                            LocalDateTime timestamp) {}

    public record SecurityEvent(long eventId, int machineId, String eventType, String severity, String description,
                                LocalDateTime timestamp) {}

    public record SystemEvent(long eventId, int machineId, String eventType, String description, String severity,
                              LocalDateTime timestamp) {}

    public record ListeningPort(int machineId, String protocol, int port, String bindAddress, String state,
                                LocalDateTime timestamp) {}

    /** sshConfig == null means the configuration could not be read. */
    public record SecurityState(int machineId, Boolean firewallActive, String selinuxMode, Boolean sshActive,
                                Integer sshPort, Map<String, String> sshConfig, LocalDateTime timestamp) {}
}
