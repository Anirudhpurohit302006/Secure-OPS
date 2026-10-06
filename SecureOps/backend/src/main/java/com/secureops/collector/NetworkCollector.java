package com.secureops.collector;

import com.secureops.model.Models.ListeningPort;
import com.secureops.model.Models.NetworkMetric;
import com.secureops.ssh.ApprovedCommand;
import com.secureops.ssh.RemoteSession;
import com.secureops.ssh.SshException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class NetworkCollector {

    public record IfaceInfo(String name, String state, String addresses) {}

    public record Snapshot(List<NetworkMetric> metrics, List<ListeningPort> ports) {}

    public Snapshot collect(RemoteSession s, int machineId, LocalDateTime now) throws SshException {
        Map<String, IfaceInfo> info = new LinkedHashMap<>();
        for (IfaceInfo i : parseIpBrief(s.run(ApprovedCommand.IP_BRIEF).stdout())) info.put(i.name(), i);
        Map<String, long[]> traffic = parseNetDev(s.run(ApprovedCommand.NET_DEV).stdout());

        List<NetworkMetric> metrics = new ArrayList<>();
        for (Map.Entry<String, long[]> e : traffic.entrySet()) {
            if (e.getKey().equals("lo")) continue;
            IfaceInfo i = info.get(e.getKey());
            metrics.add(new NetworkMetric(0, machineId, e.getKey(), e.getValue()[0], e.getValue()[1],
                    i == null ? null : i.state(), i == null ? null : i.addresses(), now));
        }
        List<ListeningPort> ports = parsePorts(s.run(ApprovedCommand.LISTENING_PORTS).stdout(), machineId, now);
        return new Snapshot(metrics, ports);
    }

    /** `ip -brief address`: "ens160  UP  192.168.159.129/24 fe80::1/64". */
    public static List<IfaceInfo> parseIpBrief(String text) {
        List<IfaceInfo> out = new ArrayList<>();
        for (String line : text.split("\\R")) {
            String[] p = line.trim().split("\\s+");
            if (p.length < 2) continue;
            List<String> v4 = new ArrayList<>();
            List<String> v6 = new ArrayList<>();
            for (int i = 2; i < p.length; i++) (p[i].contains(":") ? v6 : v4).add(p[i]);
            String addrs = String.join(", ", v4.isEmpty() ? v6 : v4);
            String name = p[0].contains("@") ? p[0].substring(0, p[0].indexOf('@')) : p[0];
            out.add(new IfaceInfo(name, p[1], addrs));
        }
        return out;
    }

    /** /proc/net/dev -> interface -> {rxBytes, txBytes}. */
    public static Map<String, long[]> parseNetDev(String text) {
        Map<String, long[]> out = new LinkedHashMap<>();
        for (String line : text.split("\\R")) {
            int c = line.indexOf(':');
            if (c < 0 || line.contains("|")) continue;
            String name = line.substring(0, c).trim();
            String[] f = line.substring(c + 1).trim().split("\\s+");
            if (f.length < 9) continue;
            try {
                out.put(name, new long[] {Long.parseUnsignedLong(f[0]), Long.parseUnsignedLong(f[8])});
            } catch (NumberFormatException ignored) { }
        }
        return out;
    }

    /** `ss -tuln`: Netid State Recv-Q Send-Q Local:Port Peer:Port. */
    public static List<ListeningPort> parsePorts(String text, int machineId, LocalDateTime now) {
        List<ListeningPort> out = new ArrayList<>();
        for (String line : text.split("\\R")) {
            String[] p = line.trim().split("\\s+");
            if (p.length < 5 || !(p[0].equals("tcp") || p[0].equals("udp"))) continue;
            String local = p[4];
            int colon = local.lastIndexOf(':');
            if (colon < 0) continue;
            String addr = local.substring(0, colon);
            int pct = addr.indexOf('%');
            if (pct >= 0) addr = addr.substring(0, pct);
            if (addr.startsWith("[") && addr.endsWith("]")) addr = addr.substring(1, addr.length() - 1);
            if (addr.equals("*")) addr = "0.0.0.0";
            try {
                int port = Integer.parseInt(local.substring(colon + 1));
                String state = p[1].equals("UNCONN") ? "LISTEN" : p[1];
                out.add(new ListeningPort(machineId, p[0], port, addr, state, now));
            } catch (NumberFormatException ignored) { }
        }
        return out;
    }
}
