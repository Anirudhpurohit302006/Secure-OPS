package com.secureops.util;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.secureops.dto.Dtos.TargetRequest;
import com.secureops.exception.ApiException;
import org.junit.jupiter.api.Test;

class ValidationTest {

    @Test
    void acceptsValidTarget() {
        assertThatCode(() -> Validation.validateTarget(new TargetRequest("RHEL-01", "192.168.159.129", "ani", 22, true)))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsBadIp() {
        assertThatThrownBy(() -> Validation.validateTarget(new TargetRequest("RHEL-01", "999.1.1.1", "ani", 22, true)))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> Validation.validateTarget(new TargetRequest("RHEL-01", "host; rm -rf /", "ani", 22, true)))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void rejectsBadNameUserAndPort() {
        assertThatThrownBy(() -> Validation.validateTarget(new TargetRequest("", "10.0.0.1", "ani", 22, true))).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> Validation.validateTarget(new TargetRequest("A", "10.0.0.1", "a b; ls", 22, true))).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> Validation.validateTarget(new TargetRequest("A", "10.0.0.1", "ani", 70000, true))).isInstanceOf(ApiException.class);
    }

    @Test
    void timeRangeAllowsOnlyKnownValues() {
        assertThatCode(() -> TimeRange.parse("7d")).doesNotThrowAnyException();
        assertThatThrownBy(() -> TimeRange.parse("1y")).isInstanceOf(ApiException.class);
    }
}
