package com.secureops.collector;

import static org.assertj.core.api.Assertions.assertThat;

import com.secureops.model.Models.ListeningPort;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class NetworkCollectorTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 4, 12, 0);

    @Test
    void parsesProcNetDev() {
        String text = "Inter-|   Receive |  Transmit\n face |bytes packets|bytes packets\n"
                + "    lo: 10 1 0 0 0 0 0 0 20 1 0 0 0 0 0 0\n"
                + " ens160: 1000 5 0 0 0 0 0 0 2000 6 0 0 0 0 0 0\n";
        var map = NetworkCollector.parseNetDev(text);
        assertThat(map).containsOnlyKeys("lo", "ens160");
        assertThat(map.get("ens160")).containsExactly(1000L, 2000L);
    }

    @Test
    void parsesIpBrief() {
        var list = NetworkCollector.parseIpBrief("lo UNKNOWN 127.0.0.1/8 ::1/128\nens160 UP 192.168.159.129/24 fe80::1/64\n");
        assertThat(list).hasSize(2);
        assertThat(list.get(1).name()).isEqualTo("ens160");
        assertThat(list.get(1).state()).isEqualTo("UP");
        assertThat(list.get(1).addresses()).isEqualTo("192.168.159.129/24");
    }

    @Test
    void parsesListeningPorts() {
        String ss = "Netid State Recv-Q Send-Q Local Address:Port Peer Address:Port Process\n"
                + "tcp LISTEN 0 128 0.0.0.0:22 0.0.0.0:*\n"
                + "tcp LISTEN 0 128 [::]:22 [::]:*\n"
                + "udp UNCONN 0 0 127.0.0.1%lo:323 0.0.0.0:*\n";
        List<ListeningPort> ports = NetworkCollector.parsePorts(ss, 1, NOW);
        assertThat(ports).hasSize(3);
        assertThat(ports.get(0).port()).isEqualTo(22);
        assertThat(ports.get(1).bindAddress()).isEqualTo("::");
        assertThat(ports.get(2).bindAddress()).isEqualTo("127.0.0.1");
        assertThat(ports.get(2).state()).isEqualTo("LISTEN");
    }
}
