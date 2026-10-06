package com.secureops.collector;

import com.secureops.ssh.ApprovedCommand;
import com.secureops.ssh.RemoteSession;
import com.secureops.ssh.SshException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SystemCollector {

    public record Snapshot(String hostname, String osName, String osVersion, String kernel, String machineUid,
                           String ip, long uptimeSeconds, double cpuUsage, double memoryUsage, Double diskUsage) {}

    public Snapshot collect(RemoteSession s) throws SshException {
        String hostname = s.run(ApprovedCommand.HOSTNAME).trimmed();
        Map<String, String> os = parseOsRelease(s.run(ApprovedCommand.OS_RELEASE).stdout());
        String kernel = s.run(ApprovedCommand.KERNEL).trimmed();
        String machineId = s.run(ApprovedCommand.MACHINE_ID).trimmed();
        String ip = firstToken(s.run(ApprovedCommand.IP_ADDRESSES).trimmed());
        long uptime = parseUptimeSeconds(s.run(ApprovedCommand.UPTIME).stdout());
        double cpu = parseCpuUsage(s.run(ApprovedCommand.CPU_SAMPLE).stdout());
        double mem = parseMemoryUsage(s.run(ApprovedCommand.MEMINFO).stdout());
        Double disk = parseDiskUsage(s.run(ApprovedCommand.DISK).stdout());
        return new Snapshot(hostname, os.getOrDefault("NAME", "Linux"), os.getOrDefault("VERSION_ID", ""), kernel,
                machineId, ip, uptime, cpu, mem, disk);
    }

    private static String firstToken(String s) {
        if (s.isBlank()) return "";
        return s.trim().split("\\s+")[0];
    }

    /** Parses /etc/os-release KEY="value" lines. */
    public static Map<String, String> parseOsRelease(String text) {
        Map<String, String> map = new HashMap<>();
        for (String line : text.split("\\R")) {
            int eq = line.indexOf('=');
            if (eq <= 0 || line.startsWith("#")) continue;
            String v = line.substring(eq + 1).trim();
            if (v.length() >= 2 && (v.startsWith("\"") && v.endsWith("\"") || v.startsWith("'") && v.endsWith("'"))) {
                v = v.substring(1, v.length() - 1);
            }
            map.put(line.substring(0, eq).trim(), v);
        }
        return map;
    }

    /** /proc/uptime: "12345.67 54321.00" -> 12345. */
    public static long parseUptimeSeconds(String text) {
        String t = text.trim();
        if (t.isEmpty()) return 0;
        return (long) Double.parseDouble(t.split("\\s+")[0]);
    }

    /**
     * Input: two "cpu ..." lines taken one second apart.
     * usage = (1 - dIdle / dTotal) * 100, idle = idle + iowait, total = user..steal (guest is already inside user).
     */
    public static double parseCpuUsage(String twoSamples) {
        long[][] samples = new long[2][];
        int n = 0;
        for (String line : twoSamples.split("\\R")) {
            if (line.startsWith("cpu ") && n < 2) samples[n++] = cpuFields(line);
        }
        if (n < 2) throw new IllegalArgumentException("Expected two /proc/stat samples");
        long dTotal = samples[1][0] - samples[0][0];
        long dIdle = samples[1][1] - samples[0][1];
        if (dTotal <= 0) return 0.0;
        double usage = (1.0 - (double) dIdle / dTotal) * 100.0;
        return round2(Math.max(0, Math.min(100, usage)));
    }

    /** @return {total, idleAll} */
    private static long[] cpuFields(String line) {
        String[] p = line.trim().split("\\s+");
        long total = 0;
        long idleAll = 0;
        for (int i = 1; i < p.length && i <= 8; i++) {
            long v = Long.parseLong(p[i]);
            total += v;
            if (i == 4 || i == 5) idleAll += v;   // idle, iowait
        }
        return new long[] {total, idleAll};
    }

    /** usage = (MemTotal - MemAvailable) / MemTotal. Falls back to Free+Buffers+Cached on very old kernels. */
    public static double parseMemoryUsage(String meminfo) {
        Map<String, Long> m = new HashMap<>();
        for (String line : meminfo.split("\\R")) {
            int c = line.indexOf(':');
            if (c < 0) continue;
            String[] v = line.substring(c + 1).trim().split("\\s+");
            try { m.put(line.substring(0, c).trim(), Long.parseLong(v[0])); } catch (NumberFormatException ignored) { }
        }
        Long total = m.get("MemTotal");
        if (total == null || total == 0) throw new IllegalArgumentException("MemTotal missing");
        Long avail = m.get("MemAvailable");
        if (avail == null) avail = m.getOrDefault("MemFree", 0L) + m.getOrDefault("Buffers", 0L) + m.getOrDefault("Cached", 0L);
        return round2((double) (total - avail) / total * 100.0);
    }

    /** Usage of the root filesystem from `df -B1 -P` (used / (used + available)). Null if "/" is not listed. */
    public static Double parseDiskUsage(String df) {
        for (String line : df.split("\\R")) {
            String[] p = line.trim().split("\\s+");
            if (p.length < 6 || !p[p.length - 1].equals("/")) continue;
            try {
                double used = Double.parseDouble(p[p.length - 4]);
                double avail = Double.parseDouble(p[p.length - 3]);
                if (used + avail <= 0) return null;
                return round2(used / (used + avail) * 100.0);
            } catch (NumberFormatException ignored) { }
        }
        return null;
    }

    private static double round2(double v) { return Math.round(v * 100.0) / 100.0; }
}
