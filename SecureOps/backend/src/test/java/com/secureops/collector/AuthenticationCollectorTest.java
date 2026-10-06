package com.secureops.collector;

import static org.assertj.core.api.Assertions.assertThat;

import com.secureops.model.Models.AuthEvent;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class AuthenticationCollectorTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 4, 12, 0);

    @Test
    void parsesTraditionalAndIsoLinesAndConvertsToUtc() {
        String log = "Oct  4 09:12:33 rhel01 sshd[1234]: Failed password for invalid user bob from 10.0.0.5 port 4444 ssh2\n"
                + "Oct  4 09:13:00 rhel01 sshd[1235]: Accepted publickey for ani from 192.168.159.1 port 5555 ssh2: ED25519 SHA256:x\n"
                + "2026-10-04T10:00:00.123456+05:30 rhel01 sshd[9]: Failed password for root from 1.2.3.4 port 22 ssh2\n"
                + "Oct  4 09:14:00 rhel01 sudo[1]: ani : TTY=pts/0\n";
        List<AuthEvent> ev = AuthenticationCollector.parseLog(log, 1, NOW, ZoneOffset.ofHoursMinutes(5, 30));

        assertThat(ev).hasSize(3);
        assertThat(ev.get(0).username()).isEqualTo("bob");
        assertThat(ev.get(0).sourceIp()).isEqualTo("10.0.0.5");
        assertThat(ev.get(0).eventType()).isEqualTo("SSH_LOGIN_FAILED");
        assertThat(ev.get(0).timestamp()).isEqualTo(LocalDateTime.of(2026, 10, 4, 3, 42, 33));
        assertThat(ev.get(1).eventType()).isEqualTo("SSH_LOGIN_SUCCESS");
        assertThat(ev.get(2).timestamp()).isEqualTo(LocalDateTime.of(2026, 10, 4, 4, 30, 0));
    }

    @Test
    void decemberLogLineSeenInJanuaryBelongsToLastYear() {
        LocalDateTime january = LocalDateTime.of(2026, 1, 2, 0, 0);
        var ev = AuthenticationCollector.parseLog(
                "Dec 31 23:59:00 h sshd[1]: Failed password for x from 9.9.9.9 port 1 ssh2", 1, january, ZoneOffset.UTC);
        assertThat(ev.get(0).timestamp().getYear()).isEqualTo(2025);
    }

    @Test
    void parsesTimezoneOffset() {
        assertThat(AuthenticationCollector.parseOffset("-0400")).isEqualTo(ZoneOffset.ofHours(-4));
        assertThat(AuthenticationCollector.parseOffset("garbage")).isEqualTo(ZoneOffset.UTC);
    }
}
