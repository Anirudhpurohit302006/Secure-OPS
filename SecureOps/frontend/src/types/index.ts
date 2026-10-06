// Mirrors backend com.secureops.dto.Dtos. Timestamps are UTC ISO strings without zone.

export type MachineStatus = 'ONLINE' | 'OFFLINE' | 'UNKNOWN';
export type SecurityStatus = 'SECURE' | 'WARNING' | 'AT_RISK' | 'UNKNOWN';
export type Severity = 'INFO' | 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type ServiceStatusValue = 'RUNNING' | 'STOPPED' | 'FAILED' | 'UNKNOWN';
export type SelinuxMode = 'ENFORCING' | 'PERMISSIVE' | 'DISABLED' | 'UNKNOWN';
export type RangeKey = '6h' | '24h' | '7d' | '30d';
/** 'all' or a machine id as string */
export type TargetId = 'all' | string;

export interface SessionUser { username: string }

export interface Target {
  id: number; machineName: string; hostname: string | null; ipAddress: string; sshUsername: string;
  sshPort: number; enabled: boolean; status: MachineStatus; lastSeen: string | null;
}
export interface TargetRequest {
  machineName: string; ipAddress: string; sshUsername: string; sshPort?: number; enabled?: boolean;
}
export interface ActionResult { success: boolean; message: string; durationMs: number }

export interface ServiceStatus { name: string; status: ServiceStatusValue }

export interface Machine extends Target {}

export interface SystemOverview {
  id: number; machineName: string; hostname: string | null; ipAddress: string; enabled: boolean;
  status: MachineStatus; lastSeen: string | null; osName: string | null; osVersion: string | null;
  kernelVersion: string | null; machineUid: string | null; cpuUsage: number | null; memoryUsage: number | null;
  diskUsage: number | null; uptimeSeconds: number | null; lastCollected: string | null;
  securityStatus: SecurityStatus; firewallActive: boolean | null; selinuxMode: SelinuxMode | null;
  sshActive: boolean | null; findingsCount: number; services: ServiceStatus[];
}

export interface SystemMetric {
  timestamp: string; cpuUsage: number | null; memoryUsage: number | null; diskUsage: number | null; uptime: number | null;
}
export interface NetworkMetric { name: string; state: string | null; ipAddress: string | null; rxBytes: number; txBytes: number }
export interface NetworkResponse { timestamp: string | null; interfaces: NetworkMetric[] }
export interface ListeningPort { protocol: string; port: number; bindAddress: string; state: string }

export interface SecurityFinding {
  severity: Severity; type: string; description: string; timestamp: string; machineId: number; machineName: string;
}
export interface SecurityResponse {
  overallStatus: SecurityStatus; firewallActive: boolean | null; selinuxMode: SelinuxMode | null;
  sshActive: boolean | null; sshPort: number | null; sshPortListening: boolean;
  sshConfig: Record<string, string> | null; sshConfigReadable: boolean;
  ports: ListeningPort[]; findings: SecurityFinding[]; lastCollected: string | null;
}
export interface AuthEvent {
  id: number; machineId: number; machineName: string; username: string; sourceIp: string;
  eventType: 'SSH_LOGIN_SUCCESS' | 'SSH_LOGIN_FAILED'; timestamp: string;
}
export interface SecurityEvent {
  id: number; machineId: number; machineName: string; eventType: string; severity: Severity;
  description: string; timestamp: string;
}
export interface ServiceHistoryPoint { timestamp: string; serviceName: string; status: ServiceStatusValue }
export interface EventItem {
  id: string; source: 'AUTH' | 'SECURITY' | 'SYSTEM'; machineId: number; machineName: string; type: string;
  severity: Severity; description: string; timestamp: string;
}
export interface DashboardSummary {
  totalSystems: number; onlineSystems: number; offlineSystems: number; overallSecurity: SecurityStatus;
  failedLogins24h: number; successfulLogins24h: number; securityFindings: number; systems: SystemOverview[];
}
/** value/value2: network => RX / TX bytes per second */
export interface AnalyticsPoint {
  timestamp: string; machineId: number; machineName: string; value: number | null; value2: number | null;
}
export interface ServiceAvailability {
  machineId: number; machineName: string; serviceName: string; availabilityPct: number; samples: number;
}
export interface ServiceHistoryEntry {
  timestamp: string; machineId: number; machineName: string; serviceName: string; status: ServiceStatusValue;
}
export interface ServiceAnalytics { availability: ServiceAvailability[]; history: ServiceHistoryEntry[] }
export interface LoginBucket { bucket: string; success: number; failed: number }
export interface SourceCount { key: string; count: number }
export interface AuthAnalytics {
  timeline: LoginBucket[]; topFailedSources: SourceCount[]; topFailedUsers: SourceCount[];
  totalSuccess: number; totalFailed: number;
}
export interface SeverityCount { severity: Severity; count: number }
export interface SecurityBucket { bucket: string; info: number; low: number; medium: number; high: number; critical: number }
export interface SecurityAnalytics { bySeverity: SeverityCount[]; timeline: SecurityBucket[]; byType: SourceCount[] }
