package com.secureops.ssh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ApprovedCommandTest {

    @Test
    void serviceNamesAreStrictlyValidated() {
        assertThat(ApprovedCommand.serviceActive("sshd").command()).isEqualTo("systemctl is-active sshd");
        assertThat(ApprovedCommand.serviceActive("getty@tty1").command()).isEqualTo("systemctl is-active getty@tty1");
        for (String bad : new String[] {"sshd; reboot", "$(id)", "a b", "-all", "", "x|y", "../etc"}) {
            assertThatThrownBy(() -> ApprovedCommand.serviceActive(bad)).isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> ApprovedCommand.serviceActive(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void commandsAreReadOnly() {
        assertThat(ApprovedCommand.CPU_SAMPLE.command()).contains("/proc/stat");
        assertThat(ApprovedCommand.LISTENING_PORTS.command()).isEqualTo("ss -tuln");
        assertThat(ApprovedCommand.AUTH_LOG.command()).contains("/usr/bin/tail -n 5000 /var/log/secure");
    }
}
