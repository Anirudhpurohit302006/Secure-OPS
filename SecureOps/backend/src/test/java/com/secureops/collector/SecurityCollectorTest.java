package com.secureops.collector;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SecurityCollectorTest {

    @Test
    void sshdConfigFirstOccurrenceWinsAndOnlyApprovedKeysAreKept() {
        var cfg = SecurityCollector.parseSshdConfig(
                "#Port 22\nPermitRootLogin no\nPasswordAuthentication yes\nPermitRootLogin yes\nPort 2222\nHostKey /etc/x\n");
        assertThat(cfg).containsEntry("PermitRootLogin", "no")
                .containsEntry("PasswordAuthentication", "yes")
                .containsEntry("Port", "2222")
                .hasSize(3);
    }

    @Test
    void parsesSelinuxModes() {
        assertThat(SecurityCollector.parseSelinux("Enforcing\n")).isEqualTo("ENFORCING");
        assertThat(SecurityCollector.parseSelinux("Permissive")).isEqualTo("PERMISSIVE");
        assertThat(SecurityCollector.parseSelinux("Disabled")).isEqualTo("DISABLED");
        assertThat(SecurityCollector.parseSelinux("bash: getenforce: command not found")).isEqualTo("UNKNOWN");
    }
}
