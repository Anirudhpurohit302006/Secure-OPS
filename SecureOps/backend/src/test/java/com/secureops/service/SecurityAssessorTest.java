package com.secureops.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.secureops.dto.Dtos.SecurityFinding;
import com.secureops.model.Models.ListeningPort;
import com.secureops.model.Models.SecurityState;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SecurityAssessorTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 4, 12, 0);
    private final SecurityAssessor assessor = new SecurityAssessor();

    private static List<String> types(List<SecurityFinding> f) { return f.stream().map(SecurityFinding::type).toList(); }

    @Test
    void weakConfigurationProducesFindings() {
        var state = new SecurityState(1, false, "PERMISSIVE", true, 22, Map.of("PermitRootLogin", "yes"), NOW);
        var ports = List.of(new ListeningPort(1, "tcp", 23, "0.0.0.0", "LISTEN", NOW),
                new ListeningPort(1, "tcp", 3306, "127.0.0.1", "LISTEN", NOW));
        var findings = assessor.assess(1, "RHEL-01", state, ports, NOW);

        assertThat(types(findings)).contains("FIREWALL_INACTIVE", "SELINUX_PERMISSIVE", "SSH_ROOT_LOGIN", "RISKY_PORT");
        assertThat(types(findings).stream().filter(t -> t.equals("RISKY_PORT"))).hasSize(1);   // loopback MySQL ignored
        assertThat(SecurityAssessor.overallStatus(true, findings)).isEqualTo("AT_RISK");
    }

    @Test
    void hardenedHostIsSecure() {
        var cfg = Map.of("PermitRootLogin", "no", "PasswordAuthentication", "no", "Port", "2222");
        var state = new SecurityState(1, true, "ENFORCING", true, 2222, cfg, NOW);
        var findings = assessor.assess(1, "RHEL-01", state, List.of(), NOW);
        assertThat(findings).isEmpty();
        assertThat(SecurityAssessor.overallStatus(true, findings)).isEqualTo("SECURE");
    }

    @Test
    void unreadableSshConfigIsInformationalOnly() {
        var state = new SecurityState(1, true, "ENFORCING", true, 22, null, NOW);
        var findings = assessor.assess(1, "RHEL-01", state, List.of(), NOW);
        assertThat(types(findings)).contains("SSH_CONFIG_UNREADABLE");
        assertThat(SecurityAssessor.overallStatus(true, findings)).isEqualTo("SECURE");
    }

    @Test
    void noDataMeansUnknown() {
        assertThat(SecurityAssessor.overallStatus(false, List.of())).isEqualTo("UNKNOWN");
        assertThat(assessor.assess(1, "x", null, List.of(), NOW)).isEmpty();
    }
}
