package com.secureops.collector;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ServiceCollectorTest {

    @Test
    void mapsSystemctlStates() {
        assertThat(ServiceCollector.mapState("active\n")).isEqualTo("RUNNING");
        assertThat(ServiceCollector.mapState("inactive")).isEqualTo("STOPPED");
        assertThat(ServiceCollector.mapState("failed")).isEqualTo("FAILED");
        assertThat(ServiceCollector.mapState("")).isEqualTo("UNKNOWN");
        assertThat(ServiceCollector.mapState("weird")).isEqualTo("UNKNOWN");
    }

    @Test
    void parsesFailedUnits() {
        assertThat(ServiceCollector.parseFailedUnits("httpd.service loaded failed failed The Apache HTTP Server\n"))
                .containsExactly("httpd.service");
        assertThat(ServiceCollector.parseFailedUnits("")).isEmpty();
    }
}
